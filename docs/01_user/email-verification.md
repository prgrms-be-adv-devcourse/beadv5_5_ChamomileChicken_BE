# 이메일 인증 (Email Verification) — User 서비스

## 목차

1. [개요](#1-개요)
2. [이메일 인증 흐름](#2-이메일-인증-흐름)
3. [도메인 모델](#3-도메인-모델)
4. [verifiedToken 소비](#4-verifiedtoken-소비)
5. [메일 발송 구현](#5-메일-발송-구현)
6. [엔드포인트 목록](#6-엔드포인트-목록)

---

## 1. 개요

이메일 인증은 **회원가입**과 **이메일 변경** 시 필수로 거쳐야 하는 2단계 인증이다.

1. 인증코드 발송 → 2. 코드 검증 → `verifiedToken` 발급 → 3. 회원가입/이메일 변경 시 `verifiedToken` 제출

인증 상태는 `InMemoryEmailVerificationRepository`에 저장된다 (Redis 미사용, 서버 메모리).
서버 재시작 시 인증 상태가 초기화되며, 단일 인스턴스 환경을 전제로 한다.

---

## 2. 이메일 인증 흐름

```
[1단계] 인증코드 발송
POST /api/v1/email/verifications
  { email }

EmailVerificationService.sendVerificationCode()
  1. 6자리 난수 코드 생성 (SecureRandom)
  2. expiresAt = now + 5분
  3. EmailVerification(email, code, verifiedToken=null, expiresAt) 메모리 저장
  4. 이메일 발송: "[ChamomileChicken] 이메일 인증코드를 확인해주세요"
  ← 204 No Content


[2단계] 코드 검증 → verifiedToken 발급
POST /api/v1/email/verifications/confirm
  { email, code }

EmailVerificationService.verifyCode()
  1. 메모리에서 EmailVerification 조회
     → 없으면: EMAIL_VERIFICATION_NOT_FOUND (404)
  2. 만료 확인
     → 만료: 메모리에서 삭제 + EMAIL_VERIFICATION_CODE_EXPIRED (400)
  3. 코드 일치 확인
     → 불일치: EMAIL_VERIFICATION_CODE_MISMATCH (400)
  4. verifiedToken = UUID.randomUUID()
     emailVerification.verify(verifiedToken)  ← verifiedToken 저장
  ← { verifiedToken: "UUID" }
```

---

## 3. 도메인 모델

### EmailVerification

| 필드 | 타입 | 설명 |
|------|------|------|
| `email` | String | 인증 대상 이메일 |
| `code` | String | 6자리 숫자 코드 |
| `verifiedToken` | String (UUID) | 코드 인증 성공 후 발급되는 일회용 토큰 |
| `expiresAt` | LocalDateTime | 만료 시각 (발급 후 5분) |

**상태 전이:**

```
발송 시: { code: "123456", verifiedToken: null, expiresAt: +5분 }
     ↓ verifyCode() 성공
검증 후: { code: "123456", verifiedToken: "uuid-...", expiresAt: +5분 }
     ↓ validateVerifiedToken() 소비
삭제됨  (메모리에서 제거)
```

### InMemoryEmailVerificationRepository

`ConcurrentHashMap<email, EmailVerification>`을 사용하는 인메모리 저장소.
DB나 Redis를 사용하지 않는다.

> **제약:** 다중 인스턴스 환경에서는 인증 상태가 공유되지 않는다. 현재 단일 인스턴스 운영 환경 기준이다.

---

## 4. verifiedToken 소비

`verifiedToken`은 **1회용**이다. 사용(소비) 후 메모리에서 삭제된다.

```java
// EmailVerificationService.validateVerifiedToken()

EmailVerification ev = repo.findByEmail(email)
    .orElseThrow(() -> EMAIL_VERIFICATION_TOKEN_NOT_FOUND);  // 없으면 404

if (ev.isExpired(now))       → 삭제 + EMAIL_VERIFICATION_TOKEN_EXPIRED (400)
if (!ev.hasVerifiedToken())  → EMAIL_VERIFICATION_TOKEN_MISMATCH (400)
if (!ev.matchesVerifiedToken(verifiedToken))
                             → EMAIL_VERIFICATION_TOKEN_MISMATCH (400)

repo.deleteByEmail(email);   // 소비 완료 — 재사용 불가
```

**소비 시점:**

| 사용처 | 호출 메서드 |
|--------|------------|
| 회원가입 | `UserService.register()` |
| 이메일 변경 | `UserService.changeEmail()` |

두 경우 모두 `emailVerificationUseCase.validateVerifiedToken(email, verifiedToken)`을 호출해 토큰 유효성을 검사하고 소비한다.

---

## 5. 메일 발송 구현

`MailSender` 인터페이스를 구현한 두 개의 빈이 존재하며, 프로파일에 따라 주입된다.

| 구현체 | 프로파일 | 동작 |
|--------|---------|------|
| `SmtpMailSender` | prod | JavaMailSender를 통해 실제 SMTP 발송 |
| `ConsoleMailSender` | dev / test | 로그에 메일 내용 출력 (실제 발송 없음) |

**이메일 템플릿:**
- 인증코드 메일: HTML 디자인, 코드는 6자리 숫자, 유효시간 5분 명시
- 보안 알림 메일 (`AuthService`에서 발송): "본인 아님" 클릭 시 `report-theft` API 호출 링크 포함

**환경 변수:**

```
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=user@example.com
MAIL_PASSWORD=secret
```

---

## 6. 엔드포인트 목록

| 메서드 | 경로 | 인증 | 설명 |
|--------|------|------|------|
| `POST` | `/api/v1/email/verifications` | 없음 | 인증코드 이메일 발송 |
| `POST` | `/api/v1/email/verifications/confirm` | 없음 | 코드 검증 → verifiedToken 반환 |

전체 request/response 스키마는 [01. User Service API 명세](../00_api-spec/01-user.md)를 참고한다.
