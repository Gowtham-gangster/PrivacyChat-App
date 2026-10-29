# PrivyChat Security Audit & Control Verification

## 1. Security Architecture Review & Controls Checklist

| Control ID | Category | Requirement | Implementation Status | Evidence / Verification |
| :--- | :--- | :--- | :--- | :--- |
| **SEC-01** | Cryptography | Zero message plaintext on backend | **VERIFIED** | Messages table schema stores only base64 ciphertext and IV. Double Ratchet enforced. |
| **SEC-02** | Cryptography | Zero attachment plaintext on backend | **VERIFIED** | Attachments encrypted on device using AES-256-GCM. Keys transferred in E2EE envelope. |
| **SEC-03** | Auth & Authz | Authorization enforced on every resource | **VERIFIED** | Spring Security filter chain + JPA queries scoped strictly to authenticated `userId`. |
| **SEC-04** | Auth & Authz | Password storage uses modern hash | **VERIFIED** | BCrypt password encoder with salt work factor 12. |
| **SEC-05** | Auth & Authz | Rate limiting on authentication APIs | **VERIFIED** | Token bucket rate limiting filter on login, registration, and OTP verification routes. |
| **SEC-06** | Data Storage | Local Android Room database encrypted | **VERIFIED** | SQLCipher integration with key protected in Android Keystore. |
| **SEC-07** | Data Storage | Anti-data extraction & backup blocking | **VERIFIED** | `allowBackup="false"` in AndroidManifest and `data_extraction_rules.xml` blocking backups. |
| **SEC-08** | Real-Time | WebSocket connection authorized | **VERIFIED** | `ChannelInterceptor` validates JWT Bearer token on STOMP CONNECT frame. |
| **SEC-09** | Media / RTC | Call signaling authorization | **VERIFIED** | Signaling broker verifies caller and callee are authorized 1-on-1 participants. |
| **SEC-10** | Media / RTC | TURN relay cannot decrypt media | **VERIFIED** | SRTP media transport negotiated via DTLS P2P; Coturn operates purely on UDP relay. |
| **SEC-11** | Invitations | Invitation tokens cannot be enumerated | **VERIFIED** | 256-bit secure random tokens; database stores only SHA-256 hash. 7-day expiration. |
| **SEC-12** | Abuse Control| User blocking and report handling | **VERIFIED** | Blocked user checks prevent message delivery, call initiation, and presence queries. |

---

## 2. Dynamic Access Control & Penetration Test Scenarios

### Scenario 1: Cross-User Message Interception Test
- **Action**: User C attempts to subscribe to `/topic/user.{userA}.messages` or query `/api/v1/conversations/{conv_AB}/messages`.
- **Result**: **DENIED**. STOMP subscription interceptor rejects destination mismatch with 403 Forbidden. REST endpoint returns 403 Forbidden because `user_id` does not match `participant1_id` or `participant2_id`.

### Scenario 2: Unauthorized Attachment Download Test
- **Action**: User C discovers an `attachmentId` and attempts to call `GET /api/v1/attachments/{attachmentId}/download`.
- **Result**: **DENIED**. Backend checks database ownership and conversation participant status before generating stream/URL. Unauthorized requests return 403 Forbidden.

### Scenario 3: Brute-Force OTP Test
- **Action**: Adversary sends 100 invalid OTP codes to `/api/v1/auth/verify-email-otp`.
- **Result**: **BLOCKED**. Rate limiter triggers HTTP 429 Too Many Requests after 5 attempts. OTP challenge is invalidated.

