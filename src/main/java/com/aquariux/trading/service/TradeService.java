package com.aquariux.trading.service;

import com.aquariux.trading.entity.*;
import com.aquariux.trading.model.Account;
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
     * TODO: Currently apply for 1 instance, in case multiple instances should be applied distributed lock with idempotency key
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

            Account account = loadAccounts(normalizedSymbol);
            applyTradeToWallets(account, side, quantity, totalUsdt);

            TradeTransaction tx = new TradeTransaction(
                    account.user(),
                    normalizedSymbol,
                    side,
                    quantity,
                    pricePerUnit,
                    totalUsdt,
                    requestId
            );
            tx = tradeTransactionRepository.save(tx);

            createLedgerEntries(tx, account, side, quantity, totalUsdt);

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

    private Account loadAccounts(String symbol) {
        User user = userRepository.findById(DEFAULT_USER_ID)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        String baseCurrency = symbol.replace(USDT, "");

        Wallet usdtWallet = walletRepository.findByUserIdAndCurrency(DEFAULT_USER_ID, USDT)
                .orElseThrow(() -> new IllegalStateException("USDT wallet not found"));
        Wallet baseWallet = walletRepository.findByUserIdAndCurrency(DEFAULT_USER_ID, baseCurrency)
                .orElseGet(() -> walletRepository.save(new Wallet(user, baseCurrency, BigDecimal.ZERO)));

        return new Account(user, usdtWallet, baseWallet, baseCurrency);
    }

    private void applyTradeToWallets(Account account,
                                     TradeTransaction.TradeSide side,
                                     BigDecimal quantity,
                                     BigDecimal totalUsdt) {
        Wallet usdtWallet = account.usdtWallet();
        Wallet baseWallet = account.baseWallet();

        if (side == TradeTransaction.TradeSide.BUY) {
            if (usdtWallet.getBalance().compareTo(totalUsdt) < 0) {
                throw new IllegalStateException("Insufficient USDT balance");
            }
            usdtWallet.setBalance(usdtWallet.getBalance().subtract(totalUsdt));
            baseWallet.setBalance(baseWallet.getBalance().add(quantity));
        } else {
            if (baseWallet.getBalance().compareTo(quantity) < 0) {
                throw new IllegalStateException("Insufficient " + account.baseCurrency() + " balance");
            }
            baseWallet.setBalance(baseWallet.getBalance().subtract(quantity));
            usdtWallet.setBalance(usdtWallet.getBalance().add(totalUsdt));
        }

        walletRepository.save(usdtWallet);
        walletRepository.save(baseWallet);
    }

    private void createLedgerEntries(TradeTransaction tx,
                                     Account account,
                                     TradeTransaction.TradeSide side,
                                     BigDecimal quantity,
                                     BigDecimal totalUsdt) {
        String referenceTx = String.valueOf(tx.getId());

        if (side == TradeTransaction.TradeSide.BUY) {
            LedgerEntry usdtEntry = new LedgerEntry(
                    account.user(),
                    USDT,
                    totalUsdt.negate(),
                    account.usdtWallet().getBalance(),
                    referenceTx
            );
            LedgerEntry baseEntry = new LedgerEntry(
                    account.user(),
                    account.baseCurrency(),
                    quantity,
                    account.baseWallet().getBalance(),
                    referenceTx
            );
            ledgerEntryRepository.save(usdtEntry);
            ledgerEntryRepository.save(baseEntry);
        } else {
            LedgerEntry baseEntry = new LedgerEntry(
                    account.user(),
                    account.baseCurrency(),
                    quantity.negate(),
                    account.baseWallet().getBalance(),
                    referenceTx
            );
            LedgerEntry usdtEntry = new LedgerEntry(
                    account.user(),
                    USDT,
                    totalUsdt,
                    account.usdtWallet().getBalance(),
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
