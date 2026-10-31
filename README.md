# PrivyChat — Zero-Compromise Private Android Messenger

[![CI Build](https://github.com/privychat/privychat/actions/workflows/ci.yml/badge.svg)](https://github.com/privychat/privychat/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-green.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org)

**PrivyChat** is a production-grade, privacy-first, one-to-one secure messaging platform designed from the ground up for native Android with an enterprise Java/Spring Boot and PostgreSQL backend.

---

## Key Features

- **Strict One-to-One Topology**: No group chats, channels, feeds, or broadcast lists. All interactions are strictly private between two authenticated individuals.
- **True End-to-End Encryption (E2EE)**:
  - Cryptographic identity keys on Curve25519 (X25519) + Ed25519 signatures.
  - X3DH (Extended Triple Diffie-Hellman) prekey exchange with one-time prekey replenishment.
  - Double Ratchet protocol for post-compromise security and perfect forward secrecy.
  - Safety number fingerprint verification (60-digit comparison and QR code exchange).
- **Private Media & File Sharing**:
  - Encrypted locally on sender device using AES-256-GCM with random per-file keys.
  - Transferred via private Supabase Storage (`privychat-attachments`); plaintext keys never touch the server.
- **Profile Management & Identity Verification**:
  - Jetpack Compose Material 3 Profile screen with Photo Picker and Camera integration.
  - Profile image EXIF sanitization (automatic GPS and device metadata stripping) with private Supabase Storage object isolation.
  - Secure 2-step Email Change: pending address validation, single-use token verification, and security alerts to old/new addresses.
  - Secure 2-step Mobile Change: E.164 normalization, 6-digit OTP verification, rate limiting, and uniqueness enforcement.
  - Centralized privacy controls governing avatar and last-seen visibility (Everyone, Contacts, Nobody).
- **Voice & Video Real-Time Calling**:
  - WebRTC peer-to-peer media streams with SRTP encryption.
  - Authenticated STOMP/WebSocket signaling with ICE candidate exchange.
  - Full-screen incoming call UI, lock-screen notifications, and audio routing (Speaker, Earpiece, Bluetooth).
- **Durable Delivery & Offline Synchronization**:
  - Real-time STOMP message routing backed by durable PostgreSQL offline queue.
  - Client-side message deduplication and acknowledgment tokens.
  - Accurate Sent, Delivered, and Read statuses.
- **Contact Discovery & Invitations**:
  - Privacy-preserving contact matching.
  - Opaque token friend invitations delivered via responsive HTML emails.
- **Hardened Security Architecture**:
  - Android Keystore protection for master encryption keys.
  - Encrypted local Room database (SQLCipher).
  - Rate limiting on authentication, OTP requests, and message rates.
  - Account deletion with cryptographic shredding.

---

## Monorepo Architecture

```
PrivyChat/
├── android-app/           # Native Android client (Kotlin, Compose, Room, Hilt, WebRTC)
├── backend/               # Enterprise Spring Boot 3.4 backend (Java 21/24, PostgreSQL, STOMP)
├── infrastructure/        # Docker Compose (Postgres, MinIO, Coturn), Nginx, APK Distribution
├── docs/                  # Comprehensive architecture, security, and setup manuals
├── scripts/               # Automation scripts for local setup, migrations, and keystores
└── .github/workflows/     # CI/CD automation pipeline
```

---

## Quick Start Guide

### Prerequisites
- **Java**: JDK 21 or JDK 24
- **Maven**: 3.9+
- **Android SDK**: API 34+ (for Android client development)
- **Docker & Docker Compose**: (for local PostgreSQL, MinIO, and Coturn services)

### 1. Start Local Infrastructure
```powershell
# From the project root
cd infrastructure/docker
docker compose up -d
```
This launches:
- PostgreSQL (`localhost:5432`)
- MinIO Object Storage (`localhost:9000` / Console `localhost:9001`)
- Coturn STUN/TURN server (`localhost:3478`)

### 2. Run Backend
```powershell
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- API Base URL: `http://localhost:8080/api/v1`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui/index.html`
- Health Endpoint: `http://localhost:8080/actuator/health`

### 3. Build Android App
```powershell
cd android-app
# Generate debug APK
./gradlew assembleDebug

# Run unit and cryptography tests
./gradlew testDebugUnitTest
```

---

## Documentation Index

Detailed specifications and operations guides are located in `/docs`:
- [Architecture & Data Flows](docs/ARCHITECTURE.md)
- [Threat Model & Mitigations](docs/THREAT_MODEL.md)
- [Cryptographic Specification](docs/CRYPTOGRAPHY.md)
- [Database Schema & Migrations](docs/DATABASE.md)
- [REST & WebSocket API Guide](docs/API.md)
- [Android Setup & Device Testing](docs/ANDROID_SETUP.md)
- [Backend Setup & Migrations](docs/BACKEND_SETUP.md)
- [Email & Invitation Configuration](docs/EMAIL_SETUP.md)
- [Firebase & Push Notifications](docs/FIREBASE_SETUP.md)
- [WebRTC & TURN Configuration](docs/WEBRTC_SETUP.md)
- [Production Deployment Guide](docs/DEPLOYMENT.md)
- [APK Release & Verification](docs/RELEASE.md)
- [Security Audit & Compliance](docs/SECURITY_AUDIT.md)
- [Test Report & Evidence](docs/TEST_REPORT.md)
- [Operations & Incident Playbook](docs/OPERATIONS.md)

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

