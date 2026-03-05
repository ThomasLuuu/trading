package com.aquariux.trading.service;

import com.aquariux.trading.entity.*;
import com.aquariux.trading.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class TradeService {

    private static final long DEFAULT_USER_ID = 1L;
    private static final String USDT = "USDT";
    private static final List<String> SUPPORTED_SYMBOLS = List.of("BTCUSDT", "ETHUSDT");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AggregatedPriceRepository aggregatedPriceRepository;
    private final TradeTransactionRepository tradeTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TradingPairRepository tradingPairRepository;
    /**
     * Fair lock to ensure trade requests are processed one at a time (queued)
     * and in arrival order.
     */
    private final ReentrantLock tradeLock = new ReentrantLock(true);

    public AggregatedPrice getLatestPrice(String symbol) {
        return aggregatedPriceRepository.findBySymbol(symbol.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Symbol not supported or no price yet: " + symbol));
    }

    public List<AggregatedPrice> getLatestPrices() {
        return aggregatedPriceRepository.findAll();
    }

    /**
     * Execute a trade under a fair ReentrantLock and using a requestId for idempotency.
     * If the same requestId is seen again, the existing TradeTransaction is returned.
     */
    @Transactional
    public TradeTransaction executeTrade(String symbol,
                                         TradeTransaction.TradeSide side,
                                         BigDecimal quantity,
                                         String requestId) {
        tradeLock.lock();
        try {
            TradeTransaction existing = findExistingTransaction(requestId);
            if (existing != null) {
                return existing;
            }

            String normalizedSymbol = validateAndNormalizeSymbol(symbol, quantity);
            AggregatedPrice price = getLatestPrice(normalizedSymbol);
            BigDecimal pricePerUnit = resolveTradePrice(side, price);
            BigDecimal totalUsdt = quantity.multiply(pricePerUnit).setScale(8, RoundingMode.HALF_UP);

            Accounts accounts = loadAccounts(normalizedSymbol);
            applyTradeToWallets(accounts, side, quantity, totalUsdt);

            TradeTransaction tx = new TradeTransaction(
                    accounts.getUser(),
                    normalizedSymbol,
                    side,
                    quantity,
                    pricePerUnit,
                    totalUsdt,
                    requestId
            );
            tx = tradeTransactionRepository.save(tx);

            createLedgerEntries(tx, accounts, side, quantity, totalUsdt);

            return tx;
        } finally {
            tradeLock.unlock();
        }
    }

    private TradeTransaction findExistingTransaction(String requestId) {
        return tradeTransactionRepository.findByRequestId(requestId).orElse(null);
    }

    private String validateAndNormalizeSymbol(String symbol, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        String normalized = symbol.toUpperCase();
        if (!tradingPairRepository.existsBySymbol(normalized)) {
            throw new IllegalArgumentException("Unsupported symbol. Please configure it as a trading pair first.");
        }
        return normalized;
    }

    private BigDecimal resolveTradePrice(TradeTransaction.TradeSide side, AggregatedPrice price) {
        return side == TradeTransaction.TradeSide.BUY ? price.getBestAsk() : price.getBestBid();
    }

    private Accounts loadAccounts(String symbol) {
        User user = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String baseCurrency = symbol.replace(USDT, "");

        Wallet usdtWallet = walletRepository.findByUserIdAndCurrency(DEFAULT_USER_ID, USDT)
                .orElseThrow(() -> new IllegalStateException("USDT wallet not found"));
        Wallet baseWallet = walletRepository.findByUserIdAndCurrency(DEFAULT_USER_ID, baseCurrency)
                .orElseGet(() -> walletRepository.save(new Wallet(user, baseCurrency, BigDecimal.ZERO)));

        return new Accounts(user, usdtWallet, baseWallet, baseCurrency);
    }

    private void applyTradeToWallets(Accounts accounts,
                                     TradeTransaction.TradeSide side,
                                     BigDecimal quantity,
                                     BigDecimal totalUsdt) {
        Wallet usdtWallet = accounts.getUsdtWallet();
        Wallet baseWallet = accounts.getBaseWallet();

        if (side == TradeTransaction.TradeSide.BUY) {
            if (usdtWallet.getBalance().compareTo(totalUsdt) < 0) {
                throw new IllegalStateException("Insufficient USDT balance");
            }
            usdtWallet.setBalance(usdtWallet.getBalance().subtract(totalUsdt));
            baseWallet.setBalance(baseWallet.getBalance().add(quantity));
        } else {
            if (baseWallet.getBalance().compareTo(quantity) < 0) {
                throw new IllegalStateException("Insufficient " + accounts.getBaseCurrency() + " balance");
            }
            baseWallet.setBalance(baseWallet.getBalance().subtract(quantity));
            usdtWallet.setBalance(usdtWallet.getBalance().add(totalUsdt));
        }

        walletRepository.save(usdtWallet);
        walletRepository.save(baseWallet);
    }

    private void createLedgerEntries(TradeTransaction tx,
                                     Accounts accounts,
                                     TradeTransaction.TradeSide side,
                                     BigDecimal quantity,
                                     BigDecimal totalUsdt) {
        String referenceTx = String.valueOf(tx.getId());

        if (side == TradeTransaction.TradeSide.BUY) {
            LedgerEntry usdtEntry = new LedgerEntry(
                    accounts.getUser(),
                    USDT,
                    totalUsdt.negate(),
                    accounts.getUsdtWallet().getBalance(),
                    referenceTx
            );
            LedgerEntry baseEntry = new LedgerEntry(
                    accounts.getUser(),
                    accounts.getBaseCurrency(),
                    quantity,
                    accounts.getBaseWallet().getBalance(),
                    referenceTx
            );
            ledgerEntryRepository.save(usdtEntry);
            ledgerEntryRepository.save(baseEntry);
        } else {
            LedgerEntry baseEntry = new LedgerEntry(
                    accounts.getUser(),
                    accounts.getBaseCurrency(),
                    quantity.negate(),
                    accounts.getBaseWallet().getBalance(),
                    referenceTx
            );
            LedgerEntry usdtEntry = new LedgerEntry(
                    accounts.getUser(),
                    USDT,
                    totalUsdt,
                    accounts.getUsdtWallet().getBalance(),
                    referenceTx
            );
            ledgerEntryRepository.save(baseEntry);
            ledgerEntryRepository.save(usdtEntry);
        }
    }

    public List<Wallet> getWalletBalances(Long userId) {
        Long uid = userId != null ? userId : DEFAULT_USER_ID;
        return walletRepository.findByUserId(uid);
    }

    public List<TradeTransaction> getTradeHistory(Long userId) {
        Long uid = userId != null ? userId : DEFAULT_USER_ID;
        return tradeTransactionRepository.findByUserIdOrderByCreatedAtDesc(uid);
    }

    public List<LedgerEntry> getLedger(Long userId) {
        Long uid = userId != null ? userId : DEFAULT_USER_ID;
        return ledgerEntryRepository.findByUserIdOrderByIdAsc(uid);
    }
}
