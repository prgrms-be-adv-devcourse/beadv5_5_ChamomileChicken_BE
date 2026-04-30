# 예치금 (Deposit) — User 서비스

## 목차

1. [개요](#1-개요)
2. [예치금 충전 흐름](#2-예치금-충전-흐름)
3. [예치금 사용 흐름](#3-예치금-사용-흐름)
4. [예치금 환불 흐름](#4-예치금-환불-흐름)
5. [예치금 잔액 검증](#5-예치금-잔액-검증)
6. [Kafka 이벤트 소비](#6-kafka-이벤트-소비)
7. [멱등성 처리](#7-멱등성-처리)
8. [서비스 계층 구조](#8-서비스-계층-구조)
9. [엔드포인트 목록](#9-엔드포인트-목록)

---

## 1. 개요

예치금(Deposit)은 User 엔티티의 `deposit` 필드에 직접 저장되는 잔액이다.
모든 금액 연산은 `User.chargeDeposit()` / `User.deductDeposit()` 도메인 메서드를 통해 처리하며,
`@Version` Optimistic Lock으로 동시 차감 시 정합성을 보장한다.

**DepositType:**

| 값 | 설명 |
|----|------|
| `CHARGE` | 외부 결제를 통한 충전 |
| `PAYMENT` | 주문 결제 시 사용 |
| `REFUND` | 주문 취소/환불로 반환 |

**DepositStatus:**

| 값 | 설명 |
|----|------|
| `PENDING` | 처리 중 (현재는 충전 완료 전 상태만 이론적으로 사용) |
| `COMPLETED` | 처리 완료 |
| `FAILED` | 처리 실패 |

모든 이력은 `deposit_histories` 테이블에 기록된다.

---

## 2. 예치금 충전 흐름

**트리거:** Payment 서비스가 Toss 결제 승인 후 내부 API를 직접 호출

```
PUT /api/v1/deposits/internal/users/{userId}/deposit
  { amount: BigDecimal, paymentId: UUID }

DepositController.increaseDeposit()
  → DepositUseCase.increase(userId, amount, paymentId)
    → DepositChargeService.increase()
        1. userRepository.findById(userId)
        2. user.chargeDeposit(amount)         ← User.deposit += amount
        3. DepositHistory.of(user, paymentId, amount, CHARGE)
           history.updateStatus(COMPLETED)
           depositHistoryRepository.save(history)
  ← 200 OK
```

> 충전은 내부 API(`/internal/`)로만 호출 가능하다. API Gateway의 RBAC 테이블에서 외부 접근이 차단된다.

---

## 3. 예치금 사용 흐름

**트리거:** Order 서비스가 주문 생성 시 내부 API 호출

```
POST /api/v1/deposits/use
  { userId: UUID, depositAmount: BigDecimal }

DepositRestController.useDeposit()
  → DepositUseCase.use(userId, depositAmount)
    → UseDepositService.use()
        1. userRepository.findById(userId)
        2. user.deductDeposit(amount)
           └─ 잔액 부족 시: throw INSUFFICIENT_DEPOSIT (400)
        3. DepositHistory.of(user, null, amount, PAYMENT)
           history.updateStatus(COMPLETED)
           depositHistoryRepository.save(history)
  ← 200 OK { valid: true }
```

`@Version` Optimistic Lock이 동시 차감 경합을 방지한다.
Order 서비스는 사용 전에 반드시 `/validate`로 잔액을 선검증한다.

---

## 4. 예치금 환불 흐름

환불은 두 경로로 발생한다.

### 4-1. Kafka 이벤트 기반 (비동기)

Order 서비스가 Kafka `order.events` 토픽에 이벤트를 발행하면 User 서비스가 소비한다.

```
Kafka: order.events
  eventType = ORDER_DEPOSIT_REFUND_REQUESTED | ORDER_EXPIRED
  payload = { eventId, orderId, userId, depositAmount }

OrderEventsConsumer.consume()
  → DepositUseCase.refund(eventId, userId, depositAmount, orderId)
    → RefundDepositService.refund()
        1. processedEventRepository.existsById(eventId)
           → true: 멱등성 처리 — return (중복 소비 무시)
        2. user.chargeDeposit(amount)       ← User.deposit += amount
        3. DepositHistory.of(user, orderId, amount, REFUND)
           history.updateStatus(COMPLETED)
           depositHistoryRepository.save(history)
        4. processedEventRepository.save(ProcessedEvent.of(eventId))
  ← Kafka ACK
```

### 4-2. 내부 HTTP API (동기)

```
PUT /api/v1/deposits/internal/users/{userId}/refund
  { amount: BigDecimal, paymentId: UUID }

DepositController.refundDeposit()
  → DepositUseCase.refund(null, userId, amount, paymentId)
    → RefundDepositService.refund()
        eventId = null → 멱등성 검사 스킵
        user.chargeDeposit(amount)
        DepositHistory 저장 (type=REFUND)
  ← 200 OK
```

> `eventId = null`인 HTTP 경로는 멱등성 체크를 건너뛴다. 동일 요청이 중복 호출되지 않도록 호출 측에서 보장해야 한다.

---

## 5. 예치금 잔액 검증

Order 서비스가 주문 생성 전에 잔액이 충분한지 미리 확인한다.

```
POST /api/v1/deposits/validate
  { userId: UUID, depositAmount: BigDecimal }

DepositRestController.validateDeposit()
  → DepositUseCase.validate(userId, depositAmount)
    → ValidateDepositService.validate()
        user.getDeposit().compareTo(depositAmount) >= 0
  ← { valid: true | false }
```

검증만 수행하며, 실제 차감은 `/use` 호출 시 발생한다.
검증 후 실제 차감 사이에 다른 주문이 끼어들 수 있으므로 `/use`에서 최종 차감 시 재검증한다 (Optimistic Lock).

---

## 6. Kafka 이벤트 소비

**Consumer 설정:**

| 항목 | 값 |
|------|----|
| 토픽 | `order.events` |
| Group ID | `deposit-service` |
| 재시도 | FixedBackOff (1초 간격, 3회) |
| 3회 초과 시 | DLQ 라우팅 (KafkaConsumerConfig) |

**소비 이벤트:**

| `eventType` 헤더 | 처리 |
|-----------------|------|
| `ORDER_DEPOSIT_REFUND_REQUESTED` | 환불 처리 |
| `ORDER_EXPIRED` | 만료 주문 예치금 반환 |

두 이벤트 모두 `RefundDepositService.refund()` 를 동일하게 호출한다.

**이벤트 페이로드 (OrderDepositEvent):**
```json
{
  "eventId": "UUID",
  "orderId": "UUID",
  "userId": "UUID",
  "depositAmount": "BigDecimal"
}
```

처리 실패 시 예외를 그대로 던져 Kafka `DefaultErrorHandler`의 재시도 + DLQ 정책을 따른다.

---

## 7. 멱등성 처리

Kafka 메시지는 **at-least-once** 보장으로 중복 소비가 발생할 수 있다.
`processed_events` 테이블에 처리 완료된 `eventId`를 기록해 중복 처리를 방지한다.

```java
// RefundDepositService.refund()
if (eventId != null && processedEventRepository.existsById(eventId)) {
    return; // 이미 처리된 이벤트 → 무시
}
// ... 처리 ...
processedEventRepository.save(ProcessedEvent.of(eventId));
```

**ProcessedEvent 테이블:**

| 컬럼 | 설명 |
|------|------|
| `id` (PK) | Kafka 이벤트 UUID = `eventId` |
| `reg_dt` | 처리 완료 시각 |

`eventId`가 PK이므로 중복 insert 시 `DataIntegrityViolationException`이 발생해 자연스럽게 멱등성이 보장된다 (극단적 경합 상황에서도 안전).

---

## 8. 서비스 계층 구조

`DepositService`는 facade 역할만 하며, 실제 비즈니스 로직은 기능별로 분리된 서비스에 위임한다.

```
DepositUseCase (interface)
  └── DepositService (facade)
        ├── DepositChargeService   — 충전 (increase)
        ├── DepositQueryService    — 조회 (findMyDeposit, findAllDepositHistories, findDepositHistory)
        ├── RefundDepositService   — 환불 (refund + 멱등성)
        ├── UseDepositService      — 사용 (use)
        └── ValidateDepositService — 잔액 검증 (validate)
```

---

## 9. 엔드포인트 목록

### 사용자 API (JWT 필요)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/api/v1/deposits` | 내 예치금 전체 이력 조회 |
| `GET` | `/api/v1/deposits/me` | 내 현재 예치금 잔액 조회 |
| `GET` | `/api/v1/deposits/{depositHistoryId}` | 예치금 이력 단건 조회 |

### 내부 서비스 간 API (인증 불필요, 내부망 전용)

| 메서드 | 경로 | 호출자 | 설명 |
|--------|------|--------|------|
| `PUT` | `/api/v1/deposits/internal/users/{userId}/deposit` | Payment 서비스 | 예치금 충전 |
| `PUT` | `/api/v1/deposits/internal/users/{userId}/refund` | 내부 직접 호출 | 예치금 환불 (HTTP) |
| `POST` | `/api/v1/deposits/validate` | Order 서비스 | 잔액 검증 |
| `POST` | `/api/v1/deposits/use` | Order 서비스 | 예치금 차감 |

전체 request/response 스키마는 [01. User Service API 명세](../00_api-spec/01-user.md)를 참고한다.
