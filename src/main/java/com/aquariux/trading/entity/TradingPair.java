package com.aquariux.trading.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trading_pairs", uniqueConstraints = @UniqueConstraint(columnNames = "symbol"))
@Getter
@Setter
@NoArgsConstructor
public class TradingPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false)
    private boolean enabled;

    public TradingPair(String symbol) {
        this.symbol = symbol;
        this.enabled = true;
    }
}

