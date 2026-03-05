package com.aquariux.trading.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceBookTickerDto {

    private String symbol;
    private String bidPrice;
    private String bidQty;
    private String askPrice;
    private String askQty;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBidPrice() {
        return bidPrice;
    }

    public void setBidPrice(String bidPrice) {
        this.bidPrice = bidPrice;
    }

    public String getBidQty() {
        return bidQty;
    }

    public void setBidQty(String bidQty) {
        this.bidQty = bidQty;
    }

    public String getAskPrice() {
        return askPrice;
    }

    public void setAskPrice(String askPrice) {
        this.askPrice = askPrice;
    }

    public String getAskQty() {
        return askQty;
    }

    public void setAskQty(String askQty) {
        this.askQty = askQty;
    }

    public BigDecimal getBidPriceAsBigDecimal() {
        return bidPrice != null ? new BigDecimal(bidPrice) : null;
    }

    public BigDecimal getAskPriceAsBigDecimal() {
        return askPrice != null ? new BigDecimal(askPrice) : null;
    }
}
