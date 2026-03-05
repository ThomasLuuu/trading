package com.aquariux.trading.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HuobiTickerDto {

    private String symbol;
    private BigDecimal bid;
    private BigDecimal bidSize;
    private BigDecimal ask;
    private BigDecimal askSize;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getBid() {
        return bid;
    }

    public void setBid(BigDecimal bid) {
        this.bid = bid;
    }

    public BigDecimal getBidSize() {
        return bidSize;
    }

    public void setBidSize(BigDecimal bidSize) {
        this.bidSize = bidSize;
    }

    public BigDecimal getAsk() {
        return ask;
    }

    public void setAsk(BigDecimal ask) {
        this.ask = ask;
    }

    public BigDecimal getAskSize() {
        return askSize;
    }

    public void setAskSize(BigDecimal askSize) {
        this.askSize = askSize;
    }

    /** Huobi uses lowercase symbols e.g. btcusdt, ethusdt */
    public String getSymbolUpper() {
        return symbol != null ? symbol.toUpperCase() : null;
    }
}
