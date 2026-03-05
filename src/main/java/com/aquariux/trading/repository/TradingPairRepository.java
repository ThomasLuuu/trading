package com.aquariux.trading.repository;

import com.aquariux.trading.entity.TradingPair;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradingPairRepository extends JpaRepository<TradingPair, Long> {

    Optional<TradingPair> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    List<TradingPair> findByEnabledTrue();
}

