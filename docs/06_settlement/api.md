# Settlement API

정산 서비스의 외부 호출은 API Gateway 기준 경로(`/api/v1/...`)를 사용한다.

## 정산 조회

| Method | Path                                 | 설명                        |
|--------|--------------------------------------|---------------------------|
| `GET`  | `/api/v1/settlements`                | 특정 월의 전체 정산 목록 조회         |
| `GET`  | `/api/v1/settlements/ready`          | 특정 월의 `READY` 상태 정산 목록 조회 |
| `GET`  | `/api/v1/settlements/{settlementId}` | 정산 ID 기준 단건 조회            |

### `GET /api/v1/settlements`

특정 월의 전체 정산 목록을 조회한다.

| Query Parameter | 타입                | 필수 | 설명       |
|-----------------|-------------------|----|----------|
| `month`         | `String(yyyy-MM)` | O  | 조회할 정산 월 |

응답은 `SettlementResponse` 배열이다.

### `GET /api/v1/settlements/ready`

특정 월의 `READY` 상태 정산만 조회한다. 송금 전 대상 확인에 사용한다.

| Query Parameter | 타입                | 필수 | 설명       |
|-----------------|-------------------|----|----------|
| `month`         | `String(yyyy-MM)` | O  | 조회할 정산 월 |

### `GET /api/v1/settlements/{settlementId}`

정산 ID로 단건 정산을 조회한다.

| Path Parameter | 타입     | 설명        |
|----------------|--------|-----------|
| `settlementId` | `UUID` | 조회할 정산 ID |

정산이 없으면 `SETTLEMENT_NOT_FOUND`가 발생한다.

## 판매자 정산 조회

판매자 전용 조회 API는 Gateway가 전달한 `X-User-Id`를 판매자 ID로 사용한다.

| Method | Path                                            | 설명                         |
|--------|-------------------------------------------------|----------------------------|
| `GET`  | `/api/v1/settlements/me`                        | 헤더의 판매자 ID 기준 정산 목록 페이지 조회 |
| `GET`  | `/api/v1/settlements/me/{settlementId}/details` | 특정 정산의 상세 항목 페이지 조회        |

### `GET /api/v1/settlements/me`

자신의 정산 목록을 페이지로 조회한다.

| Header      | 필수 | 설명     |
|-------------|----|--------|
| `X-User-Id` | O  | 판매자 ID |

| Query Parameter | 타입       | 필수 | 설명                         |
|-----------------|----------|----|----------------------------|
| `page`          | `Number` | X  | 페이지 번호, 기본값 `0`            |
| `size`          | `Number` | X  | 페이지 크기, 기본값 `20`, 최대 `100` |

### `GET /api/v1/settlements/me/{settlementId}/details`

자신의 특정 정산에 포함된 상세 항목을 페이지로 조회한다.

상세 항목은 `SettlementTargetCalculation`과 `SettlementTarget`을 조합해 응답한다.

| Header      | 필수 | 설명     |
|-------------|----|--------|
| `X-User-Id` | O  | 판매자 ID |

| Path Parameter | 타입     | 설명        |
|----------------|--------|-----------|
| `settlementId` | `UUID` | 조회할 정산 ID |

| Query Parameter | 타입       | 필수 | 설명                         |
|-----------------|----------|----|----------------------------|
| `page`          | `Number` | X  | 페이지 번호, 기본값 `0`            |
| `size`          | `Number` | X  | 페이지 크기, 기본값 `20`, 최대 `100` |

다른 판매자의 정산이거나 정산이 없으면 `SETTLEMENT_NOT_FOUND`가 발생한다.

## 내부 배치 API

| Method | Path                                           | 설명             |
|--------|------------------------------------------------|----------------|
| `POST` | `/api/v1/internal-batch/settlements/calculate` | 정산 계산 배치 수동 실행 |
| `POST` | `/api/v1/internal-batch/settlements/transfer`  | 정산 송금 배치 수동 실행 |

| Query Parameter   | 타입                | 필수 | 설명             |
|-------------------|-------------------|----|----------------|
| `settlementMonth` | `String(yyyy-MM)` | X  | 미입력 시 전월 기준 실행 |

### `POST /api/v1/internal-batch/settlements/calculate`

`settlementCalculateJob`을 실행한다.

응답:

```text
정산 계산 배치 실행 요청 완료
```

### `POST /api/v1/internal-batch/settlements/transfer`

`settlementTransferJob`을 실행한다.

응답:

```text
정산 송금 배치 실행 요청 완료
```

## 응답 필드 메모

| 필드                    | 설명                                                     |
|-----------------------|--------------------------------------------------------|
| `originalAmount`      | 월 정산에 포함된 원 정산 기준 금액 합계                                |
| `sellerGradeCode`     | 해당 월 정산 생성 시점에 적용된 판매자 등급                              |
| `sellerGradePolicyId` | 적용된 판매자 등급 정책 ID                                       |
| `gradeBaseAmount`     | 등급 판단에 사용된 기준 판매 금액                                    |
| `feeAmount`           | 수수료 합계                                                 |
| `feeRate`             | 월 정산 결과에 기록된 대표 수수료율                                   |
| `settlementAmount`    | 실제 판매자 정산 금액                                           |
| `status`              | `READY`, `HOLD`, `TRANSFERRING`, `SENT`, `FAILED` 중 하나 |
| `transferredAt`       | 송금 완료 시각                                               |
| `failReason`          | 송금 실패 또는 보류 사유                                         |

더 자세한 request/response 예시는 [06. Settlement Service API 명세](../00_api-spec/06-settlement.md)를 참고한다.
