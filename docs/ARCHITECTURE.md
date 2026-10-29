# PrivyChat System Architecture & Data Flows

## 1. High-Level Component Architecture

PrivyChat is structured into distinct, decoupled components designed for low latency, high resilience, and zero trust.

```mermaid
flowchart TD
    subgraph Client["Android Native Client"]
        UI["Jetpack Compose UI\n(Material 3 / MVVM)"]
        VM["State ViewModels"]
        Repo["Data Repositories"]
        Crypto["E2EE Cryptographic Engine\n(Double Ratchet, X3DH, AES-GCM)"]
        Room["Encrypted Room DB\n(SQLCipher / Keystore)"]
        RTC["WebRTC Client Engine\n(Audio/Video, ICE, SRTP)"]
        WS["WebSocket STOMP Client\n(Realtime Messaging)"]
        HTTP["Retrofit REST Client\n(Auth, Keys, Attachments)"]
    end

    subgraph Edge["Network & Edge Infrastructure"]
        NGINX["NGINX Reverse Proxy\n(TLS 1.3 Termination, WSS, Rate Limit)"]
        TURN["Coturn STUN / TURN Server\n(NAT Traversal, Relay)"]
    end

    subgraph Backend["Spring Boot Backend"]
        Security["Spring Security\n(JWT, Authorization, RBAC)"]
        REST["Spring REST Controllers\n(Auth, Contacts, Keys, Storage)"]
        STOMP["Spring STOMP Broker\n(Message Routing & Signaling)"]
        Services["Domain Business Services\n(Message, Call, Email, FCM)"]
        Flyway["Flyway Migration Engine"]
    end

    subgraph Data["Persistent & External Storage"]
        Postgres[("PostgreSQL 16 Database\n(Metadata, Queues, Receipts)")]
        S3[("Private Object Storage\n(Encrypted Attachments)")]
        FCM["Firebase Cloud Messaging\n(Wakeup & Call Push)"]
        SMTP["SMTP Mail Provider\n(Invitations & Verification)"]
    end

    UI --> VM
    VM --> Repo
    Repo --> Crypto
    Repo --> Room
    Repo --> RTC
    Repo --> WS
    Repo --> HTTP

    WS --> NGINX
    HTTP --> NGINX
    RTC -. P2P Media / SRTP .-> TURN

    NGINX --> Security
    Security --> REST
    Security --> STOMP

    REST --> Services
    STOMP --> Services
    Services --> Postgres
    Services --> S3
    Services --> FCM
    Services --> SMTP
    Flyway --> Postgres
```

---

## 2. Core Communication & Encryption Flows

### A. Extended Triple Diffie-Hellman (X3DH) Initial Handshake

When User A (Alice) wants to initiate a conversation with User B (Bob) for the first time:

```mermaid
sequenceDiagram
    autonumber
    participant Alice as Alice (Client)
    participant Server as PrivyChat Backend
    participant Bob as Bob (Client)

    Note over Bob,Server: Bob uploads keys upon registration
    Bob->>Server: Publish Identity Key (IK_B), Signed Prekey (SPK_B), Signature, One-Time Prekeys (OPK_B[])
    
    Note over Alice,Server: Alice initiates conversation with Bob
    Alice->>Server: GET /api/v1/keys/bundle/{bob_user_id}
    Server-->>Alice: Returns IK_B, SPK_B, OPK_B_1, Signature_B
    Server->>Server: Mark OPK_B_1 as consumed

    Note over Alice: Verify Signature on SPK_B using IK_B
    Note over Alice: Generate Ephemeral Key Pair (EK_A)
    Note over Alice: Compute DH1 = DH(IK_A, SPK_B)
    Note over Alice: Compute DH2 = DH(EK_A, IK_B)
    Note over Alice: Compute DH3 = DH(EK_A, SPK_B)
    Note over Alice: Compute DH4 = DH(EK_A, OPK_B_1)
    Note over Alice: Master Key = HKDF(DH1 || DH2 || DH3 || DH4)
    Note over Alice: Initialize Double Ratchet sending chain

    Alice->>Server: POST /api/v1/messages (Initial message with IK_A, EK_A, OPK_B_id, Ciphertext)
    Server->>Bob: Deliver message over STOMP WebSocket or queue offline
    
    Note over Bob: Bob receives IK_A, EK_A, OPK_B_id
    Note over Bob: Compute DH1, DH2, DH3, DH4 using private keys
    Note over Bob: Derive identical Master Key via HKDF
    Note over Bob: Initialize Double Ratchet receiving chain & Decrypt message
```

---

### B. Message Delivery & Offline Synchronization Flow

```mermaid
sequenceDiagram
    autonumber
    participant Alice as Sender (Alice)
    participant WS as STOMP Gateway
    participant DB as PostgreSQL
    participant Bob as Recipient (Bob)

    Alice->>Alice: Encrypt message via Double Ratchet
    Alice->>Alice: Save message locally as SENDING
    Alice->>WS: SEND /app/chat.send {messageId, recipientId, ciphertext, iv, salt, sequence}
    
    WS->>DB: INSERT into messages (status = ACCEPTED)
    WS-->>Alice: ACK /topic/user.{alice}.receipts {messageId, status: SENT}
    Alice->>Alice: Update message status to SENT

    alt Bob is Online (Active WebSocket)
        WS->>Bob: SEND /topic/user.{bob}.messages {messageId, senderId, ciphertext, ...}
        Bob->>Bob: Decrypt ciphertext & store in local Room DB
        Bob->>WS: SEND /app/chat.receipt {messageId, type: DELIVERED}
        WS->>DB: UPDATE messages SET status = DELIVERED
        WS->>Alice: SEND /topic/user.{alice}.receipts {messageId, status: DELIVERED}
        Alice->>Alice: Update UI ticks (Double Checkmark)
        
        opt Bob Reads Message
            Bob->>WS: SEND /app/chat.receipt {messageId, type: READ}
            WS->>DB: UPDATE messages SET status = READ
            WS->>Alice: SEND /topic/user.{alice}.receipts {messageId, status: READ}
            Alice->>Alice: Update UI ticks (Blue/Highlighted Checkmarks)
        end
    else Bob is Offline
        WS->>DB: Enqueue in pending_offline_queue
        WS->>FCM: Dispatch silent wake-up push notification
        Note over Bob: Bob reconnects later
        Bob->>WS: CONNECT & Subscribe /app/chat.sync
        WS->>DB: SELECT pending messages WHERE recipient_id = Bob
        WS-->>Bob: Stream pending messages in batch
        Bob-->>WS: Batch acknowledge received message IDs
        WS->>DB: Remove acknowledged messages from queue
    end
```

---

### C. Private Media Attachment Sharing Flow

```mermaid
sequenceDiagram
    autonumber
    participant Alice as Sender (Alice)
    participant Server as PrivyChat Backend
    participant Storage as Object Storage (S3/MinIO)
    participant Bob as Recipient (Bob)

    Alice->>Alice: User selects image / video / document
    Alice->>Alice: Generate random 256-bit AES-GCM Key (K_att) & Nonce (IV)
    Alice->>Alice: Encrypt file bytes locally -> Ciphertext
    Alice->>Alice: Compute SHA-256 digest of ciphertext
    
    Alice->>Server: POST /api/v1/attachments/upload-request {size, sha256Digest, mimeType: application/octet-stream}
    Server-->>Alice: Returns uploadUrl (Pre-signed PUT / authorized token) and attachmentId
    
    Alice->>Storage: PUT /attachments/{attachmentId} with Ciphertext
    Storage-->>Alice: 200 OK
    
    Alice->>Alice: Construct media metadata envelope: {attachmentId, K_att, IV, originalFileName, sha256, mimeType}
    Alice->>Alice: Encrypt metadata envelope inside E2EE message payload
    Alice->>Server: Send E2EE message containing media envelope
    
    Server->>Bob: Deliver E2EE message
    Bob->>Bob: Decrypt E2EE message payload -> Extract {attachmentId, K_att, IV, sha256}
    
    Bob->>Server: GET /api/v1/attachments/{attachmentId}/download-request
    Server->>Server: Verify Bob is authorized conversation participant
    Server-->>Bob: Returns downloadUrl
    
    Bob->>Storage: GET /attachments/{attachmentId} -> Download Ciphertext
    Bob->>Bob: Verify SHA-256 checksum
    Bob->>Bob: Decrypt Ciphertext with K_att and IV
    Bob->>Bob: Store decrypted file in private app storage & render in UI
```

---

### D. WebRTC Audio / Video Calling Flow

```mermaid
sequenceDiagram
    autonumber
    participant Alice as Caller (Alice)
    participant Server as STOMP Signaling Broker
    participant TURN as Coturn (STUN/TURN)
    participant Bob as Callee (Bob)

    Alice->>Server: SEND /app/call.offer {callId, calleeId, callType: VIDEO/AUDIO, sdpOffer}
    Server->>Server: Validate authorization & session state
    Server->>Bob: SEND /topic/user.{bob}.call {callId, callerId, sdpOffer, type: INCOMING}
    Server->>Bob: Trigger High-Priority FCM Push Notification (if in background)
    
    Note over Bob: Display Incoming Call Full-Screen UI & Play Ringtone
    
    Bob->>Server: SEND /app/call.ringing {callId, callerId}
    Server->>Alice: SEND /topic/user.{alice}.call {callId, state: RINGING}
    
    alt Bob Answers
        Bob->>Bob: Initialize WebRTC PeerConnection & generate SDP Answer
        Bob->>Server: SEND /app/call.answer {callId, callerId, sdpAnswer}
        Server->>Alice: SEND /topic/user.{alice}.call {callId, sdpAnswer, state: ANSWERED}
        
        par ICE Candidate Exchange
            Alice->>TURN: Query STUN/TURN for ICE candidates
            Alice->>Server: SEND /app/call.ice {callId, recipientId: Bob, candidate}
            Server->>Bob: Forward ICE candidate
        and
            Bob->>TURN: Query STUN/TURN for ICE candidates
            Bob->>Server: SEND /app/call.ice {callId, recipientId: Alice, candidate}
            Server->>Alice: Forward ICE candidate
        end
        
        Note over Alice,Bob: Direct P2P SRTP Media Stream established (or relayed via TURN)
        Alice<<-->>Bob: Bi-directional Encrypted Audio & Video Media (SRTP)
    else Bob Declines / Hangs Up
        Bob->>Server: SEND /app/call.decline {callId, callerId, reason: REJECTED}
        Server->>Alice: SEND /topic/user.{alice}.call {callId, state: DECLINED}
        Alice->>Alice: Terminate call session & record in call log
    end
```

