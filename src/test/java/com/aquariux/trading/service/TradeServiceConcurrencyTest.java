package com.aquariux.trading.service;

import com.aquariux.trading.entity.AggregatedPrice;
import com.aquariux.trading.entity.TradeTransaction;
import com.aquariux.trading.entity.User;
import com.aquariux.trading.entity.Wallet;
import com.aquariux.trading.repository.AggregatedPriceRepository;
import com.aquariux.trading.repository.TradeTransactionRepository;
import com.aquariux.trading.repository.UserRepository;
import com.aquariux.trading.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TradeServiceConcurrencyTest {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Autowired
    private TradeTransactionRepository tradeTransactionRepository;

    @BeforeEach
    void setUp() {
        tradeTransactionRepository.deleteAll();
        User user = userRepository.findById(1L).orElseThrow();
        Wallet usdt = walletRepository.findByUserIdAndCurrency(1L, "USDT").orElseThrow();
        usdt.setBalance(new BigDecimal("50000.00000000"));
        walletRepository.save(usdt);

        AggregatedPrice eth = aggregatedPriceRepository.findBySymbol("ETHUSDT")
                .orElse(new AggregatedPrice("ETHUSDT",
                        new BigDecimal("1950.00000000"),
                        new BigDecimal("1950.00000000")));
        eth.setBestBid(new BigDecimal("1950.00000000"));
        eth.setBestAsk(new BigDecimal("1950.00000000"));
        aggregatedPriceRepository.save(eth);
    }

    @Test
    void concurrentTrades_areQueuedAndBalancesAreConsistent() throws Exception {
        int threads = 5;
        BigDecimal quantityPerOrder = new BigDecimal("0.5");

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        List<TradeTransaction> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    TradeTransaction tx = tradeService.executeTrade(
                            "ETHUSDT",
                            TradeTransaction.TradeSide.BUY,
                            quantityPerOrder,
                            "concurrent-" + idx
                    );
                    synchronized (results) {
                        results.add(tx);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertThat(results).hasSize(threads);
        assertThat(tradeTransactionRepository.count()).isEqualTo(threads);

        BigDecimal price = new BigDecimal("1950.00000000");
        BigDecimal expectedUsdt = new BigDecimal("50000.00000000")
                .subtract(price.multiply(quantityPerOrder).multiply(BigDecimal.valueOf(threads)));

        Wallet usdtWallet = walletRepository.findByUserIdAndCurrency(1L, "USDT").orElseThrow();
        assertThat(usdtWallet.getBalance()).isEqualByComparingTo(expectedUsdt);
    }

    @Test
    void sameRequestId_isIdempotentUnderConcurrency() throws Exception {
        int threads = 5;
        BigDecimal quantityPerOrder = new BigDecimal("0.5");
        String requestId = "idem-123";

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        List<TradeTransaction> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    TradeTransaction tx = tradeService.executeTrade(
                            "ETHUSDT",
                            TradeTransaction.TradeSide.BUY,
                            quantityPerOrder,
                            requestId
                    );
                    synchronized (results) {
                        results.add(tx);
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        assertThat(tradeTransactionRepository.findByRequestId(requestId)).isPresent();
        assertThat(tradeTransactionRepository.count()).isEqualTo(1);

        assertThat(results).isNotEmpty();
        Long firstId = results.get(0).getId();
        assertThat(results.stream().map(TradeTransaction::getId).distinct().count()).isEqualTo(1);
        assertThat(firstId).isNotNull();
    }
}


