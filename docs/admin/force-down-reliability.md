# 상품 강제 내리기 신뢰성 설계

상품 강제 내리기(FORCE_DOWN) 흐름에서 발생할 수 있는 문제를 분석하고, 각 문제를 어떻게 해결했는지 설계 결정을 기술한다.

---

## 1. Producer — 이벤트 유실 문제

### 문제

`@TransactionalEventListener(AFTER_COMMIT)` 방식으로 Kafka를 발행하면,
Admin DB 트랜잭션은 이미 커밋된 상태에서 `kafkaTemplate.send()`가 실행된다.
이 시점에 Kafka 브로커가 다운되어 있으면 이벤트가 유실되고, DB와 product 서비스 간 데이터 불일치가 발생한다.
발행 실패 시 예외를 던져도 이미 커밋된 Admin DB는 롤백되지 않는다.

### 설계 결정: Outbox 패턴

이벤트를 Kafka에 직접 발행하지 않고, **같은 DB 트랜잭션 안에** `admin_outbox_events` 테이블에 먼저 저장한다.
별도 스케줄러(`OutboxEventPoller`)가 1초 간격으로 폴링하여 Kafka에 발행하고, 성공하면 PUBLISHED로 표시한다.

```
[트랜잭션] product.forceDown() + outbox 저장 → 원자적 커밋
[스케줄러] outbox 조회 → Kafka 발행 → PUBLISHED
           발행 실패 → retryCount++ → 임계값(5회) 초과 시 FAILED (무한 재시도 방지)
```

DB에 먼저 저장되므로 Kafka가 다운되어 있어도 복구 후 반드시 발행된다.

---

## 2. Producer — Worker 노드 중복 처리 문제

### 문제

`OutboxEventPoller` 스케줄러가 여러 인스턴스에서 동시에 실행되면,
같은 outbox row를 여러 인스턴스가 동시에 조회하여 Kafka에 중복 발행할 수 있다.

### 설계 결정: FOR UPDATE SKIP LOCKED

outbox 조회 쿼리에 `FOR UPDATE SKIP LOCKED`를 적용한다.
한 인스턴스가 row를 점유하면 다른 인스턴스는 해당 row를 건너뛰고 다음 row를 처리한다.

```sql
SELECT * FROM admin_outbox_events
WHERE status = 'PENDING'
   OR (status = 'SENDING' AND last_attempt_at < :threshold)
ORDER BY created_at ASC
LIMIT 100
FOR UPDATE SKIP LOCKED
```

`SENDING` + `last_attempt_at < threshold` 조건은 인스턴스가 죽어서 SENDING 상태에서 멈춘 이벤트를 재처리하기 위함이다.

---

## 3. Producer — 재시도 & FAILED 처리

### 문제

Kafka 발행 실패 시 무한히 재시도하면 FAILED 이벤트가 쌓여 정상 이벤트 처리도 지연된다.

### 설계 결정: retryCount >= 5 초과 시 FAILED

```
발행 실패 → retryCount++ → status=PENDING (재시도 대기)
retryCount >= 5 → status=FAILED (더 이상 재시도하지 않음)
```

FAILED 이벤트는 별도로 모니터링하여 원인 파악 후 수동 재처리한다.

---

## 4. Consumer — ES 삭제 실패 처리

### 문제

product 서비스에서 DB 처리(DISABLE + soft delete)는 성공했으나
ES 인덱스 삭제가 실패하면 DB에는 삭제된 상품이 ES에는 여전히 노출되는 불일치가 발생한다.

### 설계 결정: Kafka 재시도로 위임 (보상 트랜잭션 미적용)

ES 삭제 실패 시 보상 트랜잭션으로 DB 상태를 되돌리지 않는다.

**이유**: ES는 검색 전용이다. DB가 이미 `status=DISABLE`이므로 실제 비즈니스 로직(상품 조회, 주문 등)은 DB 기준으로 차단된다. ES에 잠시 노출되는 것은 허용 가능한 수준의 일시적 불일치이며, 보상 트랜잭션으로 DB를 되돌리는 것은 오히려 "강제 내리기가 취소된 것처럼 보이는" 더 큰 문제를 야기한다.

```
[DB 트랜잭션]
  product: status=DISABLE, deleteDt=now()
  schedule: delete_dt=now() (soft-delete)
  ↓ 커밋

[ES 삭제 — 트랜잭션 밖]
  성공 → 완료
  실패 → RuntimeException 재던짐
           → Kafka FixedBackOff 재시도 (1s × 3회)
           → 재시도 소진 시 DLQ 전송
```

DB는 이미 DISABLE 상태이므로 ES 재시도 중에도 상품은 실질적으로 차단된 상태가 유지된다.

---

## 5. 상태 전이 요약

```
PENDING → SENDING (폴러가 처리 시작)
SENDING → PUBLISHED (Kafka 발행 성공)
SENDING → PENDING (발행 실패, retryCount < 5)
SENDING → FAILED (발행 실패, retryCount >= 5)
SENDING → PENDING (인스턴스 장애 후 threshold 초과 시 재처리)
```