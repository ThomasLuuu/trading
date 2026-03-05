package com.aquariux.trading.controller;

import com.aquariux.trading.dto.TradeRequest;
import com.aquariux.trading.dto.TradeResponse;
import com.aquariux.trading.entity.TradeTransaction;
import com.aquariux.trading.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trade")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Execute a trade (BUY or SELL) at the latest best aggregated price.
     * Idempotency key is supplied via the X-Idempotency-Key header.
     */
    @PostMapping
    public ResponseEntity<TradeResponse> trade(
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TradeRequest request) {
        TradeTransaction.TradeSide side;
        try {
            side = TradeTransaction.TradeSide.valueOf(request.getSide().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        TradeTransaction tx = tradeService.executeTrade(
                request.getSymbol(),
                side,
                request.getQuantity(),
                idempotencyKey
        );
        return ResponseEntity.ok(TradeResponse.from(tx));
    }
}
