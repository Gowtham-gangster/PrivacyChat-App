# PrivyChat Threat Model & Security Evaluation

## 1. Scope & System Assets

### Protected Assets
1. **Message Content**: Plaintext of text messages, voice notes, locations, and reactions.
2. **Media Attachments**: Photos, videos, voice recordings, PDFs, and documents.
3. **Cryptographic Key Material**:
   - Device Identity Private Keys ($IK_{priv}$).
   - Ephemeral Ratchet Private Keys.
   - Root, Chain, and Message Derived Keys.
   - Attachment Ephemeral Encryption Keys.
4. **User Identifiers & Metadata**:
   - Phone numbers, email addresses, contact lists, and social graph.
   - Message timestamps and communication frequency.
5. **Account Credentials**: Passwords, OTP verification codes, and JWT refresh tokens.
6. **Real-time Call Media**: Audio and video streams during live calls.

---

## 2. Adversary Models & Threat Scenarios

| Adversary | Capabilities | Assumptions / Objectives |
| :--- | :--- | :--- |
| **Untrusted Network Observer (ISP / Public Wi-Fi / Man-in-the-Middle)** | Passive interception, packet sniffing, active TLS proxy injection attempts, DNS spoofing. | Attempt to inspect message plaintext, intercept files, hijack sessions, or spoof calling signaling. |
| **Compromised Backend / Rogue Administrator** | Full read/write access to PostgreSQL database, object storage buckets, and application server memory. | Attempt to read user messages, steal attachments, forge messages, or inspect contact graphs. |
| **Local Device Intruder / Stolen Device** | Physical possession of the Android device, non-root user access, USB debugging, or backup extraction. | Extract Room database, read cached plaintext, or recover private identity keys. |
| **Malicious Peer / Disgruntled Contact** | Legitimate participant in a conversation or uninvited sender. | Send malicious file payloads, spam invitations, attempt user enumeration, or trigger denial-of-service. |
| **Relay / TURN Server Operator** | Full visibility into packets traversing the TURN media relay. | Attempt to eavesdrop on audio/video call streams. |

---

## 3. Threat Mitigation Matrix

### A. Network & Transport Security
- **Threat**: Man-in-the-middle decryption or downgrade attacks on REST API and WebSocket connections.
- **Mitigation**:
  - Mandatory TLS 1.3 with forward-secure cipher suites (`TLS_AES_128_GCM_SHA256`, `TLS_AES_256_GCM_SHA384`).
  - Network Security Config on Android enforcing `cleartextTrafficPermitted="false"`.
  - Certificate Pinning enabled in Android OkHttp client for production hosts.
  - WSS (WebSocket Secure) with STOMP authentication token validation on initial handshake.

### B. Backend Compromise & Server-Side Snooping
- **Threat**: Attacker breaches backend server or dumps PostgreSQL database to read user chats.
- **Mitigation**:
  - **Zero-Plaintext Architecture**: All messages are encrypted end-to-end using Double Ratchet before leaving the client. The database stores only base64 ciphertext envelopes.
  - **Zero Attachment Plaintext**: Files are encrypted with AES-256-GCM using client-generated keys. S3 buckets store encrypted binary blobs. The decryption key is sent inside the E2EE payload; the server never has the key.
  - **Ephemeral Message Pruning**: Messages delivered to online clients are acknowledged and purged from delivery queues according to retention policies.
  - **Metadata Minimization**: Filenames, MIME types, and message lengths are obfuscated or placed inside encrypted payloads.

### C. Cryptographic Compromise & Replay Attacks
- **Threat**: Attacker steals an ephemeral key or intercepts an old ciphertext to replay or decrypt future messages.
- **Mitigation**:
  - **Perfect Forward Secrecy (PFS)**: Diffie-Hellman ratcheting creates fresh keys for every roundtrip. Compromise of a current key cannot decrypt past messages.
  - **Post-Compromise Security (Break-in Recovery)**: Sending a new message after compromise refreshes the DH ratchet, locking an attacker out of future messages as soon as the legitimate device communicates.
  - **Message Replay Prevention**: Unique 128-bit message UUIDs and monotonic ratchet sequence numbers verified by recipient; replayed messages are rejected by the ratchet state machine.

### D. Device Theft & Forensic Data Extraction
- **Threat**: Physical extraction of SQLite databases from flash storage or via ADB backup.
- **Mitigation**:
  - Room database encrypted via **SQLCipher** (AES-256 in CBC mode with HMAC-SHA512).
  - Database encryption key generated securely and stored in the **Android Keystore**, backed by hardware Secure Element (SE) / StrongBox where available.
  - `android:allowBackup="false"` and explicit `data_extraction_rules.xml` blocking cloud and local device backups.
  - App screens protected with `FLAG_SECURE` in sensitive views to prevent screenshots and task switcher previews.

### E. Attachment Security & Malicious Payloads
- **Threat**: Attacker sends executable malware, path traversal archives, or oversized files to exhaust server/device resources.
- **Mitigation**:
  - Client-side pre-upload file size limits enforced (e.g. 50MB max).
  - Backend enforces strict Content-Length verification before issuing storage presigned URLs.
  - Storage paths use server-generated opaque UUIDs; original client filenames are never used as filesystem or S3 object keys.
  - Files are processed through Android's Storage Access Framework / Photo Picker with sandboxed content URIs.

### F. WebRTC Media Relay Privacy
- **Threat**: Eavesdropping on voice/video calls by the TURN relay operator.
- **Mitigation**:
  - Media streams are protected end-to-end via **SRTP** (Secure Real-time Transport Protocol) with DTLS (Datagram Transport Layer Security) key negotiation.
  - Coturn acts strictly as a packet relay at the IP/UDP transport layer and cannot access SRTP media keys or decrypt audio/video packets.

---

## 4. Known Residual Risks & Explicit Limitations

1. **Compromised Client OS / Root Exploits**:
   If an adversary gains kernel or root access on the Android device (e.g. via an active spyware payload or memory inspection tool like Frida), memory containing decrypted messages currently displayed in the Compose UI could theoretically be inspected. This is an inherent limitation of endpoint security on any operating system.
2. **Metadata Retention (Traffic Analysis)**:
   While message text and attachments are mathematically unreadable by the server, the server must process the sender ID, recipient ID, message submission timestamp, and ciphertext byte length to route packets and manage delivery queues.
3. **Screenshot Capabilities on Custom ROMs**:
   While `FLAG_SECURE` blocks standard Android screen capture and recents previews, modified Android operating systems with patched surfaceflinger frameworks can capture screen buffers.
4. **Disappearing Messages Limitation**:
   Ephemeral/disappearing messages are enforced locally by the application scheduling local Room deletion. A modified third-party client could ignore the self-destruct timer or take external photos of the screen.

