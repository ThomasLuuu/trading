package com.aquariux.trading.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradingPairRequest {

    @NotBlank(message = "Symbol is required, e.g. BTCUSDT")
    private String symbol;
}

