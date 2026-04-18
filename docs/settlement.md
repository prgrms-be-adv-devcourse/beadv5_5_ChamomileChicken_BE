# Settlement 서비스 구조 및 흐름

## 배경 및 목적

`settlement` 서비스는 이미 적재된 정산 대상 데이터를 바탕으로 월 단위 정산을 계산하고, 판매자 송금까지 이어지는 배치 중심 서비스를 담당한다.

현재 구현에서 `settlement` 서비스는 아래 역할을 가진다.

| 항목 | 역할 |
|------|------|
| 정산 계산 | 건별 정산 계산 후 판매자 월 정산 생성 |
| 정산 조회 | 특정 월 정산 목록, READY 상태 정산, 단건 정산 조회 |
| 송금 처리 | 판매자 정산 계좌를 조회한 뒤 송금 어댑터를 통해 이체 요청 |
| 배치 스케줄링 | 정산 계산, 송금 배치의 스케줄 실행 |

---

## 현재 구현 상태

### 공개 API

| 영역 | 대표 엔드포인트 | 설명 |
|------|------------------|------|
| 정산 조회 | `GET /settlements` | 월별 정산 목록 조회 |
| 정산 조회 | `GET /settlements/ready` | READY 상태 정산 목록 조회 |
| 정산 조회 | `GET /settlements/{settlementId}` | 정산 단건 조회 |
| 배치 실행 | `POST /internal-batch/settlements/calculate` | 정산 계산 배치 수동 실행 |
| 배치 실행 | `POST /internal-batch/settlements/transfer` | 정산 송금 배치 수동 실행 |

### 현재 서비스 흐름의 큰 축

```text
Batch Scheduler / Controller
  -> Application Service
    -> Domain Repository
    -> External Client (user, transfer)
    -> SettlementTargetCalculation / Settlement / SettlementHistory / SettlementTransfer 저장
```

### 현재 핵심 특징

- 실시간 트랜잭션 처리보다 배치 처리 중심 성격이 강하다.
- 정산 계산의 기준 키는 `sellerId + settlementMonth` 조합이다.
- `SettlementTarget`은 외부 이벤트나 별도 적재 절차로 이미 저장되어 있다고 가정한다.
- 정산 계산은 `SettlementTarget -> SettlementTargetCalculation -> Settlement` 순서로 진행된다.
- 송금 단계에서는 user 서비스의 정산 계좌 조회와 transfer 서비스 호출이 함께 동작한다.

---

## 인증 및 API Gateway 전환 상태

현재 인증 책임은 개별 서비스가 아니라 `api-gateway`로 이동하는 방향이다.

정산 서비스 관점에서 보면:

- JWT 검증 자체는 서비스 내부 책임이 아니다.
- API Gateway가 인증에 성공하면 `X-User-Id`, `X-User-Role` 헤더를 downstream 서비스로 전달할 수 있다.
- 다만 현재 정산 API는 요청자의 `userId`를 실제 비즈니스 조건으로 사용하지 않는다.

즉 현재 정산 서비스는 아래 상태로 이해하면 된다.

| 항목 | 상태 |
|------|------|
| JWT 직접 파싱 | 제거 대상 |
| 현재 사용자 기반 조회 | 아직 미사용 |
| 판매자 기준 정산 계산 | 사용 중 |
| Gateway 헤더 기반 확장 가능성 | 있음 |

정리하면, 정산 서비스는 게이트웨이 전환의 영향은 받지만 주문/결제처럼 `@CurrentUser`를 즉시 강하게 활용하는 구조는 아니다.

---

## 주요 도메인 구조

### 엔티티별 역할

| 엔티티 | 설명 | 핵심 필드 |
|--------|------|-----------|
| `SettlementTarget` | 결제/환불 원천에서 적재된 정산 대상 데이터 | `sellerId`, `orderId`, `productId`, `paymentId`, `refundId`, `settlementMonth`, `grossAmount`, `targetType`, `calculationStatus` |
| `SettlementTargetCalculation` | 정산 대상 1건에 수수료 정책을 적용한 계산 결과 | `settlementTargetId`, `settlementBaseAmount`, `sellerGradeCode`, `sellerGradePolicyId`, `feeRate`, `feeAmount`, `settlementAmount` |
| `Settlement` | 판매자별 월 정산 결과 스냅샷 | `sellerId`, `settlementMonth`, `sellerGradeCode`, `sellerGradePolicyId`, `gradeBaseAmount`, `feeAmount`, `feeRate`, `settlementAmount`, `status` |
| `SettlementHistory` | 월 정산에 어떤 정산 대상이 포함되었는지 남기는 상세 이력 | `settlementId`, `settlementTargetId`, `sellerId`, `productId`, `originalAmount`, `feeAmount`, `settlementAmount`, `status` |
| `SettlementTransfer` | 송금 요청과 결과 이력 | `settlementId`, `transferStatus`, `bankCode`, `accountNumberMasked`, `amount`, `requestedAt`, `completedAt`, `failReason` |
| `SellerGradePolicy` | 최근 3개월 판매금액 구간별 등급/수수료 정책 | `gradeCode`, `minSalesAmount`, `maxSalesAmount`, `feeRate`, `version`, `active` |
| `SellerGrade` | 판매자별 마지막 계산 등급 정보 | `sellerId`, `sellerGradePolicyId`, `calculatedMonth` |

### 상태값

| 타입 | 값 |
|------|----|
| `SettlementTargetType` | `PAYMENT`, `REFUND` |
| `SettlementTargetCalculationStatus` | `PENDING`, `CALCULATED`, `FAILED` |
| `SettlementStatus` | `READY`, `HOLD`, `TRANSFERRING`, `SENT`, `FAILED` |
| `SettlementTransferStatus` | `REQUESTED`, `SENT`, `FAILED`, `HOLD` |

> 정산 서비스의 핵심 식별자는 "로그인 유저"가 아니라 "정산 대상 판매자"다.

---

## 패키지 구조 및 계층 역할

```text
presentation/controller/
  SettlementController.java
  SettlementBatchController.java

application/service/
  SettlementService.java
  SettlementCalculateService.java
  SettlementTransferService.java

application/usecase/
  SettlementUseCase.java
  SettlementCalculateUseCase.java
  SettlementTransferUseCase.java

domain/model/
  Settlement.java
  SettlementHistory.java
  SettlementTarget.java
  SettlementTargetCalculation.java
  SettlementTransfer.java
  SellerGrade.java
  SellerGradePolicy.java

domain/repository/
  SettlementRepository.java
  SettlementHistoryRepository.java
  SettlementTargetRepository.java
  SettlementTargetCalculationRepository.java
  SettlementTransferRepository.java
  SellerGradeRepository.java
  SellerGradePolicyRepository.java

infrastructure/client/
  user, settlementTransfer 연동 클라이언트

infrastructure/batch/
  SettlementBatchConfig.java
  SettlementScheduler.java
  SettlementJobExecutionListener.java

infrastructure/persistence/
  JPA Repository + Adapter
```

---

## 핵심 서비스별 책임

### SettlementService

| 기능 | 설명 |
|------|------|
| 월별 정산 조회 | `settlementMonth` 기준 전체 정산 조회 |
| READY 정산 조회 | 송금 전 상태의 정산만 조회 |
| 정산 단건 조회 | 정산 ID 기준 상세 조회 |

### SettlementCalculateService

| 기능 | 설명 |
|------|------|
| 계산 대상 조회 | `PENDING` 상태의 `SettlementTarget` 조회 |
| 건별 계산 | 결제/환불 건별 `SettlementTargetCalculation` 생성 |
| 판매 등급 산정 | 최근 3개월 판매금액 기준 등급 정책 조회 |
| 월 정산 생성 | seller/month 집계 결과로 `Settlement` 생성 |
| 구성 이력 생성 | `SettlementHistory` 저장 |
| 중복 계산 방지 | 동일 정산 대상, 동일 seller/month 정산 중복 방지 |

### SettlementTransferService

| 기능 | 설명 |
|------|------|
| READY 정산 로드 | 송금 가능한 정산만 조회 |
| 계좌 조회 | user 서비스에서 판매자 정산 계좌 조회 |
| 이체 요청 | transfer 서비스 연동 |
| 결과 반영 | 송금 성공/실패 상태 및 `SettlementTransfer` 이력 저장 |

---

## 주요 배치 흐름

### 1. 정산 계산

정산 계산 Job은 2개의 step으로 구성된다.

```text
SettlementScheduler or SettlementBatchController
  -> settlementTargetCalculationStep
    -> SettlementTarget(PENDING) 조회
    -> 건별 SettlementTargetCalculation 생성
    -> SettlementTarget.calculationStatus 갱신

  -> settlementAggregationStep
    -> SettlementTargetCalculation seller/month 집계 조회
    -> 최근 3개월 판매금액 기준 등급 정책 조회
    -> Settlement 생성
    -> SettlementHistory 생성
```

핵심 포인트:

- 계산 기준 월은 `yyyy-MM` 형식이다.
- 기본적으로 이전 달 정산을 계산하는 흐름을 가진다.
- `SettlementTargetCalculation`은 정산 대상 1건당 1건만 생성된다.
- 이미 생성된 판매자/월 정산은 중복 생성하지 않는다.
- `Settlement`에는 당시 적용된 등급 코드, 등급 정책 ID, 기준 금액이 스냅샷으로 남는다.

### 2. 정산 송금

```text
SettlementScheduler or SettlementBatchController
  -> SettlementTransferService
    -> READY 상태 정산 조회
    -> UserClient로 판매자 계좌 조회
    -> SettlementTransferClient로 송금 요청
    -> Settlement 상태 갱신
    -> SettlementTransfer 이력 저장
```

핵심 포인트:

- 송금은 `READY` 상태 정산만 대상으로 한다.
- 정산 금액이 0 이하이거나 계좌 정보가 없으면 `HOLD` 처리한다.
- 외부 송금 호출 성공 시 `SENT`, 실패 시 `FAILED` 처리한다.

---

## 외부 연동 구조

### user 서비스 연동

| 목적 | 설명 |
|------|------|
| 판매자 정산 계좌 조회 | 송금에 필요한 계좌 정보 확보 |

### transfer 서비스 연동

| 목적 | 설명 |
|------|------|
| 송금 실행 | READY 정산에 대한 실제 이체 요청 |

현재 구현에는 payment/order/product 원천 조회 클라이언트가 포함되어 있지 않으며, `SettlementTarget` 적재는 외부 적재 절차 또는 이벤트 연동을 전제로 한다.

---

## 운영 관점 메모

현재 코드 기준으로 정산 서비스는 게이트웨이 전환 과정에서 아래 항목을 함께 점검하는 것이 좋다.

- 서비스 내부 JWT 설정이 아직 실제로 필요한지
- Swagger 문서가 bearer 인증을 계속 노출해야 하는지
- Gateway 헤더 기반 사용자 식별이 필요한 API가 실제로 존재하는지
- 배치 실행 API를 내부망 전용으로만 열어둘지, 역할 기반 보호가 필요한지

특히 정산 서비스는 "요청 사용자 인증"보다 "배치 실행 권한"과 "내부망 접근 제어"가 더 중요한 서비스다.

---

## 관련 문서

- [API Gateway — 중앙집중 인증 구조](./api-gateway.md)
- [06. Settlement Service API 명세](./api-spec/06-settlement.md)
