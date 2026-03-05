package com.aquariux.trading.dto;

import com.aquariux.trading.entity.LedgerEntry;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class LedgerEntryResponse {

    private Long id;
    private Long userId;
    private String asset;
    private BigDecimal change;
    private BigDecimal balanceAfter;
    private String referenceTx;
    private Instant createdAt;

    public static LedgerEntryResponse from(LedgerEntry entry) {
        LedgerEntryResponse ledgerEntryResponse = new LedgerEntryResponse();
        ledgerEntryResponse.id = entry.getId();
        ledgerEntryResponse.userId = entry.getUser().getId();
        ledgerEntryResponse.asset = entry.getAsset();
        ledgerEntryResponse.change = entry.getChange();
        ledgerEntryResponse.balanceAfter = entry.getBalanceAfter();
        ledgerEntryResponse.referenceTx = entry.getReferenceTx();
        ledgerEntryResponse.createdAt = entry.getCreatedAt();
        return ledgerEntryResponse;
    }
}

