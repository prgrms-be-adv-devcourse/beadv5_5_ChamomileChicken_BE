# Settlement 서비스 구조 및 흐름

## 배경 및 목적

`settlement` 서비스는 결제/환불 원천 데이터를 기반으로 정산 대상을 적재하고, 월 단위 정산을 계산한 뒤, 최종 송금까지 이어지는 배치 중심 서비스를 담당한다.

현재 프로젝트에서 `settlement` 서비스는 아래 역할을 가진다.

| 항목 | 역할 |
|------|------|
| 정산 대상 적재 | 결제/환불 서비스에서 정산 원천 데이터를 조회해 정산 대상 테이블에 적재 |
| 정산 계산 | 판매자별 월 정산 금액, 수수료, 상태 계산 |
| 정산 조회 | 특정 월 정산 목록, READY 상태 정산, 단건 정산 조회 |
| 송금 처리 | 판매자 정산 계좌를 조회한 뒤 송금 어댑터를 통해 이체 요청 |
| 배치 스케줄링 | 적재, 계산, 송금 배치의 스케줄 실행 |

---

## 현재 구현 상태

**주요 외부 API**

| 영역 | 대표 엔드포인트 | 설명 |
|------|------------------|------|
| 정산 조회 | `GET /settlements` | 월별 정산 목록 조회 |
| 정산 조회 | `GET /settlements/ready` | READY 상태 정산 목록 조회 |
| 정산 조회 | `GET /settlements/{settlementId}` | 정산 단건 조회 |
| 배치 실행 | `POST /internal-batch/settlements/load-targets` | 정산 대상 적재 배치 수동 실행 |
| 배치 실행 | `POST /internal-batch/settlements/calculate` | 정산 계산 배치 수동 실행 |
| 배치 실행 | `POST /internal-batch/settlements/transfer` | 정산 송금 배치 수동 실행 |

**현재 서비스 흐름의 큰 축**

```text
Batch Scheduler / Controller
  -> UseCase
    -> Application Service
      -> Domain Repository
      -> External Client (payment, order, product, user, transfer)
      -> Settlement / SettlementHistory / SettlementTarget 저장
```

**현재 핵심 특징**

- 실시간 트랜잭션 처리보다 배치 처리 중심 성격이 강하다.
- 정산 계산의 기준 키는 `sellerId + settlementMonth` 조합이다.
- 결제/환불 원천 데이터는 payment 서비스에서 커서 기반으로 읽어온다.
- 송금 단계에서는 user 서비스의 정산 계좌 조회와 transfer 서비스 호출이 함께 동작한다.
- 조회 API는 존재하지만 현재는 "요청자 userId"보다 "정산 대상 sellerId" 중심으로 모델링되어 있다.

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
| `SettlementTarget` | 결제/환불 원천에서 적재된 정산 대상 데이터 | `sellerId`, `orderId`, `productId`, `settlementMonth`, `amount`, `type` |
| `Settlement` | 판매자별 월 정산 결과 | `sellerId`, `settlementMonth`, `amount`, `fee`, `status` |
| `SettlementHistory` | 정산 상태 변경 및 송금 이력 | `settlementId`, `sellerId`, `settlementMonth`, `status`, `message` |
| `SettlementBatchCursor` | 커서 기반 적재 배치 진행 위치 저장 | `targetType`, `cursor`, `targetDate` |

### 상태값

| 타입 | 값 |
|------|----|
| `SettlementStatus` | `READY`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `SettlementTargetType` | 결제/환불 계열 정산 구분 |

> 정산 서비스의 핵심 식별자는 "로그인 유저"가 아니라 "정산 대상 판매자"다.

---

## 패키지 구조 및 계층 역할

```text
presentation/controller/
  SettlementController.java
  SettlementBatchController.java

application/service/
  SettlementService.java
  SettlementTargetLoadService.java
  SettlementCalculateService.java
  SettlementTransferService.java

application/usecase/
  SettlementUseCase.java

domain/model/
  Settlement.java
  SettlementHistory.java
  SettlementTarget.java
  SettlementBatchCursor.java

domain/repository/
  SettlementRepository.java
  SettlementHistoryRepository.java
  SettlementTargetRepository.java
  SettlementBatchCursorRepository.java

infrastructure/client/
  payment, order, product, user, settlementTransfer 연동 클라이언트

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

### SettlementTargetLoadService

| 기능 | 설명 |
|------|------|
| 결제 원천 적재 | payment 서비스 결제 정산 대상 조회 |
| 환불 원천 적재 | payment 서비스 환불 정산 대상 조회 |
| 커서 처리 | 대량 적재 시 마지막 커서 저장 |
| 중복 방지 | 기존 적재 대상과의 중복 여부 제어 |

### SettlementCalculateService

| 기능 | 설명 |
|------|------|
| 판매자별 집계 | 정산 대상을 판매자 단위로 그룹화 |
| 수수료 계산 | 정책에 따른 수수료/정산 금액 산정 |
| 정산 생성 | `Settlement`, `SettlementHistory` 생성 |
| 중복 계산 방지 | 동일 판매자/월 기준 중복 생성 방지 |

### SettlementTransferService

| 기능 | 설명 |
|------|------|
| READY 정산 로드 | 송금 가능한 정산만 조회 |
| 계좌 조회 | user 서비스에서 판매자 정산 계좌 조회 |
| 이체 요청 | transfer 서비스 연동 |
| 결과 반영 | 송금 성공/실패 상태 및 이력 저장 |

---

## 주요 배치 흐름

### 1. 정산 대상 적재

```text
SettlementScheduler or SettlementBatchController
  -> SettlementTargetLoadService
    -> PaymentClient
      -> 결제 정산 대상 조회
      -> 환불 정산 대상 조회
    -> SettlementTarget 저장
    -> SettlementBatchCursor 저장
```

핵심 포인트:

- 특정 날짜 기준으로 원천 데이터를 수집한다.
- 대량 데이터 적재를 위해 커서 기반 조회를 사용한다.
- 결제와 환불 데이터를 각각 읽어 정산 대상에 반영한다.

### 2. 정산 계산

```text
SettlementScheduler or SettlementBatchController
  -> SettlementCalculateService
    -> SettlementTargetRepository에서 월별 대상 조회
    -> sellerId 기준 그룹화
    -> 정산 금액/수수료 계산
    -> Settlement, SettlementHistory 저장
```

핵심 포인트:

- 계산 기준 월은 `yyyy-MM` 형식이다.
- 기본적으로 이전 달 정산을 계산하는 흐름을 가진다.
- 이미 생성된 판매자/월 정산은 중복 생성하지 않는다.

### 3. 정산 송금

```text
SettlementScheduler or SettlementBatchController
  -> SettlementTransferService
    -> READY 상태 정산 조회
    -> UserClient로 판매자 계좌 조회
    -> SettlementTransferClient로 송금 요청
    -> Settlement / SettlementHistory 상태 업데이트
```

핵심 포인트:

- 송금은 READY 상태 정산만 대상으로 한다.
- 판매자 계좌 정보가 없거나 송금 실패 시 실패 이력이 남아야 한다.
- 송금 단계는 외부 연동 실패에 민감하므로 재처리 전략이 중요하다.

---

## 외부 연동 구조

### payment 서비스 연동

| 목적 | 설명 |
|------|------|
| 결제 정산 대상 조회 | 월 정산 계산에 필요한 결제 원천 데이터 조회 |
| 환불 정산 대상 조회 | 차감/보정 계산을 위한 환불 원천 데이터 조회 |

### user 서비스 연동

| 목적 | 설명 |
|------|------|
| 판매자 정보 조회 | 셀러 메타데이터 확인 |
| 정산 계좌 조회 | 송금에 필요한 계좌 정보 확보 |

### product / order 서비스 연동

| 목적 | 설명 |
|------|------|
| 상품 정보 조회 | 정산 대상 상품 메타데이터 보강 |
| 주문 정보 조회 | 정산 대상 주문 정보 보강 |

### transfer 서비스 연동

| 목적 | 설명 |
|------|------|
| 송금 실행 | READY 정산에 대한 실제 이체 요청 |

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
