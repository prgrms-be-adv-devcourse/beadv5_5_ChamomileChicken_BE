# Deploy Environment Workflow 가이드

## 1. 문서 목적

이 문서는 `.github/workflows/deploy-env.yml`의 역할과 실행 흐름을 정리한 문서입니다.

`deploy-env.yml`은 애플리케이션 이미지를 새로 빌드하거나 서비스 manifest를 배포하는 워크플로가 아닙니다.

역할은 아래 세 가지입니다.

- GitHub Secrets에 저장된 env 내용을 서버 env 파일로 동기화합니다.
- env 파일을 기준으로 Kubernetes ConfigMap / Secret을 생성하거나 갱신합니다.
- 선택한 deployment를 재시작해서 변경된 설정을 Pod에 반영합니다.

## 2. 언제 사용하는지

아래처럼 코드가 아니라 설정값이 바뀐 경우 사용합니다.

- `COMMON_CONFIG_ENV` 변경
- `COMMON_SECRET_ENV` 변경
- `<SERVICE>_CONFIG_ENV` 변경
- `<SERVICE>_SECRET_ENV` 변경
- ConfigMap / Secret만 다시 적용해야 하는 경우
- env 변경 후 Pod 재시작이 필요한 경우

서비스 코드나 Docker 이미지가 바뀐 경우에는 서비스별 `cd-*.yml` 워크플로를 사용합니다.

## 3. 실행 대상

`workflow_dispatch` 입력값 `service`로 대상을 선택합니다.

선택 가능한 값:

- `admin`
- `ai`
- `api-gateway`
- `order`
- `payment`
- `product`
- `settlement`
- `user`
- `all`

`all`을 선택하면 모든 서비스의 서비스별 env 파일과 ConfigMap / Secret을 대상으로 처리합니다.

## 4. 입력 옵션

`deploy-env.yml`은 아래 boolean 입력값을 사용합니다.

- `apply_common_config`
- `apply_common_secret`
- `apply_service_config`
- `apply_service_secret`

각 옵션의 의미는 아래와 같습니다.

| 입력값 | 적용 대상 |
| --- | --- |
| `apply_common_config` | `COMMON_CONFIG_ENV` -> `common-config` |
| `apply_common_secret` | `COMMON_SECRET_ENV` -> `common-secret` |
| `apply_service_config` | `<SERVICE>_CONFIG_ENV` -> `<service>-config` |
| `apply_service_secret` | `<SERVICE>_SECRET_ENV` -> `<service>-secret` |

체크하지 않은 항목은 env 파일 기록과 Kubernetes 리소스 적용 대상에서 제외됩니다.

## 5. 사용하는 GitHub Secrets

공통 env:

- `COMMON_CONFIG_ENV`
- `COMMON_SECRET_ENV`

서비스별 env:

- `ADMIN_CONFIG_ENV`, `ADMIN_SECRET_ENV`
- `AI_CONFIG_ENV`, `AI_SECRET_ENV`
- `API_GATEWAY_CONFIG_ENV`, `API_GATEWAY_SECRET_ENV`
- `ORDER_CONFIG_ENV`, `ORDER_SECRET_ENV`
- `PAYMENT_CONFIG_ENV`, `PAYMENT_SECRET_ENV`
- `PRODUCT_CONFIG_ENV`, `PRODUCT_SECRET_ENV`
- `SETTLEMENT_CONFIG_ENV`, `SETTLEMENT_SECRET_ENV`
- `USER_CONFIG_ENV`, `USER_SECRET_ENV`

## 6. 서버 경로

워크플로는 self-hosted runner에서 실행되며 아래 경로를 사용합니다.

- kubeconfig: `/home/ubuntu/.kube/config`
- env 파일 디렉터리: `/home/ubuntu/apps/deploy/env`

`apply-env.sh`는 env 파일을 위 디렉터리에 기록합니다.

예시:

- `/home/ubuntu/apps/deploy/env/common_config.env`
- `/home/ubuntu/apps/deploy/env/common_secret.env`
- `/home/ubuntu/apps/deploy/env/user_config.env`
- `/home/ubuntu/apps/deploy/env/user_secret.env`

## 7. 실행 흐름

현재 흐름은 아래 순서입니다.

1. `.github/scripts/apply-env.sh`만 sparse checkout합니다.
2. 선택한 service와 checkbox 값에 따라 GitHub Secrets 내용을 고릅니다.
3. `apply-env.sh`로 서버 env 파일을 기록합니다.
4. env 파일을 기준으로 ConfigMap / Secret을 `kubectl create ... --dry-run=client -o yaml | kubectl apply -f -` 방식으로 적용합니다.
5. 선택한 deployment를 `kubectl rollout restart`로 재시작합니다.
6. 적용된 ConfigMap / Secret을 조회해서 로그에 남깁니다.

## 8. all 선택 시 동작

`service = all`을 선택하면 아래 서비스들을 모두 처리합니다.

- `admin`
- `ai`
- `api-gateway`
- `order`
- `payment`
- `product`
- `settlement`
- `user`

주의할 점:

- 공통 env는 한 번만 파일로 기록합니다.
- `apply_service_config`나 `apply_service_secret`이 true이면 모든 서비스의 서비스별 env를 순회합니다.
- restart 단계에서는 모든 서비스 deployment를 재시작합니다.

## 9. ConfigMap / Secret 이름 규칙

공통 리소스:

- `common-config`
- `common-secret`

서비스별 리소스:

- `<service>-config`
- `<service>-secret`

예시:

- `user-config`
- `user-secret`
- `api-gateway-config`
- `api-gateway-secret`

이 이름은 `.github/k3s/*-service.yml`의 `envFrom` 이름과 맞아야 합니다.

## 10. 재시작 동작

워크플로는 ConfigMap / Secret 적용 후 선택한 deployment를 재시작합니다.

개별 서비스 선택 시:

```bash
kubectl rollout restart deployment/<service>-service
kubectl rollout status deployment/<service>-service --timeout=300s
```

`all` 선택 시에는 전체 서비스 deployment를 순회합니다.

현재 스크립트의 함수명은 `restart_if_changed`이지만, 실제 동작은 변경 여부를 세밀하게 비교해서 재시작하는 방식이 아닙니다. 선택한 서비스가 존재하면 재시작합니다.

## 11. 자주 생기는 문제

### Secret 값이 비어 있는 경우

`apply-env.sh`는 내용이 비어 있으면 파일을 쓰지 않고 아래 로그를 출력합니다.

```text
Empty content, skipping: ...
```

이 경우 GitHub Secret 값이 비어 있거나, 해당 checkbox를 선택하지 않았을 수 있습니다.

### env를 바꿨는데 Pod에 반영되지 않는 경우

아래를 확인합니다.

- 올바른 checkbox를 선택했는지
- ConfigMap / Secret이 실제로 갱신되었는지
- `Restart deployment` step이 성공했는지
- Pod가 새로 뜬 뒤 `envFrom`으로 같은 ConfigMap / Secret 이름을 참조하는지

### all을 선택했는데 특정 서비스가 재시작되지 않는 경우

워크플로는 deployment가 없으면 건너뜁니다.

로그에서 아래 문구를 확인합니다.

```text
Deployment not found. Skip restart: ...
```

## 12. 관련 파일

- `.github/workflows/deploy-env.yml`
- `.github/scripts/apply-env.sh`
- `.github/k3s/*-service.yml`
- `docs/12_k3s/k3s-sh.md`
- `docs/12_k3s/k3s-yml.md`

## 13. 요약

`deploy-env.yml`은 환경변수와 Kubernetes 설정 리소스를 반영하는 전용 워크플로입니다.

- 코드 배포는 서비스별 `cd-*.yml`
- env / ConfigMap / Secret 반영은 `deploy-env.yml`
- env 파일 기록은 `apply-env.sh`
- Pod 반영은 deployment restart

이 기준으로 코드 배포와 설정 배포를 분리해서 운영합니다.
