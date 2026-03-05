package com.aquariux.trading.service;

import com.aquariux.trading.entity.User;
import com.aquariux.trading.entity.Wallet;


public record Account(User user, Wallet usdtWallet, Wallet baseWallet, String baseCurrency) {
}

