# PayFlux

Nền tảng điều phối thanh toán (payment orchestration) cho mô hình B2B SaaS. Merchant tích hợp qua một API thống nhất; PayFlux định tuyến tới các adapter (VNPay, MoMo, Stripe), cô lập multi-tenant, và gửi webhook đáng tin cậy.

**Ngôn ngữ:** [English](README.md) | Tiếng Việt (file này)

---

## Hệ thống làm gì

PayFlux đứng giữa ứng dụng merchant và các cổng thanh toán:

1. Merchant gọi `POST /v1/payments` qua **Apache APISIX** (xác thực API key, giới hạn tần suất).
2. **payment-orchestrator** lưu giao dịch, xử lý idempotency, và gọi adapter phù hợp qua gRPC.
3. Adapter (ví dụ **vnpay-adapter**) tạo URL thanh toán sandbox thật hoặc gọi API provider.
4. Khi thanh toán thành công (ví dụ callback IPN từ VNPay), trạng thái chuyển sang `CAPTURED`.
5. Ghi **transactional outbox** trong cùng transaction DB; **Debezium** publish lên Kafka.
6. **webhook-service** consume event và POST tới URL webhook của merchant kèm chữ ký HMAC.

Multi-tenant dùng schema Postgres chung với cột `merchant_id` và **Row Level Security (RLS)**. Tenant context truyền qua `ScopedValue` (Java 21) và header do APISIX inject.

---

## Công nghệ

| Tầng | Lựa chọn |
|---|---|
| Runtime | Java 21, Spring Boot 4.0.6, virtual threads |
| API edge | Apache APISIX (key-auth, limit-count, OpenTelemetry) |
| Giao tiếp nội bộ | gRPC + Protobuf (`grpc-libs`) |
| Database | PostgreSQL 17, Flyway, MyBatis |
| Cache / idempotency | Redis |
| Sự kiện | Transactional outbox + Debezium + Kafka (KRaft) |
| Resilience | Resilience4j (VNPay query API) |
| Tracing | OpenTelemetry → Jaeger |

---

## Cấu trúc monorepo

```
SpringBoot-PayFlux/
├── payment-orchestrator/     # Core: REST API, persistence, state machine, outbox producer
├── webhook-service/          # Kafka consumer, gửi webhook, DLQ
├── merchant-config-service/  # Scaffold (tương lai: API key, routing rules)
├── payment-adapters/
│   ├── vnpay-adapter/        # VNPay sandbox (tích hợp thật)
│   ├── momo-adapter/         # Stub
│   └── stripe-adapter/       # Stub
├── grpc-libs/                # Protobuf contracts và generated stubs
├── shared-libs/              # Exception, error code, observability starter
├── core-utils/               # TenantContext, RLS interceptor, state machine
├── libs/adapter-commons/     # HMAC signer, HTTP client có timeout
├── apisix/                   # Cấu hình route APISIX
├── infra/debezium/           # Đăng ký Debezium connector
├── init-db/                  # Script khởi tạo Postgres
└── docs/                     # Kế hoạch kiến trúc, milestone, ADR
```

---

## Yêu cầu

- JDK 21
- Docker và Docker Compose
- Maven (hoặc dùng `./mvnw`)

Để test end-to-end VNPay sandbox cần thêm credential VNPay và URL public (ví dụ ngrok) cho callback IPN.

---

## Chạy nhanh

### 1. Khởi động hạ tầng

```bash
docker compose up -d etcd redis postgres flyway-payment kafka debezium apisix jaeger
```

Đăng ký Debezium outbox connector (sau khi Debezium healthy):

```bash
bash infra/debezium/register-connector.sh
```

### 2. Cấu hình secret

Mỗi service đọc secret từ file `.env` / `.env.local` riêng (gitignored). Tối thiểu:

- **vnpay-adapter**: `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, URL return/IPN
- **payment-orchestrator**: `VNPAY_HASH_SECRET` (verify IPN), thông tin database

Chi tiết biến môi trường xem trong tài liệu milestone tương ứng.

### 3. Chạy các service ứng dụng

```bash
./mvnw -pl payment-orchestrator -am spring-boot:run
./mvnw -pl payment-adapters/vnpay-adapter -am spring-boot:run
./mvnw -pl webhook-service spring-boot:run
```

### 4. Tạo payment (qua APISIX)

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

Mở `redirect_url` trong response để vào UI VNPay sandbox. Sau khi thanh toán thành công, kiểm tra log webhook-service — mặc định gửi tới `http://localhost:8084/dev/webhook`.

### 5. Build toàn bộ

```bash
./mvnw clean compile
```

---

## Bảng port

| Service | Port | Ghi chú |
|---|---|---|
| APISIX (data plane) | 9080 | Điểm vào của merchant |
| APISIX admin | 9180 | |
| APISIX dashboard | 9000 | |
| payment-orchestrator | 8081 | HTTP REST |
| vnpay-adapter | 9101 | gRPC |
| momo-adapter | 9102 | gRPC |
| stripe-adapter | 9103 | gRPC |
| webhook-service | 8084 | |
| Postgres | 5678 | Map tới 5432 trong container |
| Redis | 6379 | |
| Kafka | 9092 | |
| Debezium Connect | 8083 | |
| Jaeger UI | 16686 | |

---

## Trạng thái milestone

Sáu milestone đã lên kế hoạch đều đã triển khai trong repo:

| Milestone | Tóm tắt |
|---|---|
| M1 | gRPC contracts (`grpc-libs`) |
| M2 | Core routing engine (orchestrator → adapters) |
| M2.5 | `shared-libs` auto-configuration starter |
| M3.5 | `core-utils` (tenant context, RLS, state machine) |
| M3 | Persistence, idempotency, optimistic locking |
| M4 | VNPay sandbox, `adapter-commons`, Resilience4j, IPN |
| M5 | APISIX edge, public API `/v1/payments` |
| M6 | Transactional outbox, Debezium, webhook delivery |

Chi tiết, checklist verify, deep-dive: [docs/README.vi.md](docs/README.vi.md).

---

## Tài liệu

| Tài liệu | Mô tả |
|---|---|
| [README.md](README.md) | Tổng quan dự án (English) |
| [docs/README.md](docs/README.md) | Documentation index (English) |
| [docs/README.vi.md](docs/README.vi.md) | Mục lục tài liệu (Tiếng Việt) |


---

## License

