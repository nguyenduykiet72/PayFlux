# PayFlux

Payment orchestration platform for B2B SaaS. Merchants integrate through a single API; PayFlux routes payments to provider adapters (VNPay, MoMo, Stripe), enforces multi-tenant isolation, and delivers webhooks reliably.

**Languages:** English | [Tiếng Việt](README.vi.md)

---

## What it does

PayFlux sits between merchant applications and payment providers:

1. Merchant calls `POST /v1/payments` through **Apache APISIX** (API key auth, rate limiting).
2. **payment-orchestrator** persists the payment, applies idempotency, and routes to the correct adapter via gRPC.
3. Provider adapter (e.g. **vnpay-adapter**) builds a real sandbox payment URL or calls provider APIs.
4. When payment is captured (e.g. VNPay IPN callback), state transitions to `CAPTURED`.
5. A **transactional outbox** row is written in the same DB transaction; **Debezium** publishes to Kafka.
6. **webhook-service** consumes the event and POSTs to the merchant webhook URL with an HMAC signature.

Multi-tenancy uses a shared Postgres schema with `merchant_id` and **Row Level Security (RLS)**. Tenant context is propagated via `ScopedValue` (Java 21) and APISIX-injected headers.

---

## Tech stack

| Layer | Choice |
|---|---|
| Runtime | Java 21, Spring Boot 4.0.6, virtual threads |
| API edge | Apache APISIX (key-auth, limit-count, OpenTelemetry) |
| Inter-service | gRPC + Protobuf (`grpc-libs`) |
| Database | PostgreSQL 17, Flyway migrations, MyBatis |
| Cache / idempotency | Redis |
| Events | Transactional outbox + Debezium + Kafka (KRaft) |
| Resilience | Resilience4j (VNPay query API) |
| Tracing | OpenTelemetry → Jaeger |

---

## Repository layout

```
SpringBoot-PayFlux/
├── payment-orchestrator/     # Core: REST API, persistence, state machine, outbox producer
├── webhook-service/          # Kafka consumer, webhook delivery, DLQ
├── merchant-config-service/  # Scaffold (future: API keys, routing rules)
├── payment-adapters/
│   ├── vnpay-adapter/        # VNPay sandbox (real integration)
│   ├── momo-adapter/         # Stub
│   └── stripe-adapter/       # Stub
├── grpc-libs/                # Protobuf contracts and generated stubs
├── shared-libs/              # Shared exceptions, error codes, observability starter
├── core-utils/               # TenantContext, RLS interceptor, state machine
├── libs/adapter-commons/     # HMAC signer, resilient HTTP client
├── apisix/                   # APISIX declarative routes and dashboard config
├── infra/debezium/           # Debezium connector registration
├── init-db/                  # Postgres init scripts (app user, extensions)
└── docs/                     # Architecture plan, milestones, ADRs
```

---

## Prerequisites

- JDK 21
- Docker and Docker Compose
- Maven (or use `./mvnw`)

For VNPay sandbox end-to-end tests you also need VNPay sandbox credentials and a public URL (e.g. ngrok) for IPN callbacks.

---

## Quick start

### 1. Start infrastructure

```bash
docker compose up -d etcd redis postgres flyway-payment kafka debezium apisix jaeger
```

Register the Debezium outbox connector (after Debezium is healthy):

```bash
bash infra/debezium/register-connector.sh
```

### 2. Configure secrets

Each service loads secrets from its own `.env` / `.env.local` file (gitignored). At minimum:

- **vnpay-adapter**: `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, return/IPN URLs
- **payment-orchestrator**: `VNPAY_HASH_SECRET` (IPN verification), database credentials

See milestone docs for full variable lists.

### 3. Run application services

```bash
./mvnw -pl payment-orchestrator -am spring-boot:run
./mvnw -pl payment-adapters/vnpay-adapter -am spring-boot:run
./mvnw -pl webhook-service spring-boot:run
```

### 4. Create a payment (via APISIX)

```bash
curl -s -X POST http://localhost:9080/v1/payments \
  -H "apikey: demo-secret-key" \
  -H "Content-Type: application/json" \
  -d '{
    "idempotency_key": "demo-001",
    "amount_minor": 100000,
    "currency": "VND",
    "provider": "vnpay"
  }'
```

Use the `redirect_url` in the response to open the VNPay sandbox UI. After a successful payment, check webhook-service logs for delivery to the dev endpoint (`http://localhost:8084/dev/webhook` by default).

### 5. Build everything

```bash
./mvnw clean compile
```

---

## Port reference

| Service | Port | Notes |
|---|---|---|
| APISIX (data plane) | 9080 | Merchant entrypoint |
| APISIX admin | 9180 | |
| APISIX dashboard | 9000 | |
| payment-orchestrator | 8081 | HTTP REST |
| vnpay-adapter | 9101 | gRPC |
| momo-adapter | 9102 | gRPC |
| stripe-adapter | 9103 | gRPC |
| webhook-service | 8084 | |
| Postgres | 5678 | Maps to 5432 in container |
| Redis | 6379 | |
| Kafka | 9092 | |
| Debezium Connect | 8083 | |
| Jaeger UI | 16686 | |

---

## Milestone status

All six planned milestones are implemented in this repository:

| Milestone | Summary |
|---|---|
| M1 | gRPC contracts (`grpc-libs`) |
| M2 | Core routing engine (orchestrator → adapters) |
| M2.5 | `shared-libs` auto-configuration starter |
| M3.5 | `core-utils` (tenant context, RLS, state machine) |
| M3 | Persistence, idempotency, optimistic locking |
| M4 | VNPay sandbox, `adapter-commons`, Resilience4j, IPN |
| M5 | APISIX edge, public `/v1/payments` API |
| M6 | Transactional outbox, Debezium, webhook delivery |

Details, verify checklists, and deep-dives: [docs/README.md](docs/README.md).

---

## Documentation

| Document | Description |
|---|---|
| [docs/README.md](docs/README.md) | Documentation index (English) |
| [docs/README.vi.md](docs/README.vi.md) | Mục lục tài liệu (Tiếng Việt) |


---

## License
