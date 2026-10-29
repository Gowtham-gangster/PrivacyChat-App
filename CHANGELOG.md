# Changelog

All notable changes to the **PrivyChat** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0-rc1] - 2026-10-29

### Added
- **Core Architecture**: Modular monorepo with native Kotlin/Jetpack Compose Android app and Java/Spring Boot backend.
- **End-to-End Encryption**: Double Ratchet implementation with Curve25519 (X25519) + Ed25519, HKDF-SHA256, and AES-256-GCM.
- **Prekey Protocol**: X3DH key agreement with Identity Keys, Signed Prekeys, and One-Time Prekey pools.
- **Private Attachment Sharing**: Client-side AES-GCM file encryption, encrypted media storage in private S3/MinIO bucket, and short-lived authorized stream access.
- **Real-Time Delivery & Offline Sync**: WebSocket/STOMP signaling, durable PostgreSQL message queue, deduplication, delivery receipts, and read state synchronization.
- **Voice & Video Calling**: WebRTC 1-to-1 audio and video calling engine, STOMP call signaling, ICE candidate exchange, full-screen incoming call UI, and audio device management.
- **Account & Verification**: Dual-factor email and mobile number verification, BCrypt password hashing, short-lived JWT access tokens with secure refresh tokens.
- **Contact Discovery & Invitations**: Opaque token-based friend invitation system with responsive HTML email delivery and consent-driven phone number matching.
- **Local Persistence & Security**: Room database with SQLCipher encryption, Android Keystore protection, biometric authentication support, and strict backup exclusion rules.
- **DevOps & Infrastructure**: Complete Docker Compose setup (Postgres, MinIO, Coturn TURN server), Flyway database migrations, CI/CD GitHub Actions workflow, and HTTPS APK download verification portal.
- **Documentation**: 16 dedicated specification and operational manuals covering threat modeling, cryptography, database schema, APIs, WebRTC, Firebase, security auditing, and deployment.

