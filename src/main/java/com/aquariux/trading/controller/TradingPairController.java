package com.aquariux.trading.controller;

import com.aquariux.trading.dto.TradingPairRequest;
import com.aquariux.trading.entity.TradingPair;
import com.aquariux.trading.repository.TradingPairRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/symbols")
public class TradingPairController {

    private final TradingPairRepository tradingPairRepository;

    public TradingPairController(TradingPairRepository tradingPairRepository) {
        this.tradingPairRepository = tradingPairRepository;
    }

    @GetMapping
    public ResponseEntity<List<TradingPair>> listAll() {
        return ResponseEntity.ok(tradingPairRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<TradingPair> create(@Valid @RequestBody TradingPairRequest request) {
        String symbol = request.getSymbol().toUpperCase();
        if (tradingPairRepository.existsBySymbol(symbol)) {
            TradingPair existing = tradingPairRepository.findBySymbol(symbol).orElseThrow();
            return ResponseEntity.ok(existing);
        }

        TradingPair pair = new TradingPair(symbol);
        TradingPair saved = tradingPairRepository.save(pair);
        return ResponseEntity.created(URI.create("/api/symbols/" + saved.getId())).body(saved);
    }
}

