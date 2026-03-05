package com.aquariux.trading.api;

import com.aquariux.trading.dto.TradeRequest;
import com.aquariux.trading.repository.AggregatedPriceRepository;
import com.aquariux.trading.repository.TradeTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TradeApiIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private AggregatedPriceRepository aggregatedPriceRepository;

    @Autowired
    private TradeTransactionRepository tradeTransactionRepository;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        // ensure clean trade history for each test
        tradeTransactionRepository.deleteAll();
    }

    @Test
    void pricesApi_viaHttp_returnsSupportedSymbols() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/v1/prices",
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        // best-effort check: body should be a JSON array (even if empty)
        assertThat(body.trim()).startsWith("[");
    }

    @Test
    void tradeBuyFlow_viaHttp_updatesWalletsAndReturnsExpectedJson() throws Exception {
        TradeRequest request = new TradeRequest();
        request.setSymbol("ETHUSDT");
        request.setSide("BUY");
        request.setQuantity(new BigDecimal("1.0"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", "itg-http-buy-1");
        HttpEntity<TradeRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/trade",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"symbol\":\"ETHUSDT\"");
        assertThat(body).contains("\"side\":\"BUY\"");

        // verify wallet API JSON
        ResponseEntity<String> walletResp = restTemplate.getForEntity(
                baseUrl + "/api/v1/wallet",
                String.class
        );
        assertThat(walletResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String walletBody = walletResp.getBody();
        assertThat(walletBody).isNotNull();
        // should contain ETH wallet entry
        assertThat(walletBody).contains("\"currency\":\"ETH\"");
    }

    @Test
    void tradeIsIdempotentForSameRequestId_overHttp() throws Exception {
        String requestId = "itg-http-idem-1";

        TradeRequest request = new TradeRequest();
        request.setSymbol("ETHUSDT");
        request.setSide("BUY");
        request.setQuantity(new BigDecimal("0.5"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", requestId);
        HttpEntity<TradeRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> first = restTemplate.exchange(
                baseUrl + "/api/v1/trade",
                HttpMethod.POST,
                entity,
                String.class
        );
        ResponseEntity<String> second = restTemplate.exchange(
                baseUrl + "/api/v1/trade",
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);

        String firstBody = first.getBody();
        String secondBody = second.getBody();
        assertThat(firstBody).isNotNull();
        assertThat(secondBody).isNotNull();

        // same id in JSON -> idempotency over HTTP
        long firstId = extractId(firstBody);
        long secondId = extractId(secondBody);
        assertThat(firstId).isEqualTo(secondId);
    }

    @Test
    void transactionsApi_viaHttp_returnsTradeHistoryAfterTrade() {
        // first perform a trade
        TradeRequest request = new TradeRequest();
        request.setSymbol("ETHUSDT");
        request.setSide("BUY");
        request.setQuantity(new BigDecimal("0.3"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", "itg-http-history-1");
        HttpEntity<TradeRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> tradeResp = restTemplate.exchange(
                baseUrl + "/api/v1/trade",
                HttpMethod.POST,
                entity,
                String.class
        );
        assertThat(tradeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // then check transactions API
        ResponseEntity<String> txResp = restTemplate.getForEntity(
                baseUrl + "/api/v1/transactions",
                String.class
        );
        assertThat(txResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String txBody = txResp.getBody();
        assertThat(txBody).isNotNull();
        assertThat(txBody).contains("\"symbol\":\"ETHUSDT\"");
        assertThat(txBody).contains("\"side\":\"BUY\"");
    }

    @Test
    void ledgerApi_viaHttp_returnsLedgerEntriesAfterTrade() {
        TradeRequest request = new TradeRequest();
        request.setSymbol("BTCUSDT");
        request.setSide("BUY");
        request.setQuantity(new BigDecimal("0.1"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Idempotency-Key", "itg-http-ledger-1");
        HttpEntity<TradeRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> tradeResp = restTemplate.exchange(
                baseUrl + "/api/v1/trade",
                HttpMethod.POST,
                entity,
                String.class
        );
        assertThat(tradeResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> ledgerResp = restTemplate.getForEntity(
                baseUrl + "/api/v1/ledger",
                String.class
        );
        assertThat(ledgerResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        String ledgerBody = ledgerResp.getBody();
        assertThat(ledgerBody).isNotNull();
        // Expect both USDT and BTC entries in the ledger
        assertThat(ledgerBody).contains("\"asset\":\"USDT\"");
        assertThat(ledgerBody).contains("\"asset\":\"BTC\"");
    }

    private long extractId(String json) {
        // very small helper to parse `"id":<number>` without full JSON parsing
        int idx = json.indexOf("\"id\":");
        assertThat(idx).isGreaterThanOrEqualTo(0);
        int start = idx + 5;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(start, end));
    }
}




