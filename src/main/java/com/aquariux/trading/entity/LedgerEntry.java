package com.aquariux.trading.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String asset;

    @Column(name = "change_amount", nullable = false, precision = 24, scale = 8)
    private BigDecimal change;

    @Column(name = "balance_after", nullable = false, precision = 24, scale = 8)
    private BigDecimal balanceAfter;

    @Column(name = "reference_tx", nullable = false, length = 64)
    private String referenceTx;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public LedgerEntry(User user, String asset, BigDecimal change, BigDecimal balanceAfter, String referenceTx) {
        this.user = user;
        this.asset = asset;
        this.change = change;
        this.balanceAfter = balanceAfter;
        this.referenceTx = referenceTx;
        this.createdAt = Instant.now();
    }
}

