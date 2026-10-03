# Transaction API

REST API for managing accounts and transactions, built with Spring Boot + PostgreSQL.

## Requirements

- Java 21
- Docker & Docker Compose

## Running

```bash
./run
```

This builds the JAR and starts the app + PostgreSQL via Docker Compose. The API will be available at `http://localhost:8080`.

## Running locally (dev)

Start Postgres first:

```bash
docker compose up postgres -d
```

Then run the app:

```bash
./mvnw spring-boot:run
```

## Running tests

```bash
./mvnw test
```

Tests use Testcontainers (Docker required).

## API

### POST /accounts
Create an account.

```json
{ "document_number": "12345678900" }
```

Response `201`:
```json
{ "account_id": 1, "document_number": "12345678900" }
```

### GET /accounts/{accountId}
Retrieve account info.

Response `200`:
```json
{ "account_id": 1, "document_number": "12345678900" }
```

### POST /transactions
Create a transaction. Purchases and withdrawals (types 1–3) are stored with negative amounts; credit vouchers (type 4) with positive amounts.

| operation_type_id | Description                  |
|-------------------|------------------------------|
| 1                 | Normal Purchase              |
| 2                 | Purchase with Installments   |
| 3                 | Withdrawal                   |
| 4                 | Credit Voucher               |

```json
{ "account_id": 1, "operation_type_id": 4, "amount": 123.45 }
```

Response `201`:
```json
{
  "transaction_id": 1,
  "account_id": 1,
  "operation_type_id": 4,
  "amount": 123.45,
  "event_date": "2020-01-05T09:34:18Z"
}
```

## Docs

Swagger UI available at: `http://localhost:8080/swagger-ui.html`
