# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Microservices e-commerce platform (`msa-shop`) built with Spring Boot 3.5.9, Java 17, Gradle multi-module. Five independent services communicate via Kafka events, backed by PostgreSQL and Redis, with AWS Cognito for authentication.

## Build & Run Commands

```bash
# Build all modules
./gradlew build

# Build a specific service
./gradlew :service:order:build

# Run tests (integration tests tagged @Tag("Integration") are excluded by default)
./gradlew test

# Run tests for a specific service
./gradlew :service:order:test

# Run a single test class
./gradlew :service:order:test --tests "com.groom.order.SomeTestClass"

# Generate JaCoCo coverage report
./gradlew jacocoTestReport

# Build bootJar for a specific service
./gradlew :service:order:bootJar

# Start local infrastructure (PostgreSQL + Redis + Kafka UI)
docker-compose up -d

# Run all services via Docker Compose
docker-compose up --build
```

## Service Ports

| Service | Local Port | Swagger |
|---------|-----------|---------|
| User    | 8081      | http://localhost:8081/swagger-ui/index.html |
| Cart    | 8082      | http://localhost:8082/swagger-ui/index.html |
| Order   | 8083      | http://localhost:8083/swagger-ui/index.html |
| Payment | 8084      | http://localhost:8084/swagger-ui/index.html |
| Product | 8085      | http://localhost:8085/swagger-ui/index.html |
| Kafka UI| 8090      | http://localhost:8090 |

All services run internally on port 8080, mapped to different external ports via docker-compose.

## Module Structure

```
service/
├── common/    # Shared library (java-library plugin, not bootable)
│              # BaseEntity, security config, Cognito/JWT, Feign, event DTOs
├── user/      # User accounts, addresses, owner/seller registration (Cognito integration)
├── cart/      # Shopping cart (Redis-based, no PostgreSQL)
├── order/     # Order management, Saga orchestration, Outbox pattern
├── payment/   # Toss Payments integration, refunds
└── product/   # Products, categories, variants, reviews, ratings (QueryDSL, Redis caching)
```

All bootable services depend on `service:common`. The `common` module uses `java-library` plugin with `api` scope so its dependencies are transitive.

## Architecture

### Event-Driven Communication
- **Kafka** for all inter-service communication
- **Event Envelope pattern** wraps domain events with metadata
- **Outbox pattern** in Order service (`OrderOutboxService`) for reliable event publishing
- **Saga pattern** for distributed transactions (Order orchestrates Payment + Product stock)
- Kafka topics: `user-events`, `order-events`, `payment-events`, `product-events`

### Inter-Service Sync Calls
- **OpenFeign** clients with **Resilience4j** circuit breaker and **Spring Retry**
- Used for synchronous queries between services (e.g., Order -> User, Order -> Product)

### Entity Patterns
- **BaseEntity** — All entities extend it; provides JPA auditing (`createdAt/By`, `updatedAt/By`) and soft delete (`deletedAt/By`, `softDelete()`)
- **UUID primary keys** on all entities
- **Soft deletes** — Never physically delete records; use `softDelete()`. Some entities use `@Where(clause = "deleted_at IS NULL")`
- **Lombok** — `@Getter`, `@SuperBuilder`, `@NoArgsConstructor(access = PROTECTED)` on entities

### Security
- **AWS Cognito** OAuth2 Resource Server with JWT validation
- `cognitoSub` field on UserEntity links to Cognito identity
- JWK Set URI configured via `AWS_COGNITO_JWK_SET_URI` environment variable

### Observability
- Spring Boot Actuator + Micrometer Prometheus metrics
- OpenTelemetry Java agent (`infra/otel/`) injected into containers
- Endpoints: `/actuator/health`, `/actuator/prometheus`

## Infrastructure Dependencies

- **PostgreSQL 15** — All services except Cart (user: `postgres`, password: `postgres`, database: `ecommerce`)
- **Redis** — Cart storage + Product review caching + Order/Payment caching
- **Kafka** — 3-node cluster on EC2 (external, not in docker-compose)
- **AWS Cognito** — Authentication provider (ap-northeast-2)
- **Toss Payments** — Payment gateway

## CI/CD

- **Jenkinsfile** — 4-stage pipeline (Prepare -> Test -> Build & Push -> GitOps)
- **Jib** — Container builds targeting `amazoncorretto:17-alpine`, pushed to AWS ECR
- **ecr-upload.sh** — Multi-arch Docker builds (`linux/amd64,linux/arm64`)
- Only changed services are built and deployed (automatic detection)

## Testing

- JUnit 5 with TestContainers (PostgreSQL, Kafka)
- Integration tests use `@Tag("Integration")` and are excluded from `./gradlew test`
- Spring Security test support available
- Awaitility for async test assertions (Order service)
