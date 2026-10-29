# PrivyChat Android Client Setup & Build Guide

## 1. Prerequisites & Environment

- **Android Studio**: Android Studio Jellyfish (2023.3.1+) or Ladybug (2024.2+)
- **JDK**: Java 17 or Java 21 (configured in Android Studio under Settings > Build > Build Tools > Gradle)
- **Android SDK**:
  - `compileSdk`: 34
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk`: 34 (Android 14)
- **Gradle**: 8.9 (via Gradle Wrapper)
- **Android Gradle Plugin (AGP)**: 8.5.2

---

## 2. Project Architecture & Stack

- **UI**: Jetpack Compose, Material 3, Navigation Compose
- **Architecture**: MVVM with unidirectional data flow (UI $\to$ ViewModel $\to$ Repository $\to$ Local/Remote Data Sources)
- **Dependency Injection**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`)
- **Local Persistence**: Room 2.6+ with **SQLCipher** (encrypted SQLite datastore)
- **Cryptography**: Curve25519 (X25519), Ed25519, HKDF, AES-256-GCM, Android Keystore
- **WebRTC**: `org.webrtc:google-webrtc:1.0.32006` or maintained WebRTC Android library
- **Networking**: Retrofit 2 + OkHttp 4 + Scarlet / Java-WebSocket for STOMP
- **Push Notifications**: Firebase Cloud Messaging (`com.google.firebase:firebase-messaging-ktx`)
- **Permissions**: System Photo Picker (`ActivityResultContracts.PickVisualMedia`), SAF for documents, CameraX for calling/in-app camera

---

## 3. Configuration & Brand Theming

All brand identity, package identifiers, server URLs, and color palettes are centrally defined and customizable:

### Brand Configuration (`PrivyConfig.kt`)
```kotlin
package com.privychat.app.core

object PrivyConfig {
    const val APP_NAME = "PrivyChat"
    const val PACKAGE_NAME = "com.privychat.app"
    
    // Server Endpoints
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/api/v1/" // 10.0.2.2 for Android Emulator
    const val DEFAULT_WS_URL = "ws://10.0.2.2:8080/ws"
    
    // STUN / TURN Defaults
    const val DEFAULT_STUN_SERVER = "stun:stun.l.google.com:19302"
    const val DEFAULT_TURN_SERVER = "turn:turn.privychat.example.com:3478"
    
    // File Limits
    const val MAX_ATTACHMENT_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB
}
```

---

## 4. Building the Application

### Debug Build
```powershell
cd android-app
./gradlew assembleDebug
```
Output APK location:
`android-app/app/build/outputs/apk/debug/app-debug.apk`

### Running Unit Tests
```powershell
./gradlew testDebugUnitTest
```

### Release Build with Keystore Signing
1. Generate a production keystore (or use the provided helper script `scripts/generate-release-keystore.ps1`):
   ```powershell
   keytool -genkeypair -v -keystore release.keystore -alias privychat -keyalg RSA -keysize 4096 -validity 10000
   ```
2. Set environment variables:
   - `KEYSTORE_PATH=release.keystore`
   - `KEYSTORE_PASSWORD=your_password`
   - `KEY_ALIAS=privychat`
   - `KEY_PASSWORD=your_password`
3. Execute the release build:
   ```powershell
   ./gradlew assembleRelease
   ```
   Output APK: `android-app/app/build/outputs/apk/release/app-release.apk`

