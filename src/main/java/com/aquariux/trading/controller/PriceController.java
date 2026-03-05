package com.aquariux.trading.controller;

import com.aquariux.trading.dto.AggregatedPriceResponse;
import com.aquariux.trading.entity.AggregatedPrice;
import com.aquariux.trading.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/prices")
public class PriceController {

    private final TradeService tradeService;

    public PriceController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Get latest best aggregated price for all supported symbols.
     */
    @GetMapping
    public ResponseEntity<List<AggregatedPriceResponse>> getLatestPrices() {
        List<AggregatedPrice> prices = tradeService.getLatestPrices();
        List<AggregatedPriceResponse> body = prices.stream()
                .map(p -> new AggregatedPriceResponse(p.getSymbol(), p.getBestBid(), p.getBestAsk(), p.getUpdatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    /**
     * Get latest best aggregated price for a specific symbol (e.g. BTCUSDT, ETHUSDT).
     */
    @GetMapping("/{symbol}")
    public ResponseEntity<AggregatedPriceResponse> getLatestPrice(@PathVariable String symbol) {
        AggregatedPrice price = tradeService.getLatestPrice(symbol);
        return ResponseEntity.ok(new AggregatedPriceResponse(
                price.getSymbol(), price.getBestBid(), price.getBestAsk(), price.getUpdatedAt()));
    }
}
