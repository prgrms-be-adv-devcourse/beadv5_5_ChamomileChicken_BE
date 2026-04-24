# Settlement 엔티티

정산 서비스의 핵심 식별자는 로그인 사용자 ID가 아니라 정산 대상 판매자 ID(`sellerId`)와 정산 월(`settlementMonth`)이다.

## 핵심 엔티티

| 엔티티 | 설명 | 핵심 필드 |
|--------|------|-----------|
| `SettlementTarget` | 결제/환불 원천 이벤트에서 적재된 정산 대상 데이터 | `sourceEventId`, `sellerId`, `orderId`, `productId`, `paymentId`, `refundId`, `settlementMonth`, `settlementBaseAmount`, `targetType`, `occurredAt`, `calculationStatus` |
| `SettlementTargetCalculation` | 정산 대상 1건을 월 정산 집계용으로 정규화한 계산 결과 | `settlementTargetId`, `settlementBaseAmount`, `appliedPromotionId`, `appliedPromotionType`, `appliedFeeRate`, `originalPaymentTargetCalculationId` |
| `Settlement` | 판매자별 월 정산 결과 스냅샷 | `sellerId`, `settlementMonth`, `sellerGradeCode`, `sellerGradePolicyId`, `gradeBaseAmount`, `feeAmount`, `feeRate`, `settlementAmount`, `status` |
| `SettlementTransfer` | 송금 요청과 결과 이력 | `settlementId`, `transferStatus`, `bankCode`, `accountNumberMasked`, `amount`, `requestedAt`, `completedAt`, `failReason` |
| `SettlementBatchLock` | 같은 정산월 배치 동시 실행 방지용 DB 락 | `lockKey`, `jobName`, `settlementMonth`, `expiresAt` |
| `SellerGradePolicy` | 최근 3개월 판매금액 구간별 등급/수수료 정책 | `gradeCode`, `minSalesAmount`, `maxSalesAmount`, `feeRate`, `version`, `active` |
| `SellerGrade` | 판매자별 마지막 적용 등급 캐시 | `sellerId`, `sellerGradePolicyId`, `calculatedMonth` |
| `SettlementPromotion` | 프로모션 마스터 정보 | `name`, `promotionType`, `feeRate`, `durationDays`, `active` |
| `SellerPromotion` | 판매자별 프로모션 적용 이력 | `sellerId`, `promotionId`, `startedAt`, `endedAt`, `active` |

## 상태값

| 타입 | 값 | 설명 |
|------|----|------|
| `SettlementTargetType` | `PAYMENT`, `REFUND` | 결제 정산 대상인지 환불 정산 대상인지 구분 |
| `SettlementTargetCalculationStatus` | `PENDING`, `CALCULATED`, `FAILED` | 정산 대상의 건별 계산 상태 |
| `SettlementStatus` | `READY`, `HOLD`, `TRANSFERRING`, `SENT`, `FAILED` | 월 정산의 송금 진행 상태 |
| `SettlementTransferStatus` | `REQUESTED`, `SENT`, `FAILED`, `HOLD` | 송금 이력의 상태 |

## 엔티티 관계 흐름

```text
SettlementTarget
  -> SettlementTargetCalculation
    -> Settlement
      -> SettlementTransfer
```

### `SettlementTarget`

Kafka 이벤트를 통해 적재되는 원천 정산 대상이다.

- 결제 성공 이벤트는 `PAYMENT` 타겟으로 적재된다.
- 환불 성공 이벤트는 `REFUND` 타겟으로 적재된다.
- `sourceEventId`는 이벤트 고유 ID이며 유니크 제약으로 중복 적재를 막는다.
- `calculationStatus`가 `PENDING`인 데이터가 정산 계산 배치의 대상이 된다.

### `SettlementTargetCalculation`

정산 대상 1건에 대한 계산 결과다.

- 결제 건은 원 정산 기준 금액과 당시 적용 프로모션/수수료율을 저장한다.
- 환불 건은 원 결제 계산 결과를 기준으로 환불 비율만큼 음수 금액을 계산한다.
- 원 결제 계산 결과가 없으면 환불 발생 시점의 판매자 프로모션을 기준으로 보정 계산한다.
- 월 정산 상세 조회는 이 엔티티와 `SettlementTarget`을 조합해 구성한다.

### `Settlement`

판매자별 월 정산 헤더다.

- `sellerId + settlementMonth` 조합이 월 정산 생성 기준이다.
- `sellerId + settlementMonth` 조합은 유니크 제약으로 보호한다.
- `originalAmount`는 월 정산에 포함된 기준 금액 합계다.
- `gradeBaseAmount`는 최근 3개월 판매금액 기준이다.
- `sellerGradeCode`, `sellerGradePolicyId`, `feeRate`는 정산 생성 시점의 정책 스냅샷이다.
- 정산 금액이 0 이하이면 송금하지 않고 `HOLD` 상태가 된다.

### `SettlementTransfer`

송금 요청과 결과 이력이다.

- 송금 가능한 `Settlement`에 대해 생성된다.
- 외부 송금 호출 전 `Settlement`를 `TRANSFERRING`으로 바꾸고, `SettlementTransfer`는 `REQUESTED` 이력으로 먼저 저장한다.
- 계좌 정보가 없거나 정산 금액이 0 이하이면 `HOLD` 이력을 남긴다.
- 송금 성공 시 `SENT`, 실패 시 `FAILED`로 남긴다.
- 외부 송금은 성공했지만 최종 DB 저장 전에 장애가 나면 `REQUESTED` 이력이 남을 수 있고, 이후 송금 복구 step이 외부 상태 조회 결과로 `SENT` 또는 `FAILED`로 정정한다.

### `SettlementBatchLock`

정산 배치 실행 전 같은 정산월을 이미 처리 중인지 확인하기 위한 락 엔티티다.

- `lockKey`는 `settlement:{settlementMonth}` 형식이다.
- `lockKey`는 유니크 제약으로 보호한다.
- 계산 배치와 송금 배치가 같은 정산월을 동시에 처리하지 못하도록 같은 lock key를 사용한다.
- `expiresAt`은 배치 서버 장애 등으로 락이 해제되지 못한 경우를 대비한 만료 시각이다.

## 등급과 프로모션

`SellerGradePolicy`는 최근 3개월 판매금액 기준 등급/수수료 정책이다. 월 정산 집계 시 active 정책을 한 번 조회하고 메모리에서 판매자별 기준 금액에 맞는 정책을 찾는다.

`SellerPromotion`과 `SettlementPromotion`은 건별 정산 계산 시 적용 수수료율을 판단하는 데 사용된다. 적용된 프로모션 정보는 `SettlementTargetCalculation`에 스냅샷으로 남는다.

### `SettlementPromotion`

정산 프로모션의 마스터 정책이다.

- 현재 신규 셀러 프로모션(`NEW_SELLER`)을 사용한다.
- `feeRate`는 할인된 수수료율이다.
- 즉 "정산 금액에서 추가 할인액을 차감"하는 구조가 아니라, 정산 계산 시 기본 수수료율 대신 더 낮은 수수료율을 적용하는 구조다.
- `durationDays`는 판매자에게 할당된 프로모션의 적용 기간 계산에 사용된다.

### `SellerPromotion`

판매자에게 실제로 할당된 프로모션 이력이다.

- 어드민에서 판매자가 SELLER로 승격되면 `USER_SELLER_APPROVED` 이벤트가 발행된다.
- 정산 서비스는 이 이벤트를 소비해 `NEW_SELLER` 프로모션을 찾아 seller에게 할당한다.
- `startedAt`은 승격 승인 시각(`approvedAt`)이다.
- `endedAt`은 `startedAt + durationDays - 1일`로 계산된다.
- 정산 계산 시 `occurredAt`이 `startedAt ~ endedAt` 구간 안에 있으면 프로모션이 적용된다.
- 이미 같은 seller에게 같은 활성 프로모션이 있으면 중복 등록하지 않는다.
