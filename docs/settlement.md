# Settlement 문서

정산 서비스 문서는 아래 4개 문서로 분리한다.

| 문서 | 설명 |
|------|------|
| [API](./settlement/api.md) | 정산 조회, 판매자 정산 조회, 내부 배치 실행 API |
| [엔티티](./settlement/entities.md) | 정산 도메인 엔티티와 상태값 |
| [타겟 적재 로직](./settlement/target-ingestion.md) | Kafka 이벤트를 정산 타겟으로 적재하는 흐름과 멱등 처리 |
| [배치 동작 흐름](./settlement/batch-flow.md) | 정산 계산/송금 배치 흐름, chunk bulk 처리, 패키지 구조 |

전체 API 상세 명세는 [06. Settlement Service API 명세](./api-spec/06-settlement.md)를 함께 참고한다.
