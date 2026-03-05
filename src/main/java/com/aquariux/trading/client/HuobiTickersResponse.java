package com.aquariux.trading.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HuobiTickersResponse {

    private List<HuobiTickerDto> data;

    public List<HuobiTickerDto> getData() {
        return data;
    }

    public void setData(List<HuobiTickerDto> data) {
        this.data = data;
    }
}
