package com.aquariux.trading.controller;

import com.aquariux.trading.dto.WalletBalanceResponse;
import com.aquariux.trading.entity.Wallet;
import com.aquariux.trading.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/wallet")
public class WalletController {

    private final TradeService tradeService;

    public WalletController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Get the user's crypto currencies wallet balance.
     */
    @GetMapping
    public ResponseEntity<List<WalletBalanceResponse>> getWalletBalance(
            @RequestParam(required = false) Long userId) {
        List<Wallet> wallets = tradeService.getWalletBalances(userId);
        List<WalletBalanceResponse> body = wallets.stream()
                .map(WalletBalanceResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
