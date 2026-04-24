# 로컬 Docker 환경 구축 가이드

## 개요

각 마이크로서비스를 독립적인 Docker 컨테이너로 실행하는 로컬 개발 환경 구성 가이드.
서비스를 개별로 재빌드/재배포할 수 있어 MSA 구조를 그대로 유지한다.

## 전체 구성

```
docker-compose.yml
├── 인프라
│   ├── mysql        (3306)  — 서비스별 DB 8개 자동 생성
│   ├── kafka        (9092)  — 비동기 이벤트
│   ├── zookeeper           — Kafka 의존
│   ├── redis        (6379)  — 세션/캐시
│   └── elasticsearch(9200) — 상품 검색
└── 앱 서비스
    ├── api-gateway  (8080)
    ├── user-service (9003)
    ├── payment-service (9001)
    ├── product-service (9004)
    ├── order-service   (9005)
    ├── settlement-service (9002)
    ├── admin-service   (9007)
    └── ai-service      (9009)
```

## 환경변수 관리

| 파일 | 역할 | git |
|------|------|-----|
| `.env` | 시크릿/외부 키 (JWT, AWS, Toss, 소셜로그인 등) | 제외 |
| `docker-compose.yml` `environment:` | Docker 네트워크 호스트 override | 포함 |

컨테이너 내부에서는 `localhost` 대신 컨테이너 이름으로 통신한다.

| 로컬 (localhost) | Docker 컨테이너 |
|-----------------|----------------|
| `localhost:3306` | `mysql:3306` |
| `localhost:9092` | `kafka:9092` |
| `localhost:6379` | `redis:6379` |
| `localhost:9200` | `elasticsearch:9200` |
| `localhost:9003` | `user-service:9003` |

## 사전 요구사항

- Docker Desktop 실행 중
- `.env` 파일 프로젝트 루트에 존재

## 실행 방법

### 1. JAR 빌드

```bash
# 전체 빌드 (테스트 제외)
./gradlew clean bootJar -x test

# 특정 서비스만
./gradlew :service:order:clean :service:order:bootJar -x test
```

### 2. 전체 기동

```bash
docker-compose up -d
```

### 3. 상태 확인

```bash
# 전체 컨테이너 상태
docker-compose ps

# 특정 서비스 로그
docker logs -f user-service
docker logs -f order-service
```

## 특정 서비스만 재배포

코드 수정 후 해당 서비스만 재빌드해서 올린다. 나머지 컨테이너는 영향 없다.

```bash
# 1. JAR 재빌드
./gradlew :service:order:clean :service:order:bootJar -x test

# 2. 해당 컨테이너만 재시작
docker-compose up -d --build order-service
```

서비스별 명령어:

| 서비스 | 빌드 | 재배포 |
|--------|------|--------|
| user | `./gradlew :service:user:bootJar -x test` | `docker-compose up -d --build user-service` |
| payment | `./gradlew :service:payment:bootJar -x test` | `docker-compose up -d --build payment-service` |
| product | `./gradlew :service:product:bootJar -x test` | `docker-compose up -d --build product-service` |
| order | `./gradlew :service:order:bootJar -x test` | `docker-compose up -d --build order-service` |
| settlement | `./gradlew :service:settlement:bootJar -x test` | `docker-compose up -d --build settlement-service` |
| admin | `./gradlew :service:admin:bootJar -x test` | `docker-compose up -d --build admin-service` |
| ai | `./gradlew :service:ai:bootJar -x test` | `docker-compose up -d --build ai-service` |
| api-gateway | `./gradlew :api-gateway:bootJar -x test` | `docker-compose up -d --build api-gateway` |

## 종료

```bash
# 컨테이너 종료 (데이터 유지)
docker-compose down

# 컨테이너 + 볼륨 삭제 (DB 초기화)
docker-compose down -v
```

## MySQL 초기화

`docker/mysql/init.sql`이 MySQL 컨테이너 최초 실행 시 자동으로 실행되어 DB 8개를 생성한다.

```
userdb / paymentdb / settlementdb / productdb / orderdb / admindb / aidb / gatewaydb
```

볼륨이 이미 존재하면 init.sql은 재실행되지 않는다. DB를 초기화하려면:

```bash
docker-compose down -v
docker-compose up -d
```

## 자주 쓰는 명령어

```bash
# 전체 로그 실시간 확인
docker-compose logs -f

# 특정 서비스 컨테이너 접속
docker exec -it order-service sh

# MySQL 접속
docker exec -it mysql mysql -uroot -p1234

# 인프라만 기동 (앱 서비스 제외하고 로컬에서 실행할 때)
docker-compose up -d mysql kafka zookeeper redis elasticsearch
```