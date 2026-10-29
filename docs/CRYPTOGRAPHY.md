# PrivyChat Cryptographic Specification

## 1. Overview & Protocol Foundations

PrivyChat enforces end-to-end encryption for all one-to-one communications. The cryptographic architecture is based on the **Double Ratchet Algorithm** and the **Extended Triple Diffie-Hellman (X3DH)** key agreement protocol, providing:
- **Confidentiality**: Only the intended recipient device can decrypt messages.
- **Integrity & Authentication**: Every message is authenticated; tampering is detected.
- **Perfect Forward Secrecy (PFS)**: Compromise of current keys does not compromise past messages.
- **Post-Compromise Security (PCS)**: A compromised session automatically self-heals as new messages are exchanged.

---

## 2. Cryptographic Primitives & Parameters

| Purpose | Algorithm / Primitive | Security Level | Details |
| :--- | :--- | :--- | :--- |
| **Asymmetric Key Exchange** | Curve25519 (X25519) | 128-bit (256-bit key) | RFC 7748 Montgomery curve Diffie-Hellman |
| **Digital Signatures** | Ed25519 | 128-bit (256-bit key) | RFC 8032 Edwards-curve Digital Signature Algorithm |
| **Key Derivation (KDF)** | HKDF-SHA256 | 256-bit | RFC 5869 HMAC-based Extract-and-Expand KDF |
| **Symmetric Message Cipher** | AES-256-GCM / ChaCha20-Poly1305 | 256-bit | Authenticated Encryption with Associated Data (AEAD) |
| **Attachment Cipher** | AES-256-GCM | 256-bit | 96-bit random IV, 128-bit authentication tag |
| **Password Hashing** | BCrypt / Argon2id | Work factor 12 | Salted iterative password hashing |
| **Random Number Generation** | `SecureRandom` (Android Linux /dev/urandom) | CSPRNG | Cryptographically secure pseudo-random numbers |
| **Safety Number Generation** | SHA-512 | 512-bit digest | 60-digit numeric representation & QR code |

---

## 3. Key Lifecycle & Prekey Bundles (X3DH)

### A. Device Registration Key Generation
Upon initial registration or device setup, the Android client generates:
1. **Identity Key Pair ($IK$)**: Long-term Curve25519 / Ed25519 key pair representing the device. The private key never leaves the device.
2. **Signed Prekey ($SPK$)**: Medium-term Curve25519 key pair. The client signs $SPK_{pub}$ with $IK_{priv}$:
   $$\text{Signature} = \text{Ed25519Sign}(IK_{priv}, SPK_{pub})$$
3. **One-Time Prekeys ($OPK_1, OPK_2, \dots, OPK_N$)**: A pool of 100 ephemeral Curve25519 key pairs.

The public keys and signature are uploaded to the backend server via `POST /api/v1/keys/bundle`:
```json
{
  "deviceId": "b6a1e944-7f12-429a-9e11-857e4b2d8d80",
  "identityKey": "base64(IK_pub)",
  "signedPreKey": {
    "keyId": 1,
    "publicKey": "base64(SPK_pub)",
    "signature": "base64(Signature)"
  },
  "oneTimePreKeys": [
    {"keyId": 101, "publicKey": "base64(OPK_1_pub)"},
    {"keyId": 102, "publicKey": "base64(OPK_2_pub)"}
  ]
}
```

### B. Session Initialization (X3DH Agreement)
When Alice wants to contact Bob:
1. Alice requests Bob's prekey bundle from the server:
   - Server returns: $IK_B$, $SPK_B$, $\text{Signature}_B$, and consumes one $OPK_B$ (e.g. $OPK_{B,1}$).
2. Alice verifies Bob's signature on $SPK_B$ using $IK_B$:
   $$\text{Ed25519Verify}(IK_B, SPK_B, \text{Signature}_B) == \text{TRUE}$$
3. Alice generates an Ephemeral Key Pair ($EK_A$).
4. Alice computes up to four Diffie-Hellman agreements:
   $$DH_1 = \text{X25519}(IK_{A,priv}, SPK_{B,pub})$$
   $$DH_2 = \text{X25519}(EK_{A,priv}, IK_{B,pub})$$
   $$DH_3 = \text{X25519}(EK_{A,priv}, SPK_{B,pub})$$
   $$DH_4 = \text{X25519}(EK_{A,priv}, OPK_{B,pub})$$
5. Alice derives the Master Secret ($SK$):
   $$SK = \text{HKDF-SHA256}(\text{info}="PrivyChat-X3DH", \text{ikm}=DH_1 \parallel DH_2 \parallel DH_3 \parallel DH_4)$$
6. Alice initializes her Double Ratchet sending state with $SK$.
7. In her initial message, Alice attaches:
   - Alice's $IK_{A,pub}$
   - Alice's $EK_{A,pub}$
   - Bob's $OPK_{B,id}$ used (if any)
   - The encrypted ciphertext

When Bob receives the message, he loads $IK_{A,pub}$ and $EK_{A,pub}$, loads his private keys for $IK_B$, $SPK_B$, and the indicated $OPK_B$, computes the identical DH values, derives $SK$, and initializes his Double Ratchet state to decrypt.

---

## 4. Double Ratchet Protocol Operation

The Double Ratchet combines two ratchets:
1. **Diffie-Hellman Ratchet**: Initiated whenever a new public key is received from the counterparty.
2. **Symmetric-Key KDF Ratchet**: Advanced on every message sent or received within a DH turn.

```
                  Root Key (RK)
                        │
                        ▼
            ┌──────────────────────┐
   DH Out ──┤ HKDF(RK, DH(Ratchet)) ├──► Next Root Key (RK')
            └───────────┬──────────┘
                        │
                        ▼
               Chain Key (CK)
                        │
                        ▼
            ┌──────────────────────┐
            │   HKDF(CK, "chain")  ├──► Next Chain Key (CK')
            └───────────┬──────────┘
                        │
                        ▼
               Message Key (MK) ──► Encrypts / Decrypts Message with AES-256-GCM
```

### Out-of-Order Message Handling
If message $N+2$ arrives before message $N+1$:
1. The receiving ratchet advances the chain key twice.
2. The intermediate Message Key ($MK_{N+1}$) is calculated and stored in an encrypted temporary cache in Room (`skipped_keys` table with TTL of 7 days).
3. Message $N+2$ is decrypted immediately.
4. When message $N+1$ subsequently arrives, $MK_{N+1}$ is retrieved from cache, the message is decrypted, and $MK_{N+1}$ is permanently zeroized and deleted.

---

## 5. Attachment Encryption Pipeline

Attachments are never sent in plaintext:
1. **Key Generation**: 256-bit cryptographically secure random key $K_{att} \leftarrow \text{SecureRandom}(32)$ and 96-bit nonce $IV \leftarrow \text{SecureRandom}(12)$.
2. **Local Encryption**:
   $$\text{Ciphertext}, \text{Tag} = \text{AES-GCM-256}(K_{att}, IV, \text{FileBytes})$$
3. **Upload**: $\text{Ciphertext} \parallel \text{Tag}$ uploaded to private S3 bucket at `/attachments/{attachmentId}`.
4. **Envelope Generation**: An attachment descriptor is serialized:
   ```json
   {
     "attachmentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
     "key": "base64(K_att)",
     "iv": "base64(IV)",
     "sha256": "digest_of_ciphertext",
     "fileName": "contract.pdf",
     "fileSize": 1048576,
     "mimeType": "application/pdf"
   }
   ```
5. **E2EE Envelope Delivery**: The descriptor is encrypted using the active Double Ratchet message key and sent as the text payload of the message. The server sees only the opaque ciphertext.
6. **Recipient Decryption**: The recipient decrypts the message, downloads the ciphertext from S3, verifies the SHA-256 checksum, and decrypts with $K_{att}$ and $IV$.

---

## 6. Safety Numbers (Identity Verification)

To prevent Active Man-in-the-Middle attacks, PrivyChat supports visual and QR-code Safety Number verification:
1. Sort the public identity keys of both parties lexicographically:
   $$\text{SortedKeys} = \min(IK_A, IK_B) \parallel \max(IK_A, IK_B)$$
2. Compute repeated SHA-512 digests:
   $$\text{Digest} = \text{SHA-512}(\text{SortedKeys} \parallel \text{ConversationId})$$
3. Format the first 32 bytes of the digest into a 60-digit number (12 blocks of 5 digits):
   ```
   12345 67890 54321 09876
   98765 43210 12345 67890
   54321 09876 98765 43210
   ```
4. Users compare the numbers on their physical screens or scan each other's generated QR code.
5. If a contact's identity key changes (e.g. app reinstalled), the client displays a prominent warning:
   > ⚠️ **Security Alert**: Your safety number with [Name] has changed. Tap to verify.

