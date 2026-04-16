# 결제 흐름 신뢰성 설계

결제 흐름에서 발생할 수 있는 문제를 분석하고, 각 문제를 어떻게 해결했는지 설계 결정을 기술한다.

---

## 1. Producer — 이벤트 유실 문제

### 문제

`kafkaTemplate.send()` 시점에 Kafka 브로커가 일시적으로 다운되어 있으면 이벤트가 유실된다.  
재시도 로직이 없으면 해당 결제/주문 이벤트는 영구적으로 소실되고, 재고나 예치금이 복구되지 않는 상태로 남는다.

또한 DB 상태 변경과 Kafka 발행을 하나의 트랜잭션으로 묶을 수 없다.  
`order.pay()` 후 `kafkaTemplate.send()` 하다가 실패하면 DB는 PAID인데 이벤트는 발행되지 않는 불일치가 생긴다.

### 설계 결정: Outbox 패턴

이벤트를 Kafka에 직접 발행하지 않고, **같은 DB 트랜잭션 안에** `outbox_events` 테이블에 먼저 저장한다.  
별도 스케줄러(OutboxPublisher)가 1초 간격으로 폴링하여 Kafka에 발행하고, 성공하면 PUBLISHED로 표시한다.

```
[트랜잭션] order.pay() + outbox 저장 → 원자적 커밋
[스케줄러] outbox 조회 → Kafka 발행 → PUBLISHED
           발행 실패 → retry_count++ → 임계값 초과 시 FAILED (무한 재시도 방지)
```

DB에 먼저 저장되므로 Kafka가 다운되어 있어도 복구 후 반드시 발행된다.

### 적용 서비스

`payment-service` (payment.events), `order-service` (order.events)

---

## 2. Producer — Worker 노드 중복 처리 문제

### 문제

OutboxPublisher 스케줄러가 여러 인스턴스에서 동시에 실행되면, 같은 outbox row를 여러 인스턴스가 동시에 조회하여 Kafka에 중복 발행할 수 있다.

### 설계 결정: FOR UPDATE SKIP LOCKED

outbox 조회 쿼리에 `FOR UPDATE SKIP LOCKED`를 적용한다.  
한 인스턴스가 row를 점유하면 다른 인스턴스는 해당 row를 건너뛰고 다음 row를 처리한다.

```sql
SELECT * FROM order_outbox_events
WHERE status = 'PENDING'
   OR (status = 'SENDING' AND last_attempt_at < :threshold)
ORDER BY created_at ASC
LIMIT 100
FOR UPDATE SKIP LOCKED
```

`SENDING` + `last_attempt_at < threshold` 조건은 인스턴스가 죽어서 SENDING 상태에서 멈춘 이벤트를 재처리하기 위함이다.

---

## 3. Producer — 네트워크 재전송으로 인한 중복 발행 문제

### 문제

Kafka 발행 후 ACK를 받기 전에 네트워크가 끊기면, Producer는 동일 메시지를 재전송한다.  
브로커는 이미 저장된 메시지를 중복으로 받을 수 있다.

### 설계 결정: enable.idempotence=true

Producer에 `enable.idempotence=true`를 설정한다.  
Producer가 각 메시지에 sequence number를 부여하고, 브로커가 중복 sequence를 감지해 한 번만 저장한다.

```java
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

Outbox 재시도(앱 레벨) + idempotence(브로커 레벨) 두 계층으로 중복 발행을 최소화한다.

---

## 4. Consumer — 중복 수신 문제

### 문제

Kafka는 at-least-once 전달을 보장한다.  
컨슈머 처리 중 장애나 리밸런싱이 발생하면 오프셋이 커밋되지 않아 동일 메시지를 두 번 이상 수신할 수 있다.  
예치금 환불이나 재고 복구가 중복 처리되면 데이터 정합성이 깨진다.

### 설계 결정: processed_events 테이블 + eventId PK

각 서비스에 `processed_events` 테이블을 두고, 이벤트 처리 시 `eventId`를 PK로 저장한다.  
처리 전 `existsById(eventId)`로 중복 여부를 확인하고, 이미 존재하면 즉시 return한다.  
`existsById` 체크와 비즈니스 로직과 `processed_events` 저장을 하나의 `@Transactional`로 묶어 원자성을 보장한다.

```java
// 같은 @Transactional 안에서
if (eventId != null && processedEventRepository.existsById(eventId)) return;
order.pay();
outboxRepository.save(...);
processedEventRepository.save(ProcessedEvent.of(eventId));
```

### 동시성 처리

두 스레드가 동시에 같은 eventId로 진입해도, `processed_events`의 PK 유니크 제약으로 하나만 커밋된다.  
실패한 쪽은 예외 → 재시도 → `existsById=true` → return으로 자연스럽게 처리된다.

### 적용 서비스 및 테이블

| 서비스 | 테이블 |
|--------|--------|
| order-service | `order_processed_events` |
| product-service | `product_processed_events` |
| user-service | `user_processed_events` |

---

## 5. Consumer — 처리 실패 시 재시도 & DLQ

### 문제

컨슈머가 이벤트를 처리하다 실패하면(DB 장애, 외부 서비스 오류 등) 해당 이벤트를 그냥 넘기면 안 된다.  
그렇다고 무한 재시도하면 이후 메시지가 전혀 처리되지 않는다.

### 설계 결정: 제한된 재시도 + DLQ

`DefaultErrorHandler`에 `FixedBackOff(1000L, 3L)`을 설정해 1초 간격으로 최대 3회 재시도한다.  
3회 모두 실패하면 `DeadLetterPublishingRecoverer`가 `{topic}.dlq` 토픽으로 이동시킨다.

```
처리 실패 → 1초 후 재시도 → 1초 후 재시도 → 1초 후 재시도 → DLQ 전송
```

DLQ 전송 후에는 오프셋이 커밋되어 이후 메시지 처리가 계속된다.  
DLQ에 쌓인 메시지는 별도로 모니터링하여 수동 또는 자동으로 재처리한다.

### 롤백 보장

컨슈머 처리 중 예외가 발생하면 `@Transactional`이 롤백되어 DB 상태 변화가 없다.  
오프셋도 커밋되지 않아 Kafka가 동일 메시지를 재전달한다.

---

## 6. Consumer — 오프셋 커밋 시점

### 문제

오프셋은 Kafka에 "여기까지 처리했다"고 알리는 체크포인트다.  
처리 성공 전에 커밋하면 서버가 죽었다 살아날 때 해당 메시지를 다시 받지 못해 유실된다.  
반대로 실패한 메시지를 커밋하지 않으면 계속 같은 메시지에서 막혀 이후 메시지를 처리하지 못한다.

### 설계 결정

- **성공 시**: 처리 완료 후 오프셋 커밋 → 정상 진행
- **실패 시**: 오프셋 커밋하지 않고 재시도 → 3회 모두 실패 시 DLQ로 이동 후 오프셋 커밋

DLQ로 보내고 오프셋을 커밋해야 문제 메시지를 건너뛰고 다음 메시지를 계속 처리할 수 있다.  
DLQ에 쌓인 메시지는 별도로 확인하여 원인 파악 후 수동 재처리한다.

---

## 7. 트랜잭션 정합성 — 외부 호출과 DB 트랜잭션 혼용 문제

### 문제

PG 승인 요청(외부 HTTP)과 DB 트랜잭션을 같은 `@Transactional` 안에 묶으면,  
외부 호출 성공 후 DB 저장 실패 시 롤백이 발생해도 이미 PG에서 결제가 승인된 상태가 남는다.  
반대로 외부 호출 실패 시 롤백되어야 할 DB 변경이 이미 커밋된 상태로 남을 수도 있다.

### 설계 결정: 오케스트레이터(트랜잭션 없음) + 핸들러(@Transactional)

외부 HTTP 호출을 포함하는 오케스트레이터는 `@Transactional`을 갖지 않는다.  
DB 상태 변경 + Outbox 저장이 필요한 단계만 핸들러로 분리하여 독립된 트랜잭션으로 처리한다.

```
[오케스트레이터 — 트랜잭션 없음]
  외부 HTTP 호출 (PG, 재고 예약, 예치금 차감)
  성공/실패에 따라 핸들러 호출

[핸들러 — @Transactional]
  도메인 상태 변경 + Outbox 저장 + processed_events 저장 → 원자적 커밋
```

외부 호출과 DB 트랜잭션이 분리되어 외부 호출 실패가 DB 정합성에 영향을 주지 않는다.
