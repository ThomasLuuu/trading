package com.aquariux.trading.repository;

import com.aquariux.trading.entity.AggregatedPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AggregatedPriceRepository extends JpaRepository<AggregatedPrice, Long> {

    Optional<AggregatedPrice> findBySymbol(String symbol);
}
