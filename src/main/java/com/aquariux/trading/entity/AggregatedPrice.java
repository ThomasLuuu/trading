package com.aquariux.trading.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "aggregated_prices", uniqueConstraints = @UniqueConstraint(columnNames = "symbol"))
@Getter
@Setter
@NoArgsConstructor
public class AggregatedPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    /** Best bid price (used for SELL orders) - highest bid across sources */
    @Column(name = "best_bid", nullable = false, precision = 24, scale = 8)
    private BigDecimal bestBid;

    /** Best ask price (used for BUY orders) - lowest ask across sources */
    @Column(name = "best_ask", nullable = false, precision = 24, scale = 8)
    private BigDecimal bestAsk;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AggregatedPrice(String symbol, BigDecimal bestBid, BigDecimal bestAsk) {
        this.symbol = symbol;
        this.bestBid = bestBid;
        this.bestAsk = bestAsk;
        this.updatedAt = Instant.now();
    }
}
