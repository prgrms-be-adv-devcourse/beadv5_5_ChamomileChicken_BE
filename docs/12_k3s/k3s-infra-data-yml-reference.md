# K3s Infra Data YML Reference

## 안내

이 문서는 `.github/k3s` 아래의 인프라 데이터 관련 YAML을 이해하기 위한 참고용 문서입니다.

- 참고용 문서입니다.
- 실제 배포 환경의 최종 설정과 다를 수 있습니다.
- 운영 배포 기준은 이 문서가 아니라 실제 `.github/k3s/*.yml` 파일과 배포 스크립트입니다.

대상 파일:

- `.github/k3s/postgres.yml`
- `.github/k3s/kafka.yml`
- `.github/k3s/redis.yml`
- `.github/k3s/elasticsearch.yml`

## 목적

이 문서는 인프라성 데이터 저장소와 메시징 컴포넌트가 현재 어떤 Kubernetes 리소스로 구성되어 있는지 빠르게 파악하기 위한 요약입니다.

정리 범위:

- 어떤 리소스가 생성되는지
- 어떤 포트와 볼륨을 쓰는지
- 어떤 probe를 쓰는지
- 어떤 설정이 들어가 있는지

## 1. Postgres

파일: `.github/k3s/postgres.yml`

구성 리소스:

- `Service` `postgres-headless`
- `Service` `postgres`
- `StatefulSet` `postgres`

핵심 포인트:

- DB 포트는 `5432`입니다.
- 내부 DNS는 `postgres-headless` 또는 `postgres`를 사용합니다.
- 데이터는 `volumeClaimTemplates`의 `postgres-data`에 저장됩니다.
- 이미지로 `pgvector/pgvector:pg15`를 사용합니다.

주요 환경 변수:

- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_DB`
- `PGDATA`

상태 확인:

- `startupProbe`, `readinessProbe`, `livenessProbe` 모두 `pg_isready` 기반입니다.
- 단순 포트 오픈이 아니라 실제 Postgres 응답 가능 여부를 확인합니다.

주의:

- 현재 파일 안의 계정 정보는 예시값 형태입니다.
- 실제 운영에서는 Secret 분리 여부를 별도로 확인해야 합니다.

## 2. Kafka and Zookeeper

파일: `.github/k3s/kafka.yml`

구성 리소스:

- `Service` `zookeeper-service-headless`
- `Service` `zookeeper-service`
- `StatefulSet` `zookeeper`
- `Service` `kafka-service-headless`
- `Service` `kafka-service`
- `StatefulSet` `kafka`

Zookeeper 핵심 포인트:

- 포트는 `2181`입니다.
- 데이터 볼륨 `zookeeper-data`, 로그 볼륨 `zookeeper-log`를 사용합니다.
- `ZOOKEEPER_CLIENT_PORT`, `ZOOKEEPER_TICK_TIME` 등을 환경 변수로 설정합니다.

Zookeeper 상태 확인:

- `readinessProbe`
- `livenessProbe`
- 둘 다 `2181` TCP socket 기준입니다.

Kafka 핵심 포인트:

- 브로커 포트는 `9092`입니다.
- 데이터는 `kafka-data` PVC에 저장합니다.
- 브로커 주소는 headless service 기반 DNS로 광고됩니다.
- 현재 단일 브로커 기준 설정입니다.

주요 Kafka 환경 변수:

- `KAFKA_BROKER_ID`
- `KAFKA_ZOOKEEPER_CONNECT`
- `KAFKA_LISTENERS`
- `KAFKA_ADVERTISED_LISTENERS`
- `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR`

Kafka 상태 확인:

- `readinessProbe`
- `livenessProbe`
- 둘 다 `9092` TCP socket 기준입니다.

주의:

- 현재 구성은 단일 브로커, 단일 Zookeeper 기준입니다.
- 운영 규모가 커지면 replica, ISR, storage, listener 정책이 달라질 수 있습니다.

## 3. Redis

파일: `.github/k3s/redis.yml`

구성 리소스:

- `Service` `redis`
- `StatefulSet` `redis`

핵심 포인트:

- 포트는 `6379`입니다.
- 데이터는 `redis-storage` PVC에 저장합니다.
- `redis-server`를 직접 실행합니다.
- `appendonly yes`로 AOF를 켠 상태입니다.

현재 실행 인자:

- `--appendonly yes`
- `--bind 0.0.0.0`
- `--protected-mode no`

상태 확인:

- `startupProbe`, `readinessProbe`, `livenessProbe` 모두 `redis-cli ping` 기반입니다.

주의:

- `protected-mode no`는 내부 클러스터 통신 편의를 위한 현재 설정입니다.
- 보안 강화를 하려면 bind, 인증, ACL, 네트워크 정책을 함께 검토해야 합니다.

## 4. Elasticsearch

파일: `.github/k3s/elasticsearch.yml`

구성 리소스:

- `Service` `elasticsearch`
- `PersistentVolumeClaim` `elasticsearch-pvc`
- `Deployment` `elasticsearch`

핵심 포인트:

- 포트는 `9200`입니다.
- 데이터는 `elasticsearch-pvc`에 저장합니다.
- 현재 단일 노드 모드입니다.
- 이미지로 `ccteam2026/es-service:0.0.1`를 사용합니다.

주요 환경 변수:

- `discovery.type=single-node`
- `xpack.security.enabled=false`
- `node.store.allow_mmap=false`
- `ES_JAVA_OPTS=-Xms512m -Xmx512m`

상태 확인:

- `readinessProbe`는 `/` HTTP GET
- `livenessProbe`는 `/` HTTP GET

주의:

- 현재 보안 기능은 꺼져 있습니다.
- 보안을 켜면 인증, Secret, 애플리케이션 연결 설정, probe 방식까지 같이 조정해야 할 수 있습니다.

## 5. 공통 구조

현재 인프라 YAML 전반의 공통점:

- 모두 클러스터 내부 통신 기준입니다.
- 영속 저장소가 필요한 컴포넌트는 PVC 또는 `volumeClaimTemplates`를 사용합니다.
- 단일 인스턴스 기준이 많아서 HA 구성이 아닙니다.
- 애플리케이션 서비스들이 내부 DNS 이름으로 이 인프라들에 연결됩니다.

대표 DNS 예시:

- `postgres`
- `postgres-headless`
- `kafka-service`
- `zookeeper-service`
- `redis`
- `elasticsearch`

## 6. Monitoring 관련 파일

모니터링 관련 파일은 아래 경로에 있지만, 이 문서에서는 상세히 다루지 않습니다.

- `.github/k3s/monitoring/monitoring-values.yaml`
- `.github/k3s/monitoring/prometheus-scrape-config.yaml`
- `.github/k3s/monitoring/grafana-dashboards.yml`
- `.github/k3s/monitoring/grafana-official-dashboards.yml`

Prometheus, Grafana, scrape target, 현재 분리 서버 구조에서의 제약은 `k3s-monitoring.md`에서 따로 다룹니다.

## 7. 문서 해석 시 주의

이 문서는 구조를 빠르게 이해하기 위한 참고용 요약입니다.

반드시 같이 확인해야 하는 실제 기준:

- `.github/k3s/*.yml`
- `.github/workflows/*.yml`
- `.github/scripts/*.sh`

특히 아래 항목은 실제 배포 시점에 달라질 수 있습니다.

- 이미지 태그
- 리소스 제한
- probe 시간값
- PVC 크기
- 보안 설정
- Secret 주입 방식

## 8. 요약

현재 인프라 데이터 YAML은 Postgres, Kafka, Zookeeper, Redis, Elasticsearch를 각각 독립 리소스로 정의하고 있습니다.

다만 이 문서는 참고용이며, 실제 배포와 완전히 같다고 보면 안 됩니다. 최종 판단은 실제 manifest와 배포 워크플로우 기준으로 해야 합니다.
