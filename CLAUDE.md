# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build entire project
./gradlew build

# Run all tests
./gradlew test

# Run tests for a specific service module
./gradlew :service:user:test
./gradlew :service:order:test
# etc.

# Run a single test class
./gradlew :service:user:test --tests "jabaclass.user.UserApplicationTests"

# Run the user service
./gradlew :service:user:bootRun
```

## Architecture

This is a **multi-module Gradle project** with a microservices layout. Modules are defined in `settings.gradle`:
- `common` — shared utilities
- `service:user` — user + deposit domain (port 9003, most developed)
- `service:admin`, `service:order`, `service:payment`, `service:product`, `service:settlement`, `service:file`

Each service follows **Hexagonal/Ports & Adapters** layering:

```
domain/
  model/       — JPA entities, enums
  repository/  — repository interfaces (ports)
application/
  usecase/     — use case interfaces
  service/     — implementations
  exception/   — domain-specific error codes
infrastructure/
  persistence/ — JPA repository adapters
presentation/
  controller/  — REST controllers + OpenAPI (@Tag, @Operation) interfaces
  dto/         — request/response DTOs
```

Dependencies flow inward: `presentation → application → domain ← infrastructure`.

### Key Conventions

- **Base entity:** `common/model/BaseEntity` provides `createdAt`/`updatedAt` via JPA Auditing (`@EnableJpaAuditing` in `JpaAuditingConfig`)
- **Error handling:** `GlobalExceptionHandler` in each service's `common/error/` maps `ErrorCode` enums to `ApiResponseDto`
- **DTOs:** Domain models are never exposed directly; always use DTOs with static factory methods (e.g., `DepositHistoryResponseDto.from(entity)`)
- **Lombok:** `@RequiredArgsConstructor` for constructor injection; `@Getter`, `@Builder` on entities/DTOs
- **Transactional reads:** Services use `@Transactional(readOnly = true)` for queries
- **UUIDs:** All entity IDs are `UUID` type, auto-generated with `@UuidGenerator`
- **Security:** Endpoints require Bearer token; user identity injected via `@AuthenticationPrincipal UUID userId` (never from path params)
- **API interface pattern:** Controllers implement a separate interface (e.g., `DepositApi`) that holds all OpenAPI annotations (`@Tag`, `@Operation`, `@SecurityRequirement`), keeping the controller class clean
- **Response wrapper:** All responses use `ApiResponseDto<T>` (`status`, `message`, `data`); `@JsonInclude(NON_NULL)` omits null fields
- **DTOs:** Prefer Java `record` types for immutability; use static factory methods (`DepositHistoryResponseDto.from(entity)`)
- **API docs:** Springdoc OpenAPI 3 (Swagger UI available at `/swagger-ui.html`)
- **Database:** H2 in-memory for runtime/tests

### User Service Specifics

The `user` domain and `deposit` domain coexist in `service:user`. The `deposit` domain references `User` via `@ManyToOne(fetch = FetchType.LAZY)`.

Deposit types: `DepositType.CHARGE` / `DepositType.PAYMENT` / `DepositType.REFUND`

Current deposit balance is stored on `User.deposit` (BigDecimal); `DepositHistory` tracks individual transactions.

**Deposit charge flow:** `POST /api/v1/deposits` → prepare payment (`PaymentClient.createPayment`) → confirm payment → `User.chargeDeposit(amount)` → save `DepositHistory`. The payment service (port 9001) is called via `RestTemplate` with a two-phase prepare/confirm pattern. URL configured via `payment.service.url` in `application.yml`.
