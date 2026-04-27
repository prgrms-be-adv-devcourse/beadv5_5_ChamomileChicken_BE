# K3s Workflow 가이드

## 1. 문서 목적

이 문서는 현재 저장소에서 사용하는 GitHub Actions 워크플로를 K3s 배포 관점에서 정리한 문서입니다.

주요 목적은 아래와 같습니다.

- 어떤 워크플로가 어떤 역할을 하는지 설명합니다.
- 코드 배포와 env 배포가 어떻게 분리되어 있는지 설명합니다.
- 실제 운영 시 어떤 워크플로를 실행해야 하는지 정리합니다.

## 2. 현재 워크플로 구성

현재 K3s 관련 워크플로는 크게 두 종류로 나뉩니다.

### 1. 서비스별 CD 워크플로

현재 서비스별 CD 워크플로는 아래와 같습니다.

- `cd-api-gateway.yml`
- `cd-admin.yml`
- `cd-ai.yml`
- `cd-order.yml`
- `cd-payment.yml`
- `cd-product.yml`
- `cd-settlement.yml`
- `cd-user.yml`

이 워크플로들은 공통적으로 아래 역할을 담당합니다.

- 애플리케이션 빌드
- Docker 이미지 빌드 및 푸시
- 서비스 manifest 복사
- K3s 배포
- rollout 확인
- health check

### 2. 환경변수 반영 워크플로

- `deploy-env.yml`

이 워크플로는 아래 역할을 담당합니다.

- GitHub Secrets에서 env 내용 읽기
- 서버 env 파일 갱신
- ConfigMap / Secret 생성 또는 갱신
- 필요 시 서비스 재시작

즉, 현재 구조는 아래처럼 정리되어 있습니다.

```text
코드 배포
-> 서비스별 CD 워크플로

환경변수 / 설정 반영
-> deploy-env.yml
```

## 3. 서비스별 CD 워크플로 공통 구조

서비스별 CD 워크플로는 거의 같은 구조를 따릅니다.

## 4. Build 단계

Build 단계는 GitHub hosted runner에서 실행됩니다.

현재 공통 흐름은 아래와 같습니다.

1. 저장소 체크아웃
2. Java 21 설정
3. Gradle 캐시 사용
4. `clean` + `bootJar` 실행
5. 짧은 SHA 기준 이미지 태그 생성
6. Docker Hub 로그인
7. Docker 이미지 빌드
8. Docker 이미지 푸시

### 예시: api-gateway

현재 `cd-api-gateway.yml`의 빌드 단계는 아래 흐름을 따릅니다.

```bash
./gradlew :api-gateway:clean :api-gateway:bootJar --no-daemon
```

이미지 태그는 아래 규칙을 사용합니다.

```bash
IMAGE_TAG=${GITHUB_SHA::7}
```

이미지는 아래처럼 `latest`와 짧은 SHA 태그를 함께 푸시합니다.

```bash
docker build \
  -t <DOCKERHUB_USERNAME>/chamomile-api-gateway:latest \
  -t <DOCKERHUB_USERNAME>/chamomile-api-gateway:${IMAGE_TAG} \
  ./api-gateway
```

이 방식의 목적은 아래와 같습니다.

- 최신 이미지 확인이 쉽습니다.
- 특정 배포가 어떤 커밋에서 만들어졌는지 추적할 수 있습니다.

## 5. Deploy 단계

Deploy 단계는 self-hosted runner에서 실행됩니다.

현재 runner 조건은 아래와 같습니다.

```yaml
runs-on: [ self-hosted, ec2, docker ]
```

이 단계가 self-hosted runner에서 실행되는 이유는 아래와 같습니다.

- K3s 클러스터에 직접 접근해야 합니다.
- `/home/ubuntu/.kube/config`를 사용해야 합니다.
- 서버 고정 경로에 스크립트와 manifest를 복사해야 합니다.

### 현재 Deploy 단계 공통 흐름

1. 필요한 파일만 sparse checkout
2. 서버 디렉터리 준비
3. `deploy-app.sh` 복사 및 실행 권한 부여
4. 대상 서비스 manifest 복사
5. Docker Hub 로그인
6. `deploy-app.sh <service>` 실행
7. Kubernetes 상태 확인
8. readiness health check

## 6. 현재 적용된 CD 최적화

서비스별 CD 워크플로에는 아래 최적화가 적용되어 있습니다.

### 1. concurrency

현재 각 CD 워크플로에는 `concurrency`가 적용되어 있습니다.

예시:

```yaml
concurrency:
  group: cd-api-gateway-${{ github.ref }}
  cancel-in-progress: true
```

즉, 같은 브랜치에서 중복 배포가 겹치지 않도록 제어합니다.

### 2. Gradle 캐시

현재 `actions/setup-java@v5`와 `cache: gradle`을 사용합니다.

즉, 의존성 다운로드 시간을 줄이면서 기존 빌드 방식은 그대로 유지합니다.

### 3. 전체 저장소 복사 제거

예전 구조와 달리, 지금은 저장소 전체를 서버의 고정 경로로 `rsync`하지 않습니다.

현재는 필요한 파일만 체크아웃하고 바로 복사합니다.

예시:

- `.github/scripts/deploy-app.sh`
- `.github/k3s/api-gateway-service.yml`

### 4. health check 시간 축소

CD 마지막 단계의 readiness 확인은 유지하되, 재시도 횟수와 간격은 줄였습니다.

예시: `cd-api-gateway.yml`

- 최대 3회 확인
- 2초 간격 재시도

## 7. deploy-env.yml의 현재 역할

`deploy-env.yml`은 현재 env 반영 전용 워크플로입니다.

현재 지원하는 선택값은 아래와 같습니다.

- `common`
- `admin`
- `ai`
- `api-gateway`
- `order`
- `payment`
- `product`
- `settlement`
- `user`
- `all`

현재 입력값은 아래 4개입니다.

- `apply_common_config`
- `apply_common_secret`
- `apply_service_config`
- `apply_service_secret`

기본값은 모두 `false`입니다.

## 8. deploy-env.yml 실제 동작 순서

현재 워크플로는 아래 순서로 동작합니다.

### 1. apply-env.sh 체크아웃

필요한 파일만 sparse checkout으로 내려받습니다.

### 2. 선택한 범위에 맞는 secret 내용 선택

`Select env payloads` 단계에서 아래를 결정합니다.

- 어떤 서비스가 대상인지
- 공통 env를 반영할지
- 서비스별 env를 반영할지

### 3. env 파일을 서버에 기록

`apply-env.sh`를 이용해 아래 파일을 작성하거나 갱신합니다.

- `common_config.env`
- `common_secret.env`
- `<service>_config.env`
- `<service>_secret.env`

`all`일 때는 각 서비스를 순회하면서 서비스별 env 파일을 갱신합니다.

### 4. ConfigMap / Secret 적용

변경 감지 결과에 따라 아래 리소스를 생성하거나 갱신합니다.

- `common-config`
- `common-secret`
- `<service>-config`
- `<service>-secret`

### 5. 필요 시 deployment 재시작

현재 재시작 규칙은 아래와 같습니다.

- `common`: 재시작하지 않습니다.
- 개별 서비스: 실제 env 파일 변경이 있을 때만 해당 서비스 재시작
- `all`: 전체 서비스 재시작

현재 전체 재시작 대상은 아래와 같습니다.

- `admin`
- `ai`
- `api-gateway`
- `order`
- `payment`
- `product`
- `settlement`
- `user`

## 9. 언제 어떤 워크플로를 써야 하는지

### 코드가 바뀐 경우

해당 서비스의 CD 워크플로를 실행합니다.

예시:

- 게이트웨이 코드 변경 -> `cd-api-gateway.yml`
- 주문 서비스 코드 변경 -> `cd-order.yml`

### env가 바뀐 경우

`deploy-env.yml`을 실행합니다.

예시:

- 게이트웨이 설정만 변경 -> `service = api-gateway`
- 전체 공통 설정 영향 -> `service = all`

## 10. 현재 문서 기준으로 중요한 운영 원칙

### 1. CD는 코드 배포만 담당합니다

지금 CD 워크플로는 더 이상 ConfigMap / Secret 반영을 담당하지 않습니다.

### 2. env 반영은 deploy-env.yml이 담당합니다

지금 env 반영과 서비스 재시작은 `deploy-env.yml`에서 처리합니다.

### 3. deploy-app.sh는 manifest apply 전용입니다

지금 배포 스크립트는 manifest 적용과 rollout 확인만 담당합니다.

### 4. health check는 여전히 유지합니다

배포 시간을 줄였지만, 마지막 readiness 확인은 유지하고 있습니다.

## 11. 문제 발생 시 먼저 볼 항목

워크플로 관련 문제가 생기면 아래 순서로 확인하시는 편이 좋습니다.

1. GitHub Actions 실행 로그
2. `Select env payloads` 단계 결과
3. `Show detected env changes` 단계 로그
4. `Restart deployment` 단계 로그
5. CD의 `Deploy <service> service` 단계 로그
6. CD의 `Health check` 단계 로그

## 12. 요약

현재 K3s 관련 워크플로는 아래처럼 역할이 나뉘어 있습니다.

- 서비스별 CD
  - 코드 빌드
  - 이미지 푸시
  - manifest 배포
  - rollout 및 readiness 확인
- `deploy-env.yml`
  - env 파일 반영
  - ConfigMap / Secret 갱신
  - 서비스 재시작

즉, 지금 구조는 코드 배포와 설정 반영을 분리한 구조라고 이해하시면 됩니다.
