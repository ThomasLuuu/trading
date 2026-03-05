package com.aquariux.trading.service;

import com.aquariux.trading.client.BinanceBookTickerDto;
import com.aquariux.trading.client.HuobiTickerDto;
import com.aquariux.trading.client.HuobiTickersResponse;
import com.aquariux.trading.entity.AggregatedPrice;
import com.aquariux.trading.entity.TradingPair;
import com.aquariux.trading.repository.AggregatedPriceRepository;
import com.aquariux.trading.repository.TradingPairRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Aggregates best bid/ask from Binance and Huobi.
 * Best bid = highest bid (for SELL order), Best ask = lowest ask (for BUY order).
 */
@Service
@RequiredArgsConstructor
public class PriceAggregationService {

    private static final Logger log = LoggerFactory.getLogger(PriceAggregationService.class);
    private final RestTemplate restTemplate;
    private final AggregatedPriceRepository aggregatedPriceRepository;
    private final TradingPairRepository tradingPairRepository;

    @Value("${price.binance.url:https://api.binance.com/api/v3/ticker/bookTicker}")
    private String binanceUrl;

    @Value("${price.huobi.url:https://api.huobi.pro/market/tickers}")
    private String huobiUrl;

    /**
     * On startup, fetch prices once so AggregatedPriceRepository is populated
     * before any trades are executed. Scheduler will then continue to refresh
     * every 10 seconds as usual.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void populatePricesOnStartup() {
        aggregatePrices();
    }

    @Scheduled(fixedRate = 10_000) // 10 seconds
    public void aggregatePrices() {
        List<TradingPair> pairs = tradingPairRepository.findByEnabledTrue();
        if (pairs.isEmpty()) {
            return;
        }

        Set<String> symbols = new HashSet<>();
        Map<String, List<BigDecimal>> bidsBySymbol = new HashMap<>();
        Map<String, List<BigDecimal>> asksBySymbol = new HashMap<>();
        for (TradingPair pair : pairs) {
            String symbol = pair.getSymbol().toUpperCase();
            symbols.add(symbol);
            bidsBySymbol.put(symbol, new ArrayList<>());
            asksBySymbol.put(symbol, new ArrayList<>());
        }

        fetchBinance(symbols, bidsBySymbol, asksBySymbol);
        fetchHuobi(symbols, bidsBySymbol, asksBySymbol);

        for (String symbol : symbols) {
            List<BigDecimal> bids = bidsBySymbol.get(symbol);
            List<BigDecimal> asks = asksBySymbol.get(symbol);
            if (bids.isEmpty() && asks.isEmpty()) {
                continue;
            }

            BigDecimal bestBid = bids.stream().max(BigDecimal::compareTo).orElse(null);
            BigDecimal bestAsk = asks.stream().min(BigDecimal::compareTo).orElse(null);
            if (bestBid == null) {
                bestBid = bestAsk != null ? bestAsk : BigDecimal.ZERO;
            }
            if (bestAsk == null) {
                bestAsk = bestBid;
            }

            // capture values in effectively-final locals for use in lambdas
            final BigDecimal finalBestBid = bestBid;
            final BigDecimal finalBestAsk = bestAsk;
            final Instant now = Instant.now();

            AggregatedPrice price = aggregatedPriceRepository.findBySymbol(symbol)
                    .orElse(new AggregatedPrice(symbol, finalBestBid, finalBestAsk));
            price.setBestBid(finalBestBid);
            price.setBestAsk(finalBestAsk);
            price.setUpdatedAt(now);
            try {
                aggregatedPriceRepository.save(price);
            } catch (DataIntegrityViolationException ex) {
                // In case of a rare race where another thread just inserted this symbol,
                // re-read and update the existing row instead of failing the whole task.
                log.debug("Conflict saving aggregated price for {}. Retrying as update. Root cause: {}",
                        symbol, ex.getMostSpecificCause().getMessage());
                aggregatedPriceRepository.findBySymbol(symbol).ifPresent(existing -> {
                    existing.setBestBid(finalBestBid);
                    existing.setBestAsk(finalBestAsk);
                    existing.setUpdatedAt(now);
                    aggregatedPriceRepository.save(existing);
                });
            }
        }
    }

    private void fetchBinance(Set<String> supportedSymbols,
                              Map<String, List<BigDecimal>> bidsBySymbol,
                              Map<String, List<BigDecimal>> asksBySymbol) {
        try {
            ResponseEntity<List<BinanceBookTickerDto>> response = restTemplate.exchange(
                    binanceUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<BinanceBookTickerDto>>() {}
            );
            if (response.getBody() == null) {
                return;
            }
            for (BinanceBookTickerDto ticker : response.getBody()) {
                String sym = ticker.getSymbol();
                if (!supportedSymbols.contains(sym)) {
                    continue;
                }
                if (ticker.getBidPriceAsBigDecimal() != null) {
                    bidsBySymbol.get(sym).add(ticker.getBidPriceAsBigDecimal());
                }
                if (ticker.getAskPriceAsBigDecimal() != null) {
                    asksBySymbol.get(sym).add(ticker.getAskPriceAsBigDecimal());
                }
            }
        } catch (Exception e) {
            log.warn("Binance fetch failed: {}", e.getMessage());
        }
    }

    private void fetchHuobi(Set<String> supportedSymbols,
                            Map<String, List<BigDecimal>> bidsBySymbol,
                            Map<String, List<BigDecimal>> asksBySymbol) {
        try {
            HuobiTickersResponse response = restTemplate.getForObject(huobiUrl, HuobiTickersResponse.class);
            if (response == null || response.getData() == null) {
                return;
            }
            for (HuobiTickerDto ticker : response.getData()) {
                String sym = ticker.getSymbolUpper();
                if (!supportedSymbols.contains(sym)) {
                    continue;
                }
                if (ticker.getBid() != null) {
                    bidsBySymbol.get(sym).add(ticker.getBid());
                }
                if (ticker.getAsk() != null) {
                    asksBySymbol.get(sym).add(ticker.getAsk());
                }
            }
        } catch (Exception e) {
            log.warn("Huobi fetch failed: {}", e.getMessage());
        }
    }
}
