plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.privychat.app"
    compileSdk = 34

    val awsBaseUrl = project.findProperty("awsBaseUrl") as? String
        ?: "https://d3fv2w7uf8w2u4.cloudfront.net/api/v1/"
    val awsWsUrl = project.findProperty("awsWsUrl") as? String
        ?: "wss://d3fv2w7uf8w2u4.cloudfront.net/ws"
    val localBaseUrl = project.findProperty("localBaseUrl") as? String
        ?: "http://10.0.2.2:8080/api/v1/"
    val localWsUrl = project.findProperty("localWsUrl") as? String
        ?: "ws://10.0.2.2:8080/ws"

    defaultConfig {
        applicationId = "com.privychat.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "STUN_SERVER", "\"stun:stun.l.google.com:19302\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"$localBaseUrl\"")
            buildConfigField("String", "WS_URL", "\"$localWsUrl\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"$awsBaseUrl\"")
            buildConfigField("String", "WS_URL", "\"$awsWsUrl\"")
        }
    }

    signingConfigs {
        create("release") {
            val envKeystorePath = System.getenv("KEYSTORE_PATH") ?: "release.keystore"
            val envKeystorePass = System.getenv("KEYSTORE_PASSWORD") ?: "privychat_password"
            val envKeyAlias = System.getenv("KEY_ALIAS") ?: "privychat"
            val envKeyPass = System.getenv("KEY_PASSWORD") ?: "privychat_password"

            storeFile = file(envKeystorePath)
            storePassword = envKeystorePass
            this.keyAlias = envKeyAlias
            this.keyPassword = envKeyPass
            enableV3Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room & Encrypted Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Cryptography (Bouncy Castle for Curve25519 & Ed25519)
    implementation(libs.bouncycastle.prov)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // WebRTC Calling
    implementation(libs.webrtc)

    // Push Notifications
    implementation(libs.firebase.messaging)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Coil Image Loading
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register("testDebugUnitTest") {
    description = "Runs unit tests for both devDebug and prodDebug variants"
    group = "verification"
    dependsOn("testDevDebugUnitTest", "testProdDebugUnitTest")
}

