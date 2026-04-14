# 03. File Service (9000)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

---

## 파일 업로드 (File)

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| POST | `/api/v1/files/upload-request` | ✅ JWT | 업로드 URL 발급 (Presigned URL) |
| PATCH | `/api/v1/files/{fileId}/complete` | ✅ JWT | 업로드 완료 처리 |

---

### POST `/api/v1/files/upload-request`

**Request Body**
```json
{
  "originalName": "photo.jpg"
}
```

**Response** `200 OK`
```json
{
  "data": {
    "fileId": "uuid",
    "uploadUrl": "https://s3.amazonaws.com/presigned-url...",
    "storagePath": "uploads/uuid/photo.jpg"
  }
}
```

> 발급된 `uploadUrl`로 직접 PUT 요청으로 파일 업로드 후, `/complete` 호출

---

## 내부 API (Internal)

> ⚠️ `/api/internal/files/**`는 게이트웨이 미등록 경로 — 서비스 간 직접 호출

| Method | Path | Auth | 설명 |
|--------|------|------|------|
| GET | `/api/internal/files/{fileId}/confirm` | ⚙️ Internal | 단건 파일 확인 |
| POST | `/api/internal/files/confirm/bulk` | ⚙️ Internal | 다건 파일 확인 |
| POST | `/api/internal/files/presigned-urls` | ⚙️ Internal | 다건 조회용 Presigned URL 발급 |