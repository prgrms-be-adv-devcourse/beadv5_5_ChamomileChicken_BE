# Settlement 문서

정산 서비스 문서는 아래 4개 문서로 분리한다.

| 문서 | 설명 |
|------|------|
| [API](./api.md) | 정산 조회, 판매자 정산 조회, 내부 배치 실행 API |
| [엔티티](./entities.md) | 정산 도메인 엔티티와 상태값 |
| [타겟 적재 로직](./target-ingestion.md) | Kafka 이벤트를 정산 타겟으로 적재하는 흐름과 멱등 처리 |
| [배치 동작 흐름](./batch-flow.md) | 정산 계산/송금 배치 흐름, chunk bulk 처리, 패키지 구조 |

현재 정산 배치 구조의 핵심은 다음과 같다.

- 결제/환불 이벤트를 `SettlementTarget`으로 적재한 뒤 월 배치에서 정산 계산 수행
- `payment/refund` 계산은 JDBC cursor + DTO 기반으로 처리
- 정책/프로모션 데이터는 batch 시작 시 preload
- `sellerGrade`, `monthlySettlement`는 seller 기준 집계 구조로 처리
- 송금 배치도 cursor 기반으로 처리해 상태 변경 중 paging skip 위험을 줄임

전체 API 상세 명세는 [06. Settlement Service API 명세](../00_api-spec/06-settlement.md)를 함께 참고한다.
