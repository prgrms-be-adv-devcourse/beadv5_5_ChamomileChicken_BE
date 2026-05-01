# K3s Shell Script 가이드

## 1. 문서 목적

이 문서는 K3s 배포 과정에서 사용하는 스크립트 2개의 현재 역할을 정리한 문서입니다.

대상 스크립트는 아래와 같습니다.

- `.github/scripts/apply-env.sh`
- `.github/scripts/deploy-app.sh`

예전에는 두 스크립트가 더 많은 역할을 담당했지만, 지금은 책임을 분리하면서 구조가 단순해졌습니다. 이 문서는 현재 저장소 기준 최신 동작만 설명합니다.

## 2. 현재 스크립트 구조

현재 구조는 아래처럼 나뉘어 있습니다.

### `apply-env.sh`

환경변수 내용을 서버의 env 파일로 기록하고, 실제로 내용이 바뀌었는지 판단합니다.

### `deploy-app.sh`

서비스 manifest를 Kubernetes에 적용하고 rollout 완료 여부를 확인합니다.

즉, 현재는 아래처럼 역할이 분리되어 있습니다.

```text
환경변수 반영
-> apply-env.sh

서비스 배포
-> deploy-app.sh
```

중요한 점은 아래와 같습니다.

- `apply-env.sh`는 더 이상 배포를 수행하지 않습니다.
- `deploy-app.sh`는 더 이상 ConfigMap / Secret을 생성하지 않습니다.

## 3. apply-env.sh

### 3.1 역할

`apply-env.sh`는 GitHub Actions에서 전달받은 env 문자열을 서버 파일로 기록하는 스크립트입니다.

현재 처리 대상은 아래와 같습니다.

- `COMMON_CONFIG_ENV`
- `COMMON_SECRET_ENV`
- `SERVICE_CONFIG_ENV`
- `SERVICE_SECRET_ENV`

이 값을 아래 파일로 저장합니다.

- `common_config.env`
- `common_secret.env`
- `<service>_config.env`
- `<service>_secret.env`

예를 들어 `admin` 서비스를 대상으로 실행하면 아래 파일을 다룹니다.

- `/home/ubuntu/apps/deploy/env/common_config.env`
- `/home/ubuntu/apps/deploy/env/common_secret.env`
- `/home/ubuntu/apps/deploy/env/admin_config.env`
- `/home/ubuntu/apps/deploy/env/admin_secret.env`

### 3.2 실행 방식

기본 사용 예시는 아래와 같습니다.

```bash
/home/ubuntu/apps/deploy/scripts/apply-env.sh admin
```

공통만 반영할 때는 인자 없이 호출할 수도 있습니다.

```bash
/home/ubuntu/apps/deploy/scripts/apply-env.sh
```

즉, 현재 스크립트는 아래 두 경우를 모두 지원합니다.

- 공통 env만 반영
- 공통 env + 특정 서비스 env 반영

### 3.3 주요 변수

현재 스크립트가 직접 사용하는 주요 변수는 아래와 같습니다.

```bash
SERVICE="${1:-}"
ENV_DIR="${ENV_DIR:-/home/ubuntu/apps/deploy/env}"
```

의미는 아래와 같습니다.

- `SERVICE`: 서비스별 env 파일을 만들지 결정합니다.
- `ENV_DIR`: env 파일을 저장하는 디렉터리입니다.

### 3.4 디렉터리 생성과 권한 설정

스크립트는 먼저 env 디렉터리를 준비합니다.

```bash
mkdir -p "$ENV_DIR"
chmod 700 "$ENV_DIR"
```

이 설정은 아래 목적을 가집니다.

- 디렉터리가 없으면 자동으로 생성합니다.
- env 파일이 저장되는 경로의 기본 접근 범위를 제한합니다.

### 3.5 적용 여부 판단 방식

현재 `apply-env.sh`의 핵심은 `write_env_file()` 함수입니다.

이 함수는 아래 순서로 동작합니다.

1. 전달받은 내용이 비어 있으면 파일을 쓰지 않고 건너뜁니다.
2. 내용이 있으면 대상 env 파일에 기록합니다.
3. 파일을 쓴 경우 `CONFIG_CHANGED`와 세부 적용 플래그를 `true`로 설정합니다.

즉, 체크박스를 켜지 않았거나 Secret 값이 비어 있으면 아래 로그가 출력됩니다.

```text
Empty content, skipping: ...
```

내용을 파일에 기록한 경우에는 아래처럼 출력됩니다.

```text
Applied: ...
```

### 3.6 현재 생성하는 파일

현재 스크립트는 아래 흐름으로 파일을 씁니다.

```bash
write_env_file "$ENV_DIR/common_config.env" "${COMMON_CONFIG_ENV:-}" "COMMON_CONFIG_CHANGED"
write_env_file "$ENV_DIR/common_secret.env" "${COMMON_SECRET_ENV:-}" "COMMON_SECRET_CHANGED"
```

그리고 서비스 인자가 있을 때만 아래 파일을 추가로 처리합니다.

```bash
write_env_file "$ENV_DIR/${SERVICE}_config.env" "${SERVICE_CONFIG_ENV:-}" "SERVICE_CONFIG_CHANGED"
write_env_file "$ENV_DIR/${SERVICE}_secret.env" "${SERVICE_SECRET_ENV:-}" "SERVICE_SECRET_CHANGED"
```

즉, `common`만 반영할 때는 `noop` 같은 임시 파일을 만들지 않습니다. 이 부분은 예전 구조와 달라진 점입니다.

### 3.7 변경 결과로 내보내는 값

현재 스크립트는 아래 값을 출력하고, GitHub Actions 환경에도 넘깁니다.

- `CONFIG_CHANGED`
- `COMMON_CONFIG_CHANGED`
- `COMMON_SECRET_CHANGED`
- `SERVICE_CONFIG_CHANGED`
- `SERVICE_SECRET_CHANGED`

예를 들어 아래처럼 출력됩니다.

```bash
echo "CONFIG_CHANGED=$CONFIG_CHANGED"
echo "COMMON_CONFIG_CHANGED=$COMMON_CONFIG_CHANGED"
echo "COMMON_SECRET_CHANGED=$COMMON_SECRET_CHANGED"
echo "SERVICE_CONFIG_CHANGED=$SERVICE_CONFIG_CHANGED"
echo "SERVICE_SECRET_CHANGED=$SERVICE_SECRET_CHANGED"
```

이 값은 이후 워크플로에서 ConfigMap / Secret 적용 여부나 서비스 재시작 여부를 판단할 때 사용합니다.

### 3.8 현재 권한 처리

스크립트는 생성된 env 파일이 있으면 아래처럼 `600` 권한을 부여합니다.

- `common_secret.env`
- `common_config.env`
- `<service>_secret.env`
- `<service>_config.env`

즉, 현재 구조에서는 config 파일도 secret 파일과 같은 수준으로 제한해서 관리합니다.

## 4. deploy-app.sh

### 4.1 역할

`deploy-app.sh`는 현재 서비스 manifest를 Kubernetes에 적용하는 전용 스크립트입니다.

예전 문서와 달리, 지금은 아래 작업을 수행하지 않습니다.

- ConfigMap 생성
- Secret 생성
- env 파일 정리
- 설정 변경 여부에 따른 restart 판단

현재 수행하는 작업은 아래와 같습니다.

- 서비스 이름 확인
- kubeconfig 경로 확인
- 대상 manifest 파일 확인
- 이미지 변수 치환
- `kubectl apply`
- `kubectl rollout status`
- 실패 시 진단 로그 출력

### 4.2 실행 방식

기본 사용 예시는 아래와 같습니다.

```bash
/home/ubuntu/apps/deploy/scripts/deploy-app.sh admin
```

또는 환경변수로 서비스를 넘길 수도 있습니다.

```bash
SERVICE=admin /home/ubuntu/apps/deploy/scripts/deploy-app.sh
```

### 4.3 필수 환경변수

현재 스크립트는 아래 값을 필수로 요구합니다.

```bash
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME is required}"
IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG is required}"
```

즉, 최소한 아래 값이 있어야 합니다.

- `DOCKERHUB_USERNAME`
- `IMAGE_TAG`

없으면 스크립트는 즉시 종료합니다.

### 4.4 주요 변수

현재 스크립트가 사용하는 주요 기본값은 아래와 같습니다.

```bash
K3S_DIR="${K3S_DIR:-/home/ubuntu/apps/data/k3s-service}"
KUBECONFIG_PATH="${KUBECONFIG_PATH:-/home/ubuntu/.kube/config}"
ROLLOUT_TIMEOUT="${ROLLOUT_TIMEOUT:-300s}"
```

의미는 아래와 같습니다.

- `K3S_DIR`: 서비스 manifest가 놓이는 경로입니다.
- `KUBECONFIG_PATH`: K3s 클러스터에 접근할 kubeconfig 파일 경로입니다.
- `ROLLOUT_TIMEOUT`: rollout 완료를 기다리는 최대 시간입니다.

### 4.5 kubeconfig 확인

스크립트는 시작 시 kubeconfig 파일 존재 여부를 먼저 확인합니다.

```bash
if [ ! -f "$KUBECONFIG_PATH" ]; then
  echo "Kubeconfig not found: $KUBECONFIG_PATH"
  exit 1
fi
```

즉, self-hosted runner가 K3s에 접근할 수 없는 상태라면 배포는 바로 실패합니다.

### 4.6 대상 manifest 규칙

현재 manifest 파일 경로는 아래 규칙으로 찾습니다.

```bash
YAML_FILE="$K3S_DIR/${SERVICE}-service.yml"
```

즉, 서비스명이 `user`이면 아래 파일을 찾습니다.

```text
/home/ubuntu/apps/data/k3s-service/user-service.yml
```

Deployment 이름도 아래 규칙을 사용합니다.

```bash
DEPLOYMENT_NAME="${SERVICE}-service"
APP_LABEL="${SERVICE}-service"
```

즉, 현재는 아래 이름 규칙이 서로 맞아야 합니다.

- 스크립트 인자: `user`
- manifest 파일명: `user-service.yml`
- deployment 이름: `user-service`
- app label: `user-service`

### 4.7 실제 배포 방식

manifest 적용은 아래 방식으로 진행합니다.

```bash
export DOCKERHUB_USERNAME IMAGE_TAG
envsubst '$DOCKERHUB_USERNAME $IMAGE_TAG' < "$YAML_FILE" | \
  kubectl --kubeconfig "$KUBECONFIG_PATH" apply -f -
```

즉, manifest 안의 아래 변수만 치환합니다.

- `${DOCKERHUB_USERNAME}`
- `${IMAGE_TAG}`

이후 바로 `kubectl apply`를 수행합니다.

### 4.8 rollout 확인

manifest를 적용한 뒤 아래 명령으로 rollout 완료를 확인합니다.

```bash
kubectl --kubeconfig "$KUBECONFIG_PATH" rollout status deployment/"$DEPLOYMENT_NAME" --timeout="$ROLLOUT_TIMEOUT"
```

즉, `kubectl apply`가 성공했다고 바로 배포 성공으로 보지 않고, 실제 배포 완료까지 기다립니다.

### 4.9 실패 시 진단 로그

rollout이 실패하면 `print_rollout_diagnostics()`를 실행합니다.

현재 이 함수는 아래 정보를 출력합니다.

- deployment 조회 결과
- deployment describe
- replicaset 목록
- pod 목록
- 각 pod describe
- 각 pod 현재 로그
- 각 pod 이전 로그

즉, 배포 실패 시 원인을 바로 확인할 수 있도록 최소 진단 정보를 모아 주는 구조입니다.

## 5. 현재 두 스크립트의 관계

현재 두 스크립트는 서로 연결되어 있지만, 예전보다 훨씬 분리된 구조입니다.

### 배포 흐름에서의 관계

```text
Deploy Environment
-> apply-env.sh
-> env 파일 변경 여부 판단
-> ConfigMap / Secret 적용
-> 필요 시 deployment restart

서비스 CD
-> deploy-app.sh
-> manifest 배포
-> rollout 확인
```

즉, 지금은 `apply-env.sh`와 `deploy-app.sh`가 한 워크플로 안에서 연속으로 반드시 실행되는 구조가 아닙니다.

이 점이 예전 구조와 가장 크게 달라진 부분입니다.

## 6. 지금 문서 기준으로 기억하시면 좋은 점

### 1. `apply-env.sh`는 env 파일 전용입니다

이 스크립트는 파일을 쓰고 변경 여부를 알려주는 역할만 합니다.

### 2. `deploy-app.sh`는 manifest 배포 전용입니다

이 스크립트는 Kubernetes apply와 rollout 확인만 담당합니다.

### 3. ConfigMap / Secret 적용은 워크플로에서 처리합니다

지금은 `deploy-app.sh` 내부가 아니라 `deploy-env.yml` 쪽에서 별도로 처리합니다.

### 4. restart 판단도 워크플로에서 처리합니다

지금은 `CONFIG_CHANGED` 값을 기준으로 워크플로가 restart 여부를 판단합니다.

### 5. 서비스 이름 규칙은 여전히 중요합니다

아래 규칙이 어긋나면 배포가 실패할 수 있습니다.

- 서비스명
- manifest 파일명
- deployment 이름
- app label

## 7. 문제 발생 시 먼저 볼 항목

스크립트 관련 문제가 생기면 아래 순서로 확인하시는 편이 좋습니다.

1. GitHub Actions step 로그
2. `/home/ubuntu/apps/deploy/env` 아래 파일 상태
3. `/home/ubuntu/apps/data/k3s-service/<service>-service.yml` 존재 여부
4. `kubectl get deployment`
5. `kubectl describe deployment <name>`
6. `kubectl get pods -l app=<service>-service`
7. `kubectl logs <pod-name> --tail=200`

자주 발생하는 원인은 아래와 같습니다.

- GitHub Secret 값이 비어 있습니다.
- kubeconfig 경로가 잘못되었습니다.
- manifest 파일명이 규칙과 다릅니다.
- deployment 이름이나 label이 규칙과 다릅니다.
- rollout timeout이 발생했습니다.

## 8. 요약

현재 저장소 기준으로 K3s 배포 스크립트는 아래처럼 정리되어 있습니다.

- `apply-env.sh`
  - env 파일 생성 및 갱신
  - 변경 여부 판단
  - GitHub Actions 변수 전달
- `deploy-app.sh`
  - manifest 적용
  - rollout 확인
  - 실패 시 진단 로그 출력

즉, 지금 구조는 예전처럼 한 스크립트가 여러 역할을 다 담당하는 방식이 아니라, env 처리와 서비스 배포를 분리한 구조라고 이해하시면 됩니다.
