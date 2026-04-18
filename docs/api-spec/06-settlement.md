# 06. Settlement Service (9002)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

> ⚠️ Settlement 서비스 전체가 게이트웨이 미등록 — 현재 모든 경로 인증 없이 직접 접근 가능

---

## 정산 조회 (Settlement)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/settlements` | ❌ 미등록 | 월별 정산 목록 조회 |
| GET | `/settlements/ready` | ❌ 미등록 | READY 상태 정산 목록 |
| GET | `/settlements/{settlementId}` | ❌ 미등록 | 정산 상세 조회 |

---

### GET `/settlements`

**Query Parameters**: `month` (String, 형식: `yyyy-MM`)

**Response** `200 OK`
```json
{
  "data": [ ]
}
```

---

## 배치 API (Internal Batch)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/internal-batch/settlements/calculate` | ⚙️ Internal | 정산 계산 배치 실행 |
| POST | `/internal-batch/settlements/transfer` | ⚙️ Internal | 정산 송금 배치 실행 |

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| settlementMonth | String (yyyy-MM) | calculate/transfer: 정산 월 (미입력 시 전월) |

### POST `/internal-batch/settlements/calculate`

`settlementCalculateJob`을 실행한다.

- 1단계: `SettlementTarget`의 `PENDING` 건을 읽어 `SettlementTargetCalculation` 생성
- 2단계: seller/month 집계 결과로 `Settlement`, `SettlementHistory` 생성

### POST `/internal-batch/settlements/transfer`

`settlementTransferJob`을 실행한다.

- `READY` 상태 정산만 대상으로 송금을 시도
- 성공 시 `SENT`, 실패 시 `FAILED`, 송금 불가 사유가 있으면 `HOLD`
