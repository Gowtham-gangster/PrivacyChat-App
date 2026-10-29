# PrivyChat Firebase Cloud Messaging (FCM) Integration

## 1. Role of Push Notifications in PrivyChat

Push notifications in PrivyChat serve as an asynchronous signaling mechanism rather than a message datastore:
- **Wakeup Signal for Offline Delivery**: Alerts the Android client to reconnect to the WebSocket to retrieve queued ciphertexts.
- **Incoming Call Notification**: High-priority full-screen intent wakeup for WebRTC voice and video calls.
- **Zero-Plaintext Notification Policy**: Push notifications NEVER contain message text, sender identity (unless enabled by user in privacy settings), attachment keys, or URLs. The payload contains only:
  ```json
  {
    "data": {
      "type": "NEW_MESSAGE",
      "conversationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "messageId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
      "timestamp": "1730200000000"
    }
  }
  ```

---

## 2. Firebase Project Configuration

1. Create a project in the [Firebase Console](https://console.firebase.google.com).
2. Register an Android Application:
   - Package name: `com.privychat.app`
   - Download `google-services.json` and place it in `android-app/app/`.
3. In Project Settings > Service Accounts:
   - Generate a new private key JSON for the **Firebase Admin SDK**.
   - Store it securely on the backend server (e.g. `/etc/privychat/firebase-adminsdk.json`).
4. Set backend environment variables:
   ```env
   FCM_ENABLED=true
   FCM_PROJECT_ID=privychat-app
   FCM_CREDENTIALS_PATH=/etc/privychat/firebase-adminsdk.json
   ```

---

## 3. Android Notification Channels

PrivyChat creates two distinct notification channels on Android 8.0+ (API 26+):

1. **`channel_calls`**:
   - Name: "Incoming Calls"
   - Importance: `IMPORTANCE_HIGH`
   - Custom ringtone URI with continuous vibration pattern.
   - Associated with `IncomingCallActivity` using `setFullScreenIntent(...)`.
2. **`channel_messages`**:
   - Name: "Messages"
   - Importance: `IMPORTANCE_DEFAULT`
   - Supports direct reply action and conversation grouping.

