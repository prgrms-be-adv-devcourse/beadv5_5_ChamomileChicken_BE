# Settlement Target 적재 로직

정산 대상(`SettlementTarget`)은 외부 REST API로 직접 생성하지 않는다. 주문/결제/환불 흐름에서 발행된 Kafka 이벤트를 `settlement` 서비스가 소비해 적재한다.

## 전체 흐름

```text
order-service
  -> settlement.events 발행
    -> settlement-service / SettlementEventsConsumer
      -> SettlementTargetEventHandler
        -> SettlementTarget 저장
```

## 소비 이벤트

| 이벤트 타입 | 설명 | 생성 타겟 |
|-------------|------|-----------|
| `SETTLEMENT_PAYMENT_COMPLETED` | 결제 완료 후 정산 대상 생성 | `SettlementTargetType.PAYMENT` |
| `SETTLEMENT_REFUND_COMPLETED` | 환불 완료 후 정산 대상 생성 | `SettlementTargetType.REFUND` |

## 적재 데이터

`SettlementTarget`에는 정산 계산에 필요한 원천 정보가 저장된다.

| 필드 | 설명 |
|------|------|
| `sourceEventId` | Kafka 이벤트 고유 ID |
| `sellerId` | 정산 대상 판매자 ID |
| `orderId` | 주문 ID |
| `paymentId` | 결제 ID |
| `refundId` | 환불 ID, 결제 타겟은 `null` |
| `productId` | 상품 ID |
| `targetType` | `PAYMENT` 또는 `REFUND` |
| `settlementMonth` | 정산 귀속 월, `yyyy-MM` |
| `settlementBaseAmount` | 정산 기준 금액 |
| `occurredAt` | 결제/환불 발생 시각 |
| `calculationStatus` | 최초 적재 시 `PENDING` |

## 멱등 처리

Kafka 메시지는 재전달될 수 있으므로 동일 이벤트 중복 적재를 막아야 한다.

현재 멱등 기준은 `SettlementTarget.sourceEventId`다.

```text
eventId
  -> SettlementTarget.sourceEventId
    -> unique constraint
```

동일 `sourceEventId`가 이미 저장되어 있으면 같은 이벤트가 다시 들어온 것으로 보고 중복 적재하지 않는다.

별도의 이벤트 수신 테이블을 두지 않고, 최종 적재 대상인 `settlement_targets` 테이블의 유니크 제약으로 멱등성을 보장한다.

## 이후 처리

적재된 `SettlementTarget`은 바로 월 정산으로 반영되지 않는다.

```text
SettlementTarget(PENDING)
  -> settlementTargetCalculationStep
    -> SettlementTargetCalculation 생성
    -> SettlementTarget.calculationStatus = CALCULATED 또는 FAILED
```

즉 Kafka 소비는 "정산 대상 적재"까지만 담당하고, 실제 수수료/등급/월 정산 계산은 배치가 담당한다.

## 실패 처리

이벤트 payload가 잘못되었거나 필수 값이 없으면 정산 타겟 생성에 실패할 수 있다.

운영 관점에서는 아래 항목을 확인한다.

- Kafka consumer 로그
- `sourceEventId` 중복 여부
- `sellerId`, `orderId`, `paymentId`, `productId` 같은 필수 식별자 누락 여부
- `settlementMonth` 형식
- `settlementBaseAmount` 부호와 target type 정합성
