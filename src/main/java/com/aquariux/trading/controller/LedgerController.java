package com.aquariux.trading.controller;

import com.aquariux.trading.dto.LedgerEntryResponse;
import com.aquariux.trading.entity.LedgerEntry;
import com.aquariux.trading.service.TradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final TradeService tradeService;

    public LedgerController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Get ledger entries (accounting records of balance changes).
     */
    @GetMapping
    public ResponseEntity<List<LedgerEntryResponse>> getLedger(
            @RequestParam(required = false) Long userId) {
        List<LedgerEntry> entries = tradeService.getLedger(userId);
        List<LedgerEntryResponse> body = entries.stream()
                .map(LedgerEntryResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}

