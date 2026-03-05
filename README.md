# Crypto Trading System

Spring Boot application with in-memory H2 database for crypto trading (BTCUSDT, ETHUSDT).

## Assumptions

- User is already authenticated; APIs use a default user (ID=1) with initial **50,000 USDT**.
- Supported pairs: **BTCUSDT** (Bitcoin), **ETHUSDT** (Ethereum).

## Features

1. **Price aggregation** – Every 10 seconds, best bid/ask are fetched from Binance and Huobi and stored. Best **bid** is used for SELL, best **ask** for BUY.
2. **APIs**
   - Get latest aggregated prices
   - Execute trade (BUY/SELL) at latest best price
   - Get wallet balances (USDT, BTC, ETH)
   - Get trading history
   - Get ledger entries (accounting view)
   - Manage supported trading symbols

## Run

```bash
./mvnw spring-boot:run
```

Server: http://localhost:8080  
H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:cryptotrading`, user: `sa`, password: empty)

## Swagger / OpenAPI

- OpenAPI JSON: `http://localhost:8080/v3/api/v1-docs`  
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## API Reference

### 1. Get latest best aggregated price(s)

- **GET** `/api/v1/prices` – all symbols  
- **GET** `/api/v1/prices/{symbol}` – e.g. `/api/v1/prices/BTCUSDT`

Response: `symbol`, `bestBid`, `bestAsk`, `updatedAt`.

### 2. Execute trade (queued + idempotent)

- **POST** `/api/v1/trade`  
- Body:  
  ```json
  {
    "symbol": "ETHUSDT",
    "side": "BUY",
    "quantity": 0.5
  }
  ```  
- `X-Idempotency-Key` in Header is an **idempotency key** – sending the same `X-Idempotency-Key` again returns the **existing** trade instead of creating a duplicate.  
- Trades are processed under a **fair `ReentrantLock`**, so concurrent trade requests are handled **one at a time in arrival order** (synchronous, serialized processing).  
- Side: `BUY` or `SELL`. Uses latest best ask for BUY, best bid for SELL.

### 3. Get wallet balance

- **GET** `/api/v1/wallet`  
- Optional query: `?userId=1`

Response: list of `{ "currency": "USDT", "balance": 50000 }`.

### 4. Get trading history

- **GET** `/api/v1/transactions`  
- Optional query: `?userId=1`

Response: list of trades with `id`, `symbol`, `side`, `quantity`, `price`, `totalUsdt`, `createdAt`.

### 5. Get ledger entries

- **GET** `/api/v1/ledger`  
- Optional query: `?userId=1`

Response: list of ledger rows with:

- `id` – ledger entry id  
- `userId` – user id  
- `asset` – e.g. `USDT`, `BTC`, `ETH`  
- `change` – signed change amount (e.g. `-3000`, `0.1`)  
- `balanceAfter` – asset balance after this entry  
- `referenceTx` – trade transaction id this ledger entry relates to  
- `createdAt` – timestamp

### 6. Manage supported trading symbols

- **GET** `/api/v1/symbols` – list all configured trading pairs  
- **POST** `/api/v1/symbols` – add a new trading pair  
  - Body:
    ```json
    { "symbol": "NEWCOINUSDT" }
    ```
  - Symbol is stored uppercase and immediately becomes available for price aggregation and trading.

## Table structure (H2)

- **users** – id, username  
- **wallets** – id, user_id, currency, balance (unique per user+currency)  
- **aggregated_prices** – id, symbol, best_bid, best_ask, updated_at (one row per symbol)  
- **trade_transactions** – id, user_id, symbol, side, quantity, price, total_usdt, created_at, **request_id (unique)**  
 - **ledger_entries** – id, user_id, asset, change_amount, balance_after, reference_tx, created_at  
 - **trading_pairs** – id, symbol, enabled  

## Price sources

- Binance: `https://api.binance.com/api/v3/ticker/bookTicker`
- Huobi: `https://api.huobi.pro/market/tickers`

Overridable via `price.binance.url` and `price.huobi.url` in `application.properties`.
