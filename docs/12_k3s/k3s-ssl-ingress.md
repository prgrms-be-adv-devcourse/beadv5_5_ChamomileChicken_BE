# K3s SSL / Ingress 가이드

## 1. 문서 목적

이 문서는 현재 K3s 환경에서 도메인, HTTPS, Ingress, cert-manager 인증서 발급이 어떻게 연결되는지 정리한 문서입니다.

대상 파일은 아래와 같습니다.

- `.github/k3s/cluster-issuer.yml`
- `.github/k3s/main-ingress.yml`

서비스별 Kubernetes manifest나 CD 스크립트와는 역할이 다르므로 별도 문서로 분리합니다.

<<<<<<< feature/k3s/k3s-infra/244
## 2. 적용 목적

SSL / Ingress 설정의 목적은 아래와 같습니다.

- 사용자와 서버 간 통신을 HTTPS로 암호화합니다.
- 브라우저 보안 경고 없이 신뢰 가능한 인증서를 사용합니다.
- Let's Encrypt와 cert-manager를 통해 인증서 발급과 갱신을 자동화합니다.
- `jabaclass.store` 도메인 기준으로 외부 접근 경로를 제공합니다.

## 3. 전체 구조
=======
## 2. 전체 구조
>>>>>>> dev

현재 외부 HTTPS 진입 구조는 아래 흐름입니다.

```text
사용자 브라우저
-> https://jabaclass.store
-> Traefik Ingress
-> main-ingress
-> api-gateway-service 또는 frontend-service
```

인증서 발급은 아래 흐름입니다.

```text
main-ingress.yml
-> cert-manager annotation
-> ClusterIssuer letsencrypt-prod
-> Let's Encrypt HTTP-01 challenge
-> TLS Secret main-tls 생성
```

<<<<<<< feature/k3s/k3s-infra/244
사용 기술:

- K3s
- Traefik Ingress Controller
- cert-manager
- Let's Encrypt

## 4. 사전 준비

### 도메인 DNS

`jabaclass.store`가 K3s 외부 진입점으로 향해야 합니다.

Gabia 기준 루트 도메인은 보통 아래처럼 A 레코드를 둡니다.

| 타입 | 호스트 | 값 |
| --- | --- | --- |
| A | `@` | EC2 퍼블릭 IP 또는 외부 진입 IP |

`@`는 루트 도메인인 `jabaclass.store`를 의미합니다.

### EC2 보안 그룹

Let's Encrypt HTTP-01 challenge와 HTTPS 접속을 위해 아래 포트가 외부에서 열려 있어야 합니다.

| 용도 | 포트 | 소스 |
| --- | --- | --- |
| HTTP | `80` | `0.0.0.0/0` |
| HTTPS | `443` | `0.0.0.0/0` |

HTTP-01 challenge는 HTTP 80 포트로 도메인 소유권을 검증하므로, 인증서 최초 발급과 갱신 시 80 포트 접근이 막히면 실패할 수 있습니다.

### cert-manager 설치

cert-manager가 클러스터에 설치되어 있어야 `ClusterIssuer`, `Certificate`, `Order`, `Challenge` 리소스가 동작합니다.

설치 예시:

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/latest/download/cert-manager.yaml
```

설치 확인:

```bash
kubectl get pods -n cert-manager
kubectl get crd | grep cert-manager
```

## 5. cluster-issuer.yml
=======
## 3. cluster-issuer.yml
>>>>>>> dev

파일: `.github/k3s/cluster-issuer.yml`

이 파일은 cert-manager가 Let's Encrypt에서 인증서를 발급받을 때 사용할 발급자 설정입니다.

핵심 설정은 아래와 같습니다.

- Kind: `ClusterIssuer`
- 이름: `letsencrypt-prod`
- ACME 서버: `https://acme-v02.api.letsencrypt.org/directory`
- challenge 방식: `http01`
- Ingress class: `traefik`
- private key secret: `letsencrypt-prod`

즉, 클러스터 전체에서 사용할 수 있는 Let's Encrypt 발급자를 하나 정의한 상태입니다.

주의할 점:

- `spec.acme.email`은 실제 운영자가 받을 수 있는 이메일이어야 합니다.
- cert-manager가 클러스터에 설치되어 있어야 합니다.
- HTTP-01 challenge를 위해 외부에서 `jabaclass.store`의 HTTP 경로로 접근 가능해야 합니다.

<<<<<<< feature/k3s/k3s-infra/244
적용:

```bash
kubectl apply -f .github/k3s/cluster-issuer.yml
```

확인:

```bash
kubectl get clusterissuer
kubectl describe clusterissuer letsencrypt-prod
```

## 6. main-ingress.yml
=======
## 4. main-ingress.yml
>>>>>>> dev

파일: `.github/k3s/main-ingress.yml`

이 파일은 외부 도메인 요청을 어떤 내부 서비스로 보낼지 정의합니다.

핵심 설정은 아래와 같습니다.

- Ingress 이름: `main-ingress`
- Ingress class: `traefik`
- 도메인: `jabaclass.store`
- TLS secret: `main-tls`
- ClusterIssuer annotation: `letsencrypt-prod`

관련 annotation:

```yaml
cert-manager.io/cluster-issuer: letsencrypt-prod
traefik.ingress.kubernetes.io/redirect-entry-point: https
```

`cert-manager.io/cluster-issuer`는 이 Ingress의 TLS 인증서를 어떤 ClusterIssuer로 발급받을지 지정합니다.

<<<<<<< feature/k3s/k3s-infra/244
적용:

```bash
kubectl apply -f .github/k3s/main-ingress.yml
```

확인:

```bash
kubectl get ingress main-ingress
kubectl describe ingress main-ingress
```

## 7. TLS Secret
=======
## 5. TLS Secret
>>>>>>> dev

`main-ingress.yml`에는 아래 설정이 있습니다.

```yaml
tls:
  - hosts:
      - jabaclass.store
    secretName: main-tls
```

이 설정 때문에 cert-manager는 `jabaclass.store` 인증서를 발급하고, 결과 인증서를 `main-tls` Secret으로 저장합니다.

Ingress는 이후 HTTPS 요청 처리 시 이 Secret을 사용합니다.

<<<<<<< feature/k3s/k3s-infra/244
확인:

```bash
kubectl get certificate
kubectl describe certificate main-tls
kubectl get secret main-tls
```

정상 상태에서는 certificate의 `READY` 값이 `True`가 됩니다.

## 8. 현재 라우팅
=======
## 6. 현재 라우팅
>>>>>>> dev

현재 `main-ingress.yml` 기준 라우팅은 아래와 같습니다.

API Gateway로 연결되는 경로:

- `/api`
- `/swagger-ui`
- `/v3/api-docs`
- `/docs`
- `/oauth2`
- `/login/oauth2`

연결 대상:

- `api-gateway-service:8080`

Frontend로 연결되는 경로:

- `/oauth2/callback`
- `/`

연결 대상:

- `frontend-service:3000`

즉, API, Swagger, OAuth 인증 시작/처리 요청은 게이트웨이로 보내고, OAuth 완료 후 프론트엔드가 처리해야 하는 callback 경로와 기본 화면 요청은 프론트엔드로 보냅니다.

`/oauth2/callback`은 `/oauth2`보다 더 구체적인 경로이므로 `main-ingress.yml`에서 `/oauth2`보다 먼저 배치합니다.

<<<<<<< feature/k3s/k3s-infra/244
## 9. SSL 발급 동작 원리

cert-manager 기반 SSL 발급 흐름은 아래와 같습니다.

```text
Ingress 생성
-> cert-manager가 annotation 감지
-> ClusterIssuer letsencrypt-prod 사용
-> Let's Encrypt 인증 요청
-> http://jabaclass.store/.well-known/... 경로로 HTTP-01 검증
-> 인증 성공
-> TLS Secret main-tls 생성
-> Ingress가 main-tls로 HTTPS 처리
```

## 10. 적용 순서
=======
## 7. 적용 순서
>>>>>>> dev

처음 SSL을 구성할 때는 보통 아래 순서가 안전합니다.

1. cert-manager 설치 여부 확인
<<<<<<< feature/k3s/k3s-infra/244
2. DNS A 레코드가 K3s 노드 또는 외부 진입점으로 향하는지 확인
3. EC2 보안 그룹에서 80, 443 포트가 열려 있는지 확인
=======
2. DNS가 K3s 노드 또는 로드밸런서로 향하는지 확인
>>>>>>> dev
3. `cluster-issuer.yml` 적용
4. `main-ingress.yml` 적용
5. Certificate, Challenge, Secret 상태 확인

예시 명령:

```bash
kubectl apply -f .github/k3s/cluster-issuer.yml
kubectl apply -f .github/k3s/main-ingress.yml
```

확인 명령:

```bash
kubectl get clusterissuer
kubectl get ingress main-ingress
kubectl get certificate
kubectl get secret main-tls
kubectl describe ingress main-ingress
```

cert-manager 리소스까지 확인해야 할 때:

```bash
kubectl get certificate,certificaterequest,order,challenge
```

<<<<<<< feature/k3s/k3s-infra/244
## 11. HTTP -> HTTPS 리다이렉트

현재 `main-ingress.yml`에는 아래 annotation이 들어 있습니다.

```yaml
traefik.ingress.kubernetes.io/redirect-entry-point: https
```

환경에 따라 Traefik의 entrypoint 설정만으로 리다이렉트가 동작할 수도 있고, 별도 Middleware가 필요할 수도 있습니다.

Middleware를 사용하는 경우 예시는 아래와 같습니다.

```yaml
apiVersion: traefik.io/v1alpha1
kind: Middleware
metadata:
  name: redirect-https
spec:
  redirectScheme:
    scheme: https
    permanent: true
```

Ingress에 Middleware를 명시하는 방식:

```yaml
annotations:
  traefik.ingress.kubernetes.io/router.entrypoints: web,websecure
  traefik.ingress.kubernetes.io/router.middlewares: default-redirect-https@kubernetescrd
```

현재 운영 환경에서 HTTP -> HTTPS 리다이렉트가 이미 정상 동작한다면 별도 Middleware를 추가할 필요는 없습니다.

## 12. 자주 보는 문제
=======
## 8. 자주 보는 문제
>>>>>>> dev

### 인증서가 발급되지 않는 경우

먼저 아래 항목을 확인합니다.

- `jabaclass.store` DNS가 실제 K3s 진입점으로 연결되어 있는지
<<<<<<< feature/k3s/k3s-infra/244
- EC2 보안 그룹 또는 방화벽에서 80 포트가 열려 있는지
=======
>>>>>>> dev
- Traefik Ingress가 외부 HTTP 요청을 받을 수 있는지
- `cluster-issuer.yml`의 email 값이 유효한지
- cert-manager Pod가 정상인지
- `kubectl describe challenge`에 실패 사유가 있는지

### HTTPS 접속은 되지만 라우팅이 이상한 경우

먼저 아래 항목을 확인합니다.

- `main-ingress.yml`의 path 순서
- `/oauth2/callback`이 `/oauth2`보다 먼저 선언되어 있는지
- `/` 경로가 마지막에 있는지
- 각 backend service 이름과 port가 실제 Service와 맞는지
- `api-gateway-service`, `frontend-service` Pod 상태

### Secret이 없는 경우

`main-tls` Secret은 직접 만드는 값이 아니라 cert-manager가 인증서 발급 후 생성하는 값입니다.

없다면 아래 순서로 확인합니다.

```bash
kubectl describe clusterissuer letsencrypt-prod
kubectl describe ingress main-ingress
kubectl get certificate,order,challenge
```

<<<<<<< feature/k3s/k3s-infra/244
### HTTP-01 challenge가 실패하는 경우

아래 항목을 우선 확인합니다.

- `http://jabaclass.store`로 외부 접근이 가능한지
- DNS가 올바른 IP를 가리키는지
- 80 포트가 보안 그룹에서 열려 있는지
- Traefik이 HTTP entrypoint를 받고 있는지
- cert-manager challenge 리소스의 describe 로그

확인 명령:

```bash
kubectl get challenge
kubectl describe challenge <challenge-name>
```

## 13. 관련 문서
=======
## 9. 관련 문서
>>>>>>> dev

- `k3s-yml.md`
- `k3s-service-deployment-structure.md`
- `k3s-cicd-final-guide.md`

<<<<<<< feature/k3s/k3s-infra/244
## 14. 요약
=======
## 10. 요약
>>>>>>> dev

현재 SSL 연결은 `cluster-issuer.yml`과 `main-ingress.yml`이 함께 동작하는 구조입니다.

- `cluster-issuer.yml`: Let's Encrypt 인증서를 발급받는 방법 정의
- `main-ingress.yml`: `jabaclass.store` 도메인, TLS Secret, 외부 라우팅 정의
- `main-tls`: cert-manager가 발급 결과로 생성하는 TLS Secret
<<<<<<< feature/k3s/k3s-infra/244
- DNS A 레코드와 80/443 보안 그룹 설정이 선행되어야 합니다.
=======
>>>>>>> dev

따라서 HTTPS 문제가 생기면 Service manifest보다 Ingress, ClusterIssuer, Certificate, Challenge 상태를 먼저 확인하는 편이 좋습니다.
