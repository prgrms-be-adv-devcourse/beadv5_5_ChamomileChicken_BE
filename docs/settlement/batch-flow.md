# Settlement 배치 동작 흐름

정산 배치는 크게 정산 계산 배치와 정산 송금 배치로 나뉜다.

## 배치 패키지 구조

```text
infrastructure/batch/
  config/
    SettlementCalculateJobConfig.java
    SettlementTransferJobConfig.java

  launcher/
    SettlementBatchJobLauncher.java

  scheduler/
    SettlementScheduler.java

  listener/
    SettlementJobExecutionListener.java
    SettlementStepExecutionListener.java

  processor/
    SettlementTargetCalculationItemProcessor.java
    SettlementTransferItemProcessor.java

  writer/
    SettlementTargetCalculationItemWriter.java
    SettlementAggregationItemWriter.java
    SettlementTransferItemWriter.java

  support/
    SettlementMonthResolver.java

  dto/
    SettlementTargetCalculationBatchItem.java
```

| 패키지 | 역할 |
|------|------|
| `config` | 정산 계산 Job, 정산 송금 Job과 각 Step 조립 |
| `launcher` | 스케줄러/컨트롤러에서 공통으로 사용하는 Job 실행 및 파라미터 생성 |
| `scheduler` | cron 기반 배치 실행 트리거 |
| `listener` | Job/Step 실행 로그 및 처리 건수 기록 |
| `processor` | 정산 타겟 건별 계산 처리, 송금 대상 READY 필터링 |
| `writer` | 정산 타겟 계산 결과 저장, 월 정산 chunk bulk 저장, 송금 chunk 처리 |
| `support` | 배치 공통 보조 로직 |
| `dto` | step 사이에서 전달되는 배치 전용 record |

## 실행 진입점

```text
SettlementScheduler
  -> SettlementBatchJobLauncher
    -> settlementCalculateJob 또는 settlementTransferJob

SettlementBatchController
  -> SettlementBatchJobLauncher
    -> settlementCalculateJob 또는 settlementTransferJob
```

`SettlementBatchJobLauncher`는 배치 실행 파라미터 생성과 공통 예외 처리를 담당한다. 스케줄러와 컨트롤러는 직접 `JobParameters`를 만들지 않는다.

## 정산 계산 Job

정산 계산 Job은 3개의 chunk step으로 구성된다.

```text
settlementCalculateJob
  -> settlementPaymentTargetCalculationStep
  -> settlementRefundTargetCalculationStep
  -> settlementAggregationStep
```

### 1. `settlementPaymentTargetCalculationStep`

`PAYMENT` 타입의 `SettlementTarget`을 먼저 읽어 결제 정산 계산 결과를 만든다.

```text
SettlementTarget(PAYMENT, PENDING)
  -> SettlementTargetCalculationItemProcessor
    -> PAYMENT 계산
  -> SettlementTargetCalculationItemWriter
    -> SettlementTargetCalculation 저장
    -> SettlementTarget.calculationStatus 갱신
```

### 2. `settlementRefundTargetCalculationStep`

결제 계산 step이 모두 끝난 뒤 `REFUND` 타입의 `SettlementTarget`을 읽어 환불 정산 계산 결과를 만든다.

```text
SettlementTarget(REFUND, PENDING)
  -> SettlementTargetCalculationItemProcessor
    -> REFUND 계산
  -> SettlementTargetCalculationItemWriter
    -> SettlementTargetCalculation 저장
    -> SettlementTarget.calculationStatus 갱신
```

주요 규칙:

- 결제 건은 판매자 프로모션과 적용 수수료율을 계산한다.
- 환불 건은 원 결제 계산 결과를 우선 참조한다.
- 원 결제 타겟은 있는데 원 결제 계산 결과가 아직 없으면 `FAILED` 처리하지 않고 `PENDING`으로 유지해 다음 배치에서 재시도한다.
- 원 결제 타겟이 없으면 환불 발생 시점 기준 판매자 프로모션으로 보정 계산한다.
- 계산 성공 시 `SettlementTarget.calculationStatus`는 `CALCULATED`가 된다.
- 계산 실패 시 `FAILED`로 변경하고 실패 사유를 남긴다.

### 3. `settlementAggregationStep`

`SettlementTargetCalculation`을 seller/month 기준으로 집계해 월 정산 헤더인 `Settlement`를 생성한다.

```text
SettlementTargetCalculation summary paging 조회
  -> SettlementAggregationItemWriter
    -> 현재 chunk sellerIds 추출
    -> 필요한 데이터 IN 쿼리로 bulk 조회
    -> SettlementCalculateService.createMonthlySettlements
    -> Settlement saveAll
```

월 정산 생성 시 청크 단위로 조회하는 데이터:

| 데이터 | 조회 기준 |
|--------|-----------|
| 기존 월 정산 | `settlementMonth + sellerIds IN` |
| 최근 3개월 판매금액 | `sellerIds IN + settlementMonths IN` |
| 정산 계산 상세 | `settlementMonth + sellerIds IN` |
| 판매자 등급 | `sellerIds IN` |
| 등급 정책 | active 정책 전체 조회 후 메모리 매칭 |

이 구조는 seller별 단건 조회를 반복하지 않고, 현재 chunk에 포함된 sellerIds 기준으로 필요한 데이터를 모아 가져오기 위한 것이다.

월 전체 데이터를 한 번에 메모리에 올리지 않으면서도 seller 수에 비례해 쿼리가 증가하는 N+1 문제를 줄인다.

## 정산 송금 Job

정산 송금 Job은 chunk step으로 동작한다.

```text
settlementTransferJob
  -> settlementTransferStep
    -> JpaPagingItemReader<Settlement>
    -> SettlementTransferItemProcessor
    -> SettlementTransferItemWriter
  -> settlementTransferReconcileStep
    -> JpaPagingItemReader<Settlement>
    -> SettlementTransferReconcileItemProcessor
    -> SettlementTransferReconcileItemWriter
```

송금 step의 주요 흐름:

```text
settlementMonth 기준 Settlement paging 조회
  -> READY 상태만 processor 통과
  -> user 서비스에서 판매자 정산 계좌 조회
  -> 송금 가능 여부 확인
  -> Settlement TRANSFERRING 저장
  -> SettlementTransfer REQUESTED 저장
  -> fake 외부 송금 client 호출
  -> Settlement 상태 갱신
  -> SettlementTransfer 이력 저장
```

외부 송금 호출 전 `TRANSFERRING + REQUESTED`를 먼저 저장하는 이유:

```text
READY 상태 그대로 외부 송금 호출
  -> 외부 송금 성공
  -> DB 저장 전 장애
  -> DB에는 여전히 READY
  -> 다음 배치에서 다시 송금될 수 있음

READY
  -> TRANSFERRING + REQUESTED 먼저 저장
  -> 외부 송금 호출
```

즉 `READY`는 "아직 송금 시도 전", `TRANSFERRING`은 "외부 송금 시도를 시작했고 결과 최종 확정 전" 상태로 사용한다.

`SettlementTransferItemWriter`가 호출하는 `SettlementTransferService.transferSettlements`는 chunk 처리 결과를 반환하지 않는다.
배치 처리 건수는 Spring Batch의 step metric(`readCount`, `writeCount`, `filterCount`, `skipCount`)과 `SettlementTransfer` 이력으로 확인한다.

복구 step의 주요 흐름:

```text
settlementMonth 기준 TRANSFERRING Settlement paging 조회
  -> TRANSFERRING 상태만 processor 통과
  -> SettlementTransfer latest 이력 조회
  -> fake 외부 송금 client 상태 조회
  -> SENT면 Settlement / SettlementTransfer 를 SENT로 복구
  -> FAILED면 Settlement / SettlementTransfer 를 FAILED로 복구
  -> REQUESTED 또는 NOT_FOUND면 그대로 유지 후 다음 배치에서 다시 확인
```

복구 step이 필요한 이유:

```text
TRANSFERRING + REQUESTED 저장 성공
  -> 외부 송금 API 성공
  -> SENT 저장 전 DB 장애

DB 상태
  -> Settlement = TRANSFERRING
  -> SettlementTransfer = REQUESTED

다음 배치 reconcile step
  -> 외부 송금 상태 조회
  -> SENT / FAILED 로 복구
```

상태 처리:

| 상황 | Settlement 상태 | Transfer 이력 |
|------|-----------------|---------------|
| 송금 성공 | `SENT` | `SENT` |
| 송금 실패 | `FAILED` | `FAILED` |
| 정산 금액 0 이하 | `HOLD` | `HOLD` |
| 계좌 정보 없음 | `HOLD` | `HOLD` |
| 계좌 비활성 | `HOLD` | `HOLD` |
| 외부 송금 호출 예외 | `TRANSFERRING` 유지 | `REQUESTED` 유지 |
| 외부 송금 성공/실패 후 최종 저장 실패 | `TRANSFERRING` 유지 | `REQUESTED` 유지 |

상태 흐름:

```text
READY
  -> 송금 가능하면 TRANSFERRING
  -> 송금 불가면 HOLD

TRANSFERRING
  -> 외부 송금 성공 확정 시 SENT
  -> 외부 송금 실패 확정 시 FAILED
  -> 외부 결과 미확정이면 그대로 유지
```

### fake 외부 송금 client 멱등 처리

현재 실제 지급대행 API는 연동하지 않고, `SettlementTransferPort` 구현체인 `SettlementTransferClient`가 로컬 fake 외부 송금사 역할을 한다.

fake client는 `settlementId`를 멱등키로 사용한다.

```text
idempotencyKey = settlementId
```

동일 `settlementId`로 송금 요청이 다시 들어오면 새 송금 결과를 만들지 않고 최초 결과를 그대로 반환한다.

```text
첫 요청
  -> settlementId 기준 결과 없음
  -> fake 송금 실행
  -> 결과를 메모리에 저장
  -> 결과 반환

중복 요청
  -> settlementId 기준 결과 있음
  -> 기존 결과 반환
```

상태 조회도 동일한 `settlementId`를 기준으로 수행한다.

```text
getTransferStatus(settlementId)
  -> 기존 송금 결과가 있으면 SENT 또는 FAILED 반환
  -> 없으면 NOT_FOUND 반환
```

테스트용 실패 조건:

| 조건 | 결과 |
|------|------|
| `amount <= 0` | 실패 |
| `accountNumber`가 `9999`로 끝남 | 실패 |
| 그 외 | 성공 |

이 멱등 저장소는 `ConcurrentHashMap` 기반이므로 로컬 개발/테스트 검증 목적이다.
앱 재시작이나 다중 인스턴스 환경까지 보장하는 운영용 멱등성은 아니다.
실제 지급대행 API를 붙일 때는 `settlementId` 또는 외부 송금 요청 ID를 idempotency key로 전달해야 한다.
또한 외부 송금 성공 후 DB 저장 전에 장애가 난 경우를 복구할 수 있도록 상태 조회 API를 함께 제공해야 한다.

## 스케줄 설정

기본 스케줄:

| 배치 | 기본 cron | 설명 |
|------|-----------|------|
| 정산 계산 | `0 0 2 1 * *` | 매월 1일 02:00 |
| 정산 송금 | `0 */5 2-3 2 * *` | 매월 2일 02:00~03:55, 5분마다 송금 시도 |

환경변수로 조정할 수 있다.

```text
SETTLEMENT_BATCH_CALCULATE_CRON
SETTLEMENT_BATCH_TRANSFER_CRON
SETTLEMENT_BATCH_LOCK_EXPIRE_MINUTES
```

정산 배치는 `settlement:{settlementMonth}` 락을 먼저 획득한 뒤 실행한다.
같은 정산월의 계산 배치와 송금 배치가 동시에 실행되면 같은 월 정산 데이터를 함께 변경할 수 있으므로, 월 단위 락으로 중복 실행을 막는다.
락을 획득하지 못한 실행은 `이미 실행 중인 정산 배치`로 종료되고, 송금 배치는 다음 cron 시점에 다시 시도한다.

락 획득 시에는 먼저 `settlement_batch_locks` insert를 시도한다.
성공하면 바로 락을 획득하고, unique 충돌이 발생한 경우에만 같은 `lockKey`의 만료된 락을 삭제한 뒤 한 번 더 insert를 시도한다.
매번 전체 만료 락을 삭제하지 않고 충돌 시 필요한 lock key만 정리해 불필요한 delete 작업을 줄인다.

dev 환경에서 짧은 주기로 확인할 때는 cron을 짧게 설정하되, 실제 월 정산 기준과 다르게 같은 월을 반복 계산할 수 있다는 점을 함께 확인해야 한다.

## 로그와 관측

`SettlementJobExecutionListener`는 Job 시작/종료를 기록한다.

`SettlementStepExecutionListener`는 Step 종료 시 아래 값을 기록한다.

- `readCount`
- `writeCount`
- `filterCount`
- `skipCount`

배치가 실행됐는데 결과가 예상과 다르면 먼저 Step별 read/write count를 확인한다.
