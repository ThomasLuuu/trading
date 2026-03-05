package com.aquariux.trading.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "trade_transactions",
        uniqueConstraints = @UniqueConstraint(columnNames = "request_id")
)
@Getter
@Setter
@NoArgsConstructor
public class TradeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 4)
    @Enumerated(EnumType.STRING)
    private TradeSide side;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal price;

    @Column(name = "total_usdt", nullable = false, precision = 24, scale = 8)
    private BigDecimal totalUsdt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "request_id", nullable = false, unique = true, length = 64)
    private String requestId;

    public enum TradeSide {
        BUY, SELL
    }

    public TradeTransaction(User user,
                            String symbol,
                            TradeSide side,
                            BigDecimal quantity,
                            BigDecimal price,
                            BigDecimal totalUsdt,
                            String requestId) {
        this.user = user;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.totalUsdt = totalUsdt;
        this.requestId = requestId;
        this.createdAt = Instant.now();
    }
}
