# K3s CI/CD 최종 가이드

## 1. 문서 목적

이 문서는 현재 저장소에서 사용 중인 CI/CD 배포 흐름과 환경변수 반영 흐름을 처음 보는 분도 이해할 수 있도록 정리한 문서입니다.

아래 내용을 빠르게 파악할 수 있도록 구성했습니다.

- 어떤 워크플로가 어떤 역할을 하는지
- 코드 배포와 환경변수 반영을 언제 어떻게 나눠서 실행해야 하는지
- 어떤 최적화가 적용되었는지
- 문제가 생기면 어디를 먼저 확인해야 하는지

## 2. 현재 배포 구조

현재 배포 구조는 크게 두 갈래로 나뉘어 있습니다.

### 1. 소스 배포 흐름

일반적인 CD 파이프라인입니다.

이 흐름에서는 아래 작업을 수행합니다.

- 서비스 jar 빌드
- Docker 이미지 빌드
- Docker Hub 푸시
- 서비스 manifest 복사
- Kubernetes manifest 적용
- rollout 완료 확인
- health check 수행

이 흐름에서는 아래 작업을 수행하지 않습니다.

- ConfigMap 갱신
- Secret 갱신
- env 파일 작성 및 관리

### 2. 환경변수 반영 흐름

`Deploy Environment` 워크플로입니다.

이 흐름에서는 아래 작업을 수행합니다.

- GitHub Secrets에서 env 내용 읽기
- 서버에 env 파일 기록
- ConfigMap / Secret 생성 또는 갱신
- 필요한 서비스 deployment 재시작

이 흐름에서는 아래 작업을 수행하지 않습니다.

- jar 빌드
- Docker 이미지 빌드
- Docker 이미지 푸시

이렇게 분리한 이유는 단순합니다.

- 코드 배포와 설정 배포의 성격이 다릅니다.
- 각 워크플로의 책임이 분명해집니다.
- 장애 원인을 파악하기 쉬워집니다.

## 3. 어떤 워크플로를 사용해야 하는지

### 코드가 바뀐 경우

해당 서비스의 CD 워크플로를 실행합니다.

- `CD API Gateway`
- `CD Admin Service`
- `CD AI Service`
- `CD Order Service`
- `CD Payment Service`
- `CD Product Service`
- `CD Settlement Service`
- `CD User Service`

### 환경변수나 설정이 바뀐 경우

`Deploy Environment`를 사용합니다.

예시는 아래와 같습니다.

- `COMMON_CONFIG_ENV` 변경
- `USER_SECRET_ENV` 변경
- `API_GATEWAY_CONFIG_ENV` 변경

## 4. Deploy Environment에서 선택할 수 있는 값

현재 `Deploy Environment`에서는 아래 값을 선택할 수 있습니다.

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

각 의미는 아래와 같습니다.

- `common`: 공통 env만 반영합니다.
- 개별 서비스명: 해당 서비스 env를 반영합니다.
- `all`: 공통과 전체 서비스 기준으로 반영하고 전체 재시작을 수행합니다.

## 5. Deploy Environment 입력값

현재 워크플로에는 아래 4개의 boolean 입력값이 있습니다.

- `apply_common_config`
- `apply_common_secret`
- `apply_service_config`
- `apply_service_secret`

중요한 점은 아래와 같습니다.

- 기본값은 모두 `false`입니다.
- 처음 열면 아무 항목도 선택되어 있지 않습니다.

즉, 사용자가 직접 어떤 범위를 반영할지 선택해야 합니다.

## 6. 추천 사용 예시

### 경우 1. 공통 설정만 바뀐 경우

예시:

- `service = common`
- `apply_common_config = true`

### 경우 2. 특정 서비스 설정만 바뀐 경우

예시: 게이트웨이 설정만 변경한 경우

- `service = api-gateway`
- `apply_service_config = true`

### 경우 3. 공통 설정과 서비스 설정을 함께 바꾼 경우

예시: user 서비스

- `service = user`
- `apply_common_config = true`
- `apply_service_config = true`

### 경우 4. 전체 서비스를 다시 시작해야 하는 경우

예시:

- `service = all`

현재는 체크박스를 하나도 선택하지 않아도 `all`은 전체 재시작 용도로 사용할 수 있습니다.

## 7. 어떤 최적화가 적용되었는지

기존에는 deploy 단계가 너무 많은 역할을 가지고 있어 속도와 관리성이 모두 떨어졌습니다.

지금은 아래와 같이 정리했습니다.

### 1. env 처리 작업을 CD에서 제거했습니다

기존에는 CD가 env 파일 반영까지 담당했습니다.

현재는 아래처럼 분리되어 있습니다.

- CD: 이미지와 manifest 배포만 담당합니다.
- Deploy Environment: env, ConfigMap, Secret 반영만 담당합니다.

### 2. CD에서 전체 저장소 복사를 제거했습니다

기존에는 저장소 전체를 고정 경로로 복사했습니다.

현재는 필요한 파일만 체크아웃하고, 필요한 스크립트와 manifest만 복사합니다.

### 3. sparse checkout을 적용했습니다

CD deploy job은 아래 정도만 가져옵니다.

- `.github/scripts`
- 해당 서비스의 `.github/k3s/<service>-service.yml`

### 4. concurrency를 추가했습니다

각 서비스 CD 워크플로에 `concurrency`를 추가했습니다.

효과는 아래와 같습니다.

- 같은 브랜치에서 겹치는 배포를 줄일 수 있습니다.

### 5. Gradle 캐시를 적용했습니다

CD build job에 아래 조합을 사용합니다.

- `actions/setup-java@v5`
- `cache: gradle`

### 6. 헬스체크 대기 시간을 줄였습니다

헬스체크 자체는 유지하되, 재시도 횟수와 간격을 줄여 불필요한 대기 시간을 줄였습니다.

## 8. 배포 시간은 얼마나 줄었는지

실제 체감 기준으로는 배포 시간이 기존 약 5분대에서 3분대로 줄었습니다.

주요 원인은 아래와 같습니다.

- 전체 저장소 복사 제거
- deploy job 작업 범위 축소
- Gradle 캐시 적용
- 헬스체크 대기 시간 단축
- 배포 스크립트 단순화

## 9. 현재 CD 흐름

각 CD 워크플로는 거의 같은 구조를 따릅니다.

### Build 단계

- 코드 체크아웃
- Java 21 설정
- Gradle 캐시 사용
- `clean` + `bootJar` 실행
- Docker 이미지 빌드
- Docker Hub 푸시

### Deploy 단계

- deploy 스크립트와 서비스 YAML만 체크아웃
- deploy 스크립트를 고정 경로로 복사
- 서비스 Kubernetes YAML을 고정 경로로 복사
- `deploy-app.sh <service>` 실행
- rollout 상태 확인
- health check 실행

## 10. deploy-app.sh의 현재 역할

`deploy-app.sh`는 지금 매우 단순한 스크립트입니다.

현재 수행하는 작업은 아래와 같습니다.

- 서비스 이름 입력 받기
- 고정 경로의 manifest 읽기
- `envsubst`로 이미지 변수 치환
- manifest 적용
- rollout 완료 대기
- 실패 시 진단 로그 출력

현재 수행하지 않는 작업은 아래와 같습니다.

- env 파일 작성
- ConfigMap 생성
- Secret 생성
- 설정 변경 여부에 따른 restart 판단

위 역할은 이제 `Deploy Environment`로 이동했습니다.

## 11. Deploy Environment의 현재 흐름

현재 `Deploy Environment`는 아래 순서로 동작합니다.

### 1. GitHub Secrets에서 env 내용을 읽습니다

현재 사용하는 주요 secret은 아래와 같습니다.

- `COMMON_CONFIG_ENV`
- `COMMON_SECRET_ENV`
- `ADMIN_CONFIG_ENV`
- `ADMIN_SECRET_ENV`
- `AI_CONFIG_ENV`
- `AI_SECRET_ENV`
- `API_GATEWAY_CONFIG_ENV`
- `API_GATEWAY_SECRET_ENV`
- `ORDER_CONFIG_ENV`
- `ORDER_SECRET_ENV`
- `PAYMENT_CONFIG_ENV`
- `PAYMENT_SECRET_ENV`
- `PRODUCT_CONFIG_ENV`
- `PRODUCT_SECRET_ENV`
- `SETTLEMENT_CONFIG_ENV`
- `SETTLEMENT_SECRET_ENV`
- `USER_CONFIG_ENV`
- `USER_SECRET_ENV`

### 2. 서버에 env 파일을 기록합니다

사용 스크립트는 아래와 같습니다.

- `.github/scripts/apply-env.sh`

이 스크립트는 아래 방식으로 동작합니다.

- 필요한 env 파일을 생성합니다.
- 내용이 바뀐 경우에만 갱신합니다.
- 내용이 같으면 기존 파일을 유지합니다.

### 3. ConfigMap / Secret을 적용합니다

env 파일을 기준으로 Kubernetes 리소스를 생성하거나 갱신합니다.

예시는 아래와 같습니다.

- `common-config`
- `common-secret`
- `api-gateway-config`
- `api-gateway-secret`

### 4. 선택한 deployment를 재시작합니다

동작 방식은 아래와 같습니다.

- `common`: 서비스 재시작을 수행하지 않습니다.
- 개별 서비스 선택: 해당 서비스만 재시작합니다.
- `all`: 알려진 전체 서비스 deployment를 재시작합니다.

deployment가 없으면 건너뛰고 로그를 남깁니다.

## 12. 중요한 파일

### 워크플로

- [.github/workflows/deploy-env.yml](C:\project\javaclass\.github\workflows\deploy-env.yml)
- [.github/workflows/cd-api-gateway.yml](C:\project\javaclass\.github\workflows\cd-api-gateway.yml)
- [.github/workflows/cd-admin.yml](C:\project\javaclass\.github\workflows\cd-admin.yml)
- [.github/workflows/cd-ai.yml](C:\project\javaclass\.github\workflows\cd-ai.yml)
- [.github/workflows/cd-order.yml](C:\project\javaclass\.github\workflows\cd-order.yml)
- [.github/workflows/cd-payment.yml](C:\project\javaclass\.github\workflows\cd-payment.yml)
- [.github/workflows/cd-product.yml](C:\project\javaclass\.github\workflows\cd-product.yml)
- [.github/workflows/cd-settlement.yml](C:\project\javaclass\.github\workflows\cd-settlement.yml)
- [.github/workflows/cd-user.yml](C:\project\javaclass\.github\workflows\cd-user.yml)

### 스크립트

- [.github/scripts/apply-env.sh](C:\project\javaclass\.github\scripts\apply-env.sh)
- [.github/scripts/deploy-app.sh](C:\project\javaclass\.github\scripts\deploy-app.sh)

### Kubernetes manifest

- `.github/k3s/*-service.yml`

## 13. 서버 고정 경로

현재 구조에서 중요한 서버 경로는 아래와 같습니다.

### env 파일

- `/home/ubuntu/apps/deploy/env`

### deploy 스크립트

- `/home/ubuntu/apps/deploy/scripts`

### Kubernetes service manifest

- `/home/ubuntu/apps/data/k3s-service`

### kubeconfig

- `/home/ubuntu/.kube/config`

## 14. 자주 생기는 문제와 확인 포인트

### 문제 1. env를 바꿨는데 서비스 동작이 안 바뀌는 경우

보통 원인은 아래와 같습니다.

- ConfigMap / Secret은 반영되었지만 deployment restart가 수행되지 않았습니다.

먼저 확인할 항목은 아래와 같습니다.

- `Deploy Environment`의 `Restart deployment` step

### 문제 2. `all`을 선택했는데 pod가 안 바뀌는 경우

보통 원인은 아래와 같습니다.

- restart step에서 deployment를 건너뛰었습니다.
- 대상 deployment 이름을 찾지 못했습니다.

로그에서는 아래 문구를 확인하시면 됩니다.

- `Restarting deployment: ...`
- `Deployment not found. Skip restart: ...`

### 문제 3. `Empty content, skipping` 로그가 나오는 경우

보통 원인은 아래와 같습니다.

- 해당 체크박스를 선택하지 않았습니다.
- 또는 대응하는 GitHub Secret 값이 비어 있습니다.

### 문제 4. rollout이 실패하는 경우

먼저 확인할 항목은 아래와 같습니다.

- `kubectl describe deployment`
- `kubectl describe pod`
- 현재 pod 로그
- 이전 pod 로그

## 15. 운영 기준을 간단히 정리하면

코드가 바뀐 경우에는 해당 서비스 CD를 실행합니다.

env가 바뀐 경우에는 `Deploy Environment`를 실행합니다.

둘 다 바뀐 경우에는 아래 순서로 진행하시는 편이 안전합니다.

1. 필요하면 env를 먼저 반영합니다.
2. 그 다음 서비스 CD를 실행합니다.

전체 서비스 재시작이 필요한 경우에는 아래처럼 실행하시면 됩니다.

- `Deploy Environment`
- `service = all`

## 16. 최종 정리

현재 파이프라인은 이전보다 훨씬 단순해졌습니다.

- CD는 코드와 이미지 배포만 담당합니다.
- env 워크플로는 설정과 secret 반영만 담당합니다.
- 서비스 재시작은 env deploy에서 제어합니다.
- deploy job은 필요한 작업만 수행합니다.
- 전체 실행 시간도 줄어들었습니다.

앞으로 추가 변경을 하더라도 특별한 이유가 없다면 이 역할 분리를 유지하는 편이 가장 안전합니다.
