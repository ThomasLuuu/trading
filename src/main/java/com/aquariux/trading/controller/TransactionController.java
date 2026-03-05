package com.aquariux.trading.controller;

import com.aquariux.trading.dto.TradeResponse;
import com.aquariux.trading.entity.TradeTransaction;
import com.aquariux.trading.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TradeService tradeService;

    public TransactionController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Get the user's trading history.
     */
    @GetMapping
    public ResponseEntity<List<TradeResponse>> getTradingHistory(
            @RequestParam(required = false) Long userId) {
        List<TradeTransaction> transactions = tradeService.getTradeHistory(userId);
        List<TradeResponse> body = transactions.stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
