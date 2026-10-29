# PrivyChat ProGuard / R8 Rules

# Preserve Cryptography & Bouncy Castle providers
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Preserve SQLCipher Native Libraries
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# Preserve WebRTC JNI & PeerConnection
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Preserve Room & SQLite
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Preserve Retrofit / Gson DTOs
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature
-keepattributes *Annotation*

