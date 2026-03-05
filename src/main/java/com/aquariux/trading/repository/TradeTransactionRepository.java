package com.aquariux.trading.repository;

import com.aquariux.trading.entity.TradeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeTransactionRepository extends JpaRepository<TradeTransaction, Long> {

    List<TradeTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<TradeTransaction> findByRequestId(String requestId);
}
