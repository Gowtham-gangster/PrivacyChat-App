# PrivyChat REST & WebSocket API Documentation

## 1. Authentication & Security Headers

All REST endpoints under `/api/v1` (except public authentication and health routes) require an RFC 6750 Bearer JWT token in the `Authorization` header:

```http
Authorization: Bearer <access_token>
```

Tokens are valid for 15 minutes. Clients obtain fresh access tokens using the `POST /api/v1/auth/refresh` endpoint with their secure HTTP-only or encrypted refresh token.

---

## 2. REST Endpoints Specification

### A. Authentication & Onboarding
- **`POST /api/v1/auth/register`**
  - **Body**: `{ "displayName": "Alice", "email": "alice@example.com", "phoneNumber": "+12025550143", "password": "SecurePassword123!", "termsAccepted": true }`
  - **Response (201)**: `{ "userId": "...", "emailVerified": false, "phoneVerified": false }`
- **`POST /api/v1/auth/verify-email-otp`**
  - **Body**: `{ "email": "alice@example.com", "otpCode": "123456" }`
  - **Response (200)**: `{ "verified": true }`
- **`POST /api/v1/auth/verify-phone-otp`**
  - **Body**: `{ "phoneNumber": "+12025550143", "otpCode": "654321" }`
  - **Response (200)**: `{ "verified": true }`
- **`POST /api/v1/auth/login`**
  - **Body**: `{ "identifier": "+12025550143", "password": "SecurePassword123!", "deviceLabel": "Pixel 8 Pro" }`
  - **Response (200)**: `{ "accessToken": "...", "refreshToken": "...", "user": { ... }, "deviceId": "..." }`
- **`POST /api/v1/auth/refresh`**
  - **Body**: `{ "refreshToken": "..." }`
  - **Response (200)**: `{ "accessToken": "...", "refreshToken": "..." }`
- **`POST /api/v1/auth/logout`**
  - **Response (204 No Content)**

### B. Device Cryptographic Keys (E2EE)
- **`POST /api/v1/keys/bundle`**
  - Publishes or replenishes device public keys.
  - **Body**:
    ```json
    {
      "deviceId": "b6a1e944-7f12-429a-9e11-857e4b2d8d80",
      "identityKey": "base64(IK_pub)",
      "signedPreKey": {
        "keyId": 1,
        "publicKey": "base64(SPK_pub)",
        "signature": "base64(Ed25519_sig)"
      },
      "oneTimePreKeys": [
        {"keyId": 1, "publicKey": "base64(OPK1)"},
        {"keyId": 2, "publicKey": "base64(OPK2)"}
      ]
    }
    ```
- **`GET /api/v1/keys/bundle/{recipientUserId}`**
  - Retrieves recipient prekey bundle for X3DH initialization.
  - Automatically consumes one $OPK$.

### C. Contacts & Invitations
- **`POST /api/v1/contacts/discover`**
  - Matches hashed or normalized phone numbers.
  - **Body**: `{ "phoneNumbers": ["+12025550143", "+12025550199"] }`
  - **Response (200)**: `[ { "userId": "...", "displayName": "Bob", "registered": true, "avatarUrl": "..." } ]`
- **`POST /api/v1/invitations/send`**
  - Dispatches invitation email with opaque token and download URL.
  - **Body**: `{ "recipientEmail": "friend@example.com" }`
  - **Response (200)**: `{ "invitationId": "...", "status": "SENT" }`

### D. Conversations & Messages
- **`GET /api/v1/conversations`**
  - Returns user's active 1-to-1 conversations with unread counts and last message timestamp.
- **`POST /api/v1/conversations/direct`**
  - Gets or creates a 1-to-1 conversation with a specific contact.
  - **Body**: `{ "recipientUserId": "3fa85f64-5717-4562-b3fc-2c963f66afa6" }`
- **`GET /api/v1/conversations/{conversationId}/messages?beforeTimestamp=...&limit=50`**
  - Paged retrieval of message history.

### E. Private Attachments
- **`POST /api/v1/attachments/upload-request`**
  - Authorizes upload of an encrypted media blob.
  - **Body**: `{ "fileSizeBytes": 1048576, "sha256Checksum": "...", "mimeType": "image/jpeg" }`
  - **Response (200)**: `{ "attachmentId": "...", "uploadUrl": "http://.../upload" }`
- **`GET /api/v1/attachments/{attachmentId}/download`**
  - Authorizes download stream for authenticated participants.

---

## 3. Real-Time STOMP WebSocket Protocol

### Connection
- **Endpoint**: `wss://privychat.example.com/ws`
- **Handshake Headers**: `Authorization: Bearer <access_token>`

### Channel Subscriptions

| Destination | Purpose |
| :--- | :--- |
| `/topic/user.{userId}.messages` | Real-time inbound encrypted messages. |
| `/topic/user.{userId}.receipts` | Message delivery and read receipts (`SENT`, `DELIVERED`, `READ`). |
| `/topic/user.{userId}.call` | Real-time WebRTC call signaling (`OFFER`, `ANSWER`, `ICE`, `DECLINE`). |

### Message Send Destination (`/app/chat.send`)
```json
{
  "messageId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "recipientId": "4a712c9b-6381-420a-8d77-6e42b262a4d3",
  "ciphertext": "base64(encrypted_payload)",
  "iv": "base64(12_byte_nonce)",
  "ratchetPublicKey": "base64(ephemeral_dh_pub)",
  "sequenceNumber": 42,
  "previousCounter": 3,
  "oneTimePreKeyId": 104
}
```

### Call Signaling Destination (`/app/call.signal`)
```json
{
  "callId": "7e3b1c2d-9f4a-4e8b-b6c1-a2d3e4f5a6b7",
  "recipientId": "4a712c9b-6381-420a-8d77-6e42b262a4d3",
  "signalType": "OFFER", // OFFER, ANSWER, ICE_CANDIDATE, RINGING, DECLINE, HANGUP
  "callType": "VIDEO",   // AUDIO, VIDEO
  "sdp": "v=0\r\no=...",
  "candidate": {
    "sdpMid": "0",
    "sdpMLineIndex": 0,
    "candidate": "candidate:842163049 1 udp 1677729535 192.168.1.100 56788 typ host..."
  }
}
```

