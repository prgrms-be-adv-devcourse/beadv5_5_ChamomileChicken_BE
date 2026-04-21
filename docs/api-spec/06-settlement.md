# 06. Settlement Service (9002)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

> ⚠️ Settlement 서비스 전체가 게이트웨이 미등록 상태이므로 현재 모든 경로는 직접 접근 기준으로 열려 있다.

> 정산 대상(`SettlementTarget`) 적재는 외부 REST 호출이 아니라 Kafka `settlement.events` 소비로 들어온다.

---

## 정산 조회 (Settlement)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/settlements` | ❌ 미등록 | 특정 월의 전체 정산 목록 조회 |
| GET | `/settlements/ready` | ❌ 미등록 | 특정 월의 `READY` 상태 정산 목록 조회 |
| GET | `/settlements/{settlementId}` | ❌ 미등록 | 정산 단건 조회 |
| GET | `/seller/settlements` | ✅ JWT | 헤더의 판매자 ID 기준 정산 목록 페이지 조회 |
| GET | `/seller/settlements/{settlementId}/details` | ✅ JWT | 헤더의 판매자 ID 기준 정산 상세 항목 페이지 조회 |

### GET `/settlements`

특정 월의 전체 정산 목록을 조회한다.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `month` | String (`yyyy-MM`) | O | 조회할 정산 월 |

**Response** `200 OK`

```json
[
  {
    "id": "8a4a3d3e-fc3c-4a82-a861-8cf9e9f0e111",
    "sellerId": "8d6c6d4f-5cb8-45b3-a0e7-24b4d227d111",
    "settlementMonth": "2026-03",
    "originalAmount": 150000,
    "sellerGradeCode": "BASIC",
    "sellerGradePolicyId": "3cb8a7a4-b48e-4e60-b4c0-7b13c54f1111",
    "gradeBaseAmount": 420000,
    "feeAmount": 12000,
    "feeRate": 0.08,
    "settlementAmount": 138000,
    "status": "READY",
    "transferredAt": null,
    "failReason": null
  }
]
```

응답은 래핑 객체 없이 `SettlementResponse` 배열을 그대로 반환한다.

### GET `/settlements/ready`

특정 월의 `READY` 상태 정산 목록만 조회한다.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `month` | String (`yyyy-MM`) | O | 조회할 정산 월 |

**Response** `200 OK`

```json
[
  {
    "id": "8a4a3d3e-fc3c-4a82-a861-8cf9e9f0e111",
    "sellerId": "8d6c6d4f-5cb8-45b3-a0e7-24b4d227d111",
    "settlementMonth": "2026-03",
    "originalAmount": 150000,
    "sellerGradeCode": "BASIC",
    "sellerGradePolicyId": "3cb8a7a4-b48e-4e60-b4c0-7b13c54f1111",
    "gradeBaseAmount": 420000,
    "feeAmount": 12000,
    "feeRate": 0.08,
    "settlementAmount": 138000,
    "status": "READY",
    "transferredAt": null,
    "failReason": null
  }
]
```

### GET `/settlements/{settlementId}`

정산 ID로 단건 정산을 조회한다.

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `settlementId` | UUID | 조회할 정산 ID |

**Response** `200 OK`

```json
{
  "id": "8a4a3d3e-fc3c-4a82-a861-8cf9e9f0e111",
  "sellerId": "8d6c6d4f-5cb8-45b3-a0e7-24b4d227d111",
  "settlementMonth": "2026-03",
  "originalAmount": 150000,
  "sellerGradeCode": "BASIC",
  "sellerGradePolicyId": "3cb8a7a4-b48e-4e60-b4c0-7b13c54f1111",
  "gradeBaseAmount": 420000,
  "feeAmount": 12000,
  "feeRate": 0.08,
  "settlementAmount": 138000,
  "status": "READY",
  "transferredAt": null,
  "failReason": null
}
```

**주요 에러**

| HTTP | 코드 | 설명 |
|------|------|------|
| `404` | `SETTLEMENT_NOT_FOUND` | 정산 정보를 찾을 수 없음 |

### GET `/seller/settlements`

헤더의 `X-User-Id`를 판매자 ID로 사용해 자신의 정산 목록을 페이지 조회한다.

**Headers**

| 헤더 | 필수 | 설명 |
|------|------|------|
| `X-User-Id` | O | 판매자 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | Number | X | 페이지 번호, 기본값 `0` |
| `size` | Number | X | 페이지 크기, 기본값 `20`, 최대 `100` |

**Response** `200 OK`

```json
{
  "items": [
    {
      "id": "8a4a3d3e-fc3c-4a82-a861-8cf9e9f0e111",
      "sellerId": "8d6c6d4f-5cb8-45b3-a0e7-24b4d227d111",
      "settlementMonth": "2026-03",
      "originalAmount": 150000,
      "sellerGradeCode": "BASIC",
      "sellerGradePolicyId": "3cb8a7a4-b48e-4e60-b4c0-7b13c54f1111",
      "gradeBaseAmount": 420000,
      "feeAmount": 12000,
      "feeRate": 0.08,
      "settlementAmount": 138000,
      "status": "READY",
      "transferredAt": null,
      "failReason": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

### GET `/seller/settlements/{settlementId}/details`

헤더의 `X-User-Id`를 판매자 ID로 사용해 자신의 정산 상세 항목을 페이지 조회한다.

**Headers**

| 헤더 | 필수 | 설명 |
|------|------|------|
| `X-User-Id` | O | 판매자 ID |

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `settlementId` | UUID | 조회할 정산 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | Number | X | 페이지 번호, 기본값 `0` |
| `size` | Number | X | 페이지 크기, 기본값 `20`, 최대 `100` |

**Response** `200 OK`

```json
{
  "settlement": {
    "id": "8a4a3d3e-fc3c-4a82-a861-8cf9e9f0e111",
    "sellerId": "8d6c6d4f-5cb8-45b3-a0e7-24b4d227d111",
    "settlementMonth": "2026-03",
    "originalAmount": 150000,
    "sellerGradeCode": "BASIC",
    "sellerGradePolicyId": "3cb8a7a4-b48e-4e60-b4c0-7b13c54f1111",
    "gradeBaseAmount": 420000,
    "feeAmount": 12000,
    "feeRate": 0.08,
    "settlementAmount": 138000,
    "status": "READY",
    "transferredAt": null,
    "failReason": null
  },
  "items": [
    {
      "settlementTargetCalculationId": "f1d9a913-2c4d-4f53-85cf-f31d8d5d1111",
      "settlementTargetId": "c43c4223-4567-49f1-b4ff-3a6b2e7c1111",
      "orderId": "7780d5d3-7d8d-49a2-b2ad-c0fd71ef1111",
      "paymentId": "2a3e7c40-d317-49a8-a61f-13d6b92f1111",
      "refundId": null,
      "productId": "c527ca4e-d334-4b4e-9f11-a479ef7d1111",
      "targetType": "PAYMENT",
      "targetSettlementBaseAmount": 150000,
      "calculatedSettlementBaseAmount": 150000,
      "calculationStatus": "CALCULATED",
      "appliedPromotionId": null,
      "appliedPromotionType": null,
      "appliedFeeRate": null,
      "originalPaymentTargetCalculationId": null,
      "occurredAt": "2026-03-10T10:00:00",
      "calculatedAt": "2026-04-01T02:10:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "hasNext": false
}
```

**주요 에러**

| HTTP | 코드 | 설명 |
|------|------|------|
| `404` | `SETTLEMENT_NOT_FOUND` | 정산 정보를 찾을 수 없거나 다른 판매자의 정산임 |

---

## 배치 API (Internal Batch)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/internal-batch/settlements/calculate` | ⚙️ Internal | 정산 계산 배치 수동 실행 |
| POST | `/internal-batch/settlements/transfer` | ⚙️ Internal | 정산 송금 배치 수동 실행 |

**공통 Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `settlementMonth` | String (`yyyy-MM`) | X | 미입력 시 전월 기준 실행 |

### POST `/internal-batch/settlements/calculate`

`settlementCalculateJob`을 실행한다.

현재 계산 배치는 2개의 step으로 동작한다.

1. `settlementTargetCalculationStep`
2. `settlementAggregationStep`

주요 동작:

- `SettlementTarget` 중 `PENDING` 상태 대상을 읽는다.
- 결제 건이면 판매자 프로모션과 등급 기준 수수료율을 반영해 `SettlementTargetCalculation`을 생성한다.
- 환불 건이면 원 결제의 계산 결과를 우선 참조해 환불 비율만큼 음수 계산을 만든다.
- 원 결제 계산 결과가 없으면 `SettlementTarget.occurredAt` 기준으로 판매자 프로모션을 다시 조회하는 2차 방어 로직을 수행한다.
- seller/month 기준으로 집계해 `Settlement`를 생성한다.
- 정산 상세가 필요할 때는 `SettlementTargetCalculation`과 `SettlementTarget`을 조합해 조회한다.
- 판매자 등급 정보가 없으면 `BASIC` 등급을 우선 사용한다.

정산 타겟 적재 메모:

- `settlement.events` 소비로 `SettlementTarget`이 저장된다.
- 이벤트 payload의 `eventId`는 `SettlementTarget.sourceEventId`로 저장된다.
- `sourceEventId` 유니크 제약으로 동일 이벤트 재수신을 멱등 처리한다.

**Response** `200 OK`

```text
정산 계산 배치 실행 요청 완료
```

**주요 에러**

| HTTP | 코드 | 설명 |
|------|------|------|
| `400` | `SETTLEMENT_BATCH_PARAMETER_INVALID` | `settlementMonth` 형식 오류 등 잘못된 파라미터 |
| `409` | `SETTLEMENT_BATCH_ALREADY_RUNNING` | 이미 같은 배치가 실행 중 |
| `500` | `SETTLEMENT_CALCULATE_FAILED` | 정산 계산 배치 실행 실패 |

### POST `/internal-batch/settlements/transfer`

`settlementTransferJob`을 실행한다.

주요 동작:

- `READY` 상태 정산만 대상으로 송금을 시도한다.
- 판매자 정산 계좌는 user 연동을 통해 조회한다.
- 송금은 transfer 연동을 통해 실행한다.
- 성공 시 `SENT`, 실패 시 `FAILED`, 송금 불가 사유가 있으면 `HOLD`로 반영한다.

**Response** `200 OK`

```text
정산 송금 배치 실행 요청 완료
```

**주요 에러**

| HTTP | 코드 | 설명 |
|------|------|------|
| `400` | `SETTLEMENT_BATCH_PARAMETER_INVALID` | `settlementMonth` 형식 오류 등 잘못된 파라미터 |
| `409` | `SETTLEMENT_BATCH_ALREADY_RUNNING` | 이미 같은 배치가 실행 중 |
| `500` | `SETTLEMENT_TRANSFER_FAILED` | 정산 송금 배치 실행 실패 |

---

## 응답 필드 메모

정산 조회 응답의 주요 필드는 아래 의미를 가진다.

| 필드 | 설명 |
|------|------|
| `originalAmount` | 월 정산에 포함된 원 정산 기준 금액 합계 |
| `sellerGradeCode` | 해당 월 정산 생성 시점에 적용된 판매자 등급 |
| `sellerGradePolicyId` | 적용된 판매자 등급 정책 ID |
| `gradeBaseAmount` | 등급 판단에 사용된 기준 판매 금액 |
| `feeAmount` | 수수료 합계 |
| `feeRate` | 월 정산 결과에 기록된 대표 수수료율 |
| `settlementAmount` | 실제 판매자 정산 금액 |
| `status` | `READY`, `HOLD`, `TRANSFERRING`, `SENT`, `FAILED` 중 하나 |
| `transferredAt` | 송금 완료 시각 |
| `failReason` | 송금 실패 또는 보류 사유 |
