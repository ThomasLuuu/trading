package com.aquariux.trading.service;

import com.aquariux.trading.entity.User;
import com.aquariux.trading.entity.Wallet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Accounts {

    private final User user;
    private final Wallet usdtWallet;
    private final Wallet baseWallet;
    private final String baseCurrency;
}

