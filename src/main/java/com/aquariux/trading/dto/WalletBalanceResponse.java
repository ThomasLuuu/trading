package com.aquariux.trading.dto;

import com.aquariux.trading.entity.Wallet;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WalletBalanceResponse {

    private String currency;
    private BigDecimal balance;

    public static WalletBalanceResponse from(Wallet wallet) {
        WalletBalanceResponse balanceResponse = new WalletBalanceResponse();
        balanceResponse.setCurrency(wallet.getCurrency());
        balanceResponse.setBalance(wallet.getBalance());
        return balanceResponse;
    }
}
