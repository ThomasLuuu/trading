package com.aquariux.trading.config;

import com.aquariux.trading.entity.TradingPair;
import com.aquariux.trading.entity.User;
import com.aquariux.trading.entity.Wallet;
import com.aquariux.trading.repository.TradingPairRepository;
import com.aquariux.trading.repository.UserRepository;
import com.aquariux.trading.repository.WalletRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds initial user with 50,000 USDT wallet balance (per requirement).
 */
@Component
public class DataLoader implements ApplicationRunner {

    private static final BigDecimal INITIAL_USDT = new BigDecimal("50000.00000000");

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TradingPairRepository tradingPairRepository;

    public DataLoader(UserRepository userRepository,
                      WalletRepository walletRepository,
                      TradingPairRepository tradingPairRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.tradingPairRepository = tradingPairRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            User user = new User("default");
            user = userRepository.save(user);
            Wallet usdtWallet = new Wallet(user, "USDT", INITIAL_USDT);
            walletRepository.save(usdtWallet);
        }

        if (tradingPairRepository.count() == 0) {
            tradingPairRepository.save(new TradingPair("BTCUSDT"));
            tradingPairRepository.save(new TradingPair("ETHUSDT"));
        }
    }
}
