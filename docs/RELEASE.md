# PrivyChat Release, Signing & Verification Guide

## 1. APK Signing Architecture

PrivyChat enforces **APK Signature Scheme v3** (and v2 fallback) using a dedicated, high-entropy 4096-bit RSA or EC key.

### Keystore Generation Script
Use the provided PowerShell script in `scripts/generate-release-keystore.ps1` or run:
```powershell
keytool -genkeypair -v `
  -keystore release.keystore `
  -alias privychat `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000 `
  -dname "CN=PrivyChat Security, OU=Engineering, O=PrivyChat, C=US"
```

---

## 2. Release Build Execution

```powershell
cd android-app
./gradlew assembleRelease
```
The resulting signed artifact is created at:
`android-app/app/build/outputs/apk/release/app-release.apk`

---

## 3. Cryptographic Signature Verification

Users and administrators can verify the authenticity and integrity of the APK before installation:

### Verifying with `apksigner`
```powershell
apksigner verify --verbose --print-certs app-release.apk
```
Expected output:
- `Verifies: true`
- `Signer #1 certificate DN: CN=PrivyChat Security...`
- `Signer #1 certificate SHA-256 digest: <published_fingerprint>`

### Calculating SHA-256 Checksum
```powershell
Get-FileHash -Algorithm SHA256 app-release.apk
```

---

## 4. Controlled HTTPS Distribution Portal

A dedicated, lightweight download landing page is provided in `infrastructure/distribution/index.html`. It displays:
- Verified App Version and Release Date.
- Package Identifier: `com.privychat.app`.
- Direct HTTPS Download Link.
- SHA-256 Checksum and Signing Certificate Fingerprint for end-user verification.

