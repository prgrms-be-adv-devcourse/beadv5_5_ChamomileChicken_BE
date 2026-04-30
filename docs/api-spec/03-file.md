# 03. File API (Product Service, 9004)

> 인증 범례 및 공통 응답 형식 → [API_SPEC.md](../API_SPEC.md)

> ⚠️ File API는 별도 서비스가 아니라 **Product Service (port 9004)** 에 통합되어 있습니다.
> 게이트웨이에서 `/api/v1/files/**` → `:9004` 로 라우팅됩니다.

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

### PATCH `/api/v1/files/{fileId}/complete`

**Path Variables**: `fileId` (UUID)

**Response** `200 OK`
