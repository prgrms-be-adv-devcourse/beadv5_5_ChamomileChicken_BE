#!/bin/bash
set -e

REPO="prgrms-be-adv-devcourse/beadv5_5_ChamomileChicken_BE"
BASE="dev"
HEAD="refactor/redis/resilience/360"
TITLE="fix: Redis 장애 시 Gateway 인증 필터 503 완전 제거"

read -rp "GitHub Personal Access Token: " TOKEN

BODY=$(cat <<'EOF'
## 문제

Redis가 완전히 다운된 상태에서 파일 업로드 등 인증이 필요한 API 호출 시 503 에러가 간헐적으로 발생.

## 원인

`JwtAuthenticationFilter`의 Circuit Breaker fallback lambda에서 Redis 실제 오류와 CB OPEN 상태를 다르게 처리하고 있었음.

| 상황 | 기존 동작 | 결과 |
|------|-----------|------|
| CB OPEN (CallNotPermittedException) | `RedisCircuitOpenException` → user service fallback | ✅ 200 |
| Redis 실제 오류 (첫 요청, HALF-OPEN probe) | `RedisBlacklistException` → AUTH_SERVICE_UNAVAILABLE | ❌ 503 |

**CB 상태 전이 (수정 전):**
```
첫 요청 → Redis 실패 → 503  (이 시점에 CB가 OPEN 전환)
CB OPEN 동안 → short-circuit → user service fallback → 200
20초 후 HALF-OPEN → probe 요청 → Redis 실패 → 503  (20초마다 반복)
```

즉, CB가 OPEN인 구간에서만 200이 나오고 나머지 시점에는 503이 발생하는 패턴.

## 수정

CB fallback lambda를 통일하여 Redis 오류 유형에 무관하게 모두 `RedisCircuitOpenException`으로 처리.
이후 `onErrorResume(RedisCircuitOpenException)` 핸들러에서 user service(`tokenStatusClient`)로 fallback.

```java
// Before
throwable -> {
    if (throwable instanceof CallNotPermittedException) {
        return Mono.error(new RedisCircuitOpenException(throwable));
    }
    return Mono.error(new RedisBlacklistException(throwable)); // → 503
}

// After
throwable -> Mono.error(new RedisCircuitOpenException(throwable))
```

## 변경 후 동작

Redis 완전 다운 시:

| 시점 | 동작 |
|------|------|
| 첫 요청 (CB CLOSED, Redis 실패) | user service fallback → **200** |
| CB OPEN 동안 | short-circuit → user service fallback → **200** |
| HALF-OPEN probe 실패 | user service fallback → **200** |

503은 user service까지 다운된 경우에만 발생 (fail-closed 유지).

## 변경 파일

- `JwtAuthenticationFilter.java`: fallback lambda 통일, `RedisBlacklistException` 분기 및 import 제거

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)

PAYLOAD=$(jq -n \
  --arg title "$TITLE" \
  --arg body "$BODY" \
  --arg head "$HEAD" \
  --arg base "$BASE" \
  '{title: $title, body: $body, head: $head, base: $base}')

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/$REPO/pulls" \
  -d "$PAYLOAD")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY_RESPONSE=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "201" ]; then
  PR_URL=$(echo "$BODY_RESPONSE" | jq -r '.html_url')
  echo ""
  echo "✅ PR 생성 완료: $PR_URL"
else
  echo ""
  echo "❌ PR 생성 실패 (HTTP $HTTP_CODE)"
  echo "$BODY_RESPONSE" | jq '.message // .'
fi
