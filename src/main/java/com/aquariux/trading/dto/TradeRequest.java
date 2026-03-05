package com.aquariux.trading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TradeRequest {

    @NotBlank(message = "Symbol is required (BTCUSDT or ETHUSDT)")
    private String symbol;

    @NotBlank(message = "Side is required (BUY or SELL)")
    private String side;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.00000001", message = "Quantity must be positive")
    private BigDecimal quantity;
}
