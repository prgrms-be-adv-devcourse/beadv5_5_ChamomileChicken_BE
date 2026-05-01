# Gateway Rules Config 배포 Workflow

## 1. 문서 목적

이 문서는 API Gateway 라우팅/인증 예외/RBAC 규칙을 담고 있는 `gateway-rules-config.yml`을 K3s 클러스터에 적용하는 GitHub Actions workflow를 설명합니다.

해당 파일은 애플리케이션 이미지를 새로 빌드하는 대상이 아니라 Kubernetes `ConfigMap`입니다. 따라서 배포 workflow도 Docker 이미지 빌드나 push 없이 ConfigMap 적용과 API Gateway 재시작만 수행합니다.

## 2. 관련 파일

### GitHub Actions workflow

- `.github/workflows/deploy-gateway-rules-config.yml`

### Git에서 관리하는 원본 manifest

- `.github/k3s/gateway-rules-config.yml`

### self-hosted runner에 복사되는 적용 파일 경로

- `/home/ubuntu/apps/data/k3s-service/gateway-rules-config.yml`

workflow는 Git 저장소의 `.github/k3s/gateway-rules-config.yml`을 checkout한 뒤, self-hosted runner의 고정 배포 경로인 `/home/ubuntu/apps/data/k3s-service/gateway-rules-config.yml`로 복사합니다.

이후 `kubectl apply -f`는 Git checkout 경로가 아니라 runner의 고정 배포 경로에 있는 파일을 기준으로 실행됩니다.

```bash
kubectl apply -f /home/ubuntu/apps/data/k3s-service/gateway-rules-config.yml
```

## 3. 실행 조건

이 workflow는 다음 경우에 실행됩니다.

### dev 브랜치 push

아래 파일 중 하나가 변경되어 `dev` 브랜치에 push되면 자동 실행됩니다.

- `.github/k3s/gateway-rules-config.yml`
- `.github/workflows/deploy-gateway-rules-config.yml`

### 수동 실행

GitHub Actions 화면에서 `Deploy Gateway Rules Config` workflow를 직접 실행할 수 있습니다.

## 4. 배포 흐름

workflow의 주요 흐름은 다음과 같습니다.

1. self-hosted runner에서 repository checkout
2. `.github/k3s/gateway-rules-config.yml`만 sparse checkout
3. `/home/ubuntu/apps/data/k3s-service` 디렉터리가 없으면 생성
4. 원본 manifest를 runner의 고정 배포 경로로 복사
5. 복사된 파일 존재 여부 확인
6. `kubectl apply -f`로 `gateway-rules-config` ConfigMap 적용
7. `api-gateway-service` deployment 재시작
8. rollout 완료 대기
9. 적용된 ConfigMap 조회

## 5. API Gateway 재시작 이유

`gateway-rules-config.yml`은 `api-gateway-service`의 volume으로 mount되는 ConfigMap입니다.

Kubernetes ConfigMap volume은 파일 내용이 갱신될 수 있지만, 애플리케이션이 해당 설정을 런타임에 자동으로 다시 읽는지는 애플리케이션 구현에 따라 달라집니다. 이 프로젝트에서는 변경된 gateway rules를 확실히 반영하기 위해 ConfigMap 적용 후 API Gateway deployment를 재시작합니다.

재시작 명령은 다음과 같습니다.

```bash
kubectl rollout restart deployment/api-gateway-service
kubectl rollout status deployment/api-gateway-service --timeout=300s
```

## 6. runner 전제 조건

workflow는 다음 runner 환경을 전제로 합니다.

- self-hosted runner label: `self-hosted`, `ec2`, `docker`
- kubeconfig 경로: `/home/ubuntu/.kube/config`
- `kubectl` 사용 가능
- Kubernetes manifest 고정 경로: `/home/ubuntu/apps/data/k3s-service`

`/home/ubuntu/apps/data/k3s-service` 디렉터리는 일반적으로 기존 서비스 배포 workflow에서도 사용하는 경로입니다. 단, runner 초기화나 경로 삭제 상황을 고려해 이 workflow는 해당 디렉터리가 없으면 생성합니다.

## 7. 적용 대상 Kubernetes 리소스

`gateway-rules-config.yml`은 다음 ConfigMap을 생성하거나 갱신합니다.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: gateway-rules-config
```

`api-gateway-service.yml`에서는 이 ConfigMap을 `gateway-rules` volume으로 mount합니다.

```yaml
volumes:
  - name: gateway-rules
    configMap:
      name: gateway-rules-config
```

컨테이너 내부 mount 경로는 다음과 같습니다.

```yaml
volumeMounts:
  - mountPath: /etc/config
    name: gateway-rules
    readOnly: true
```

따라서 `gateway-rules-config.yml` 변경은 최종적으로 API Gateway 컨테이너의 `/etc/config/gateway-rules.yml` 설정에 반영됩니다.

## 8. 서비스 배포 workflow와의 차이

일반 서비스 CD workflow는 다음 작업을 포함합니다.

- Gradle build
- Docker image build
- Docker Hub push
- service manifest 적용
- deployment rollout 확인

반면 `deploy-gateway-rules-config.yml`은 ConfigMap 전용 workflow이므로 다음 작업만 수행합니다.

- ConfigMap manifest 복사
- `kubectl apply -f`
- `api-gateway-service` 재시작
- ConfigMap 적용 결과 확인

즉, gateway rules만 변경하는 경우 API Gateway 이미지를 새로 빌드할 필요가 없습니다.
