package com.aquariux.trading.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class AggregatedPriceResponse {

    private String symbol;
    private BigDecimal bestBid;
    private BigDecimal bestAsk;
    private Instant updatedAt;
}
