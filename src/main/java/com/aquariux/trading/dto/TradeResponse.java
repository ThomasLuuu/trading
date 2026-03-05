package com.aquariux.trading.dto;

import com.aquariux.trading.entity.TradeTransaction;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class TradeResponse {

    private Long id;
    private String symbol;
    private String side;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalUsdt;
    private Instant createdAt;

    public static TradeResponse from(TradeTransaction tx) {
        TradeResponse r = new TradeResponse();
        r.setId(tx.getId());
        r.setSymbol(tx.getSymbol());
        r.setSide(tx.getSide().name());
        r.setQuantity(tx.getQuantity());
        r.setPrice(tx.getPrice());
        r.setTotalUsdt(tx.getTotalUsdt());
        r.setCreatedAt(tx.getCreatedAt());
        return r;
    }
}
