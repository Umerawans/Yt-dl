plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.ytdlpapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.ytdlpapp"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    // Required so the bundled python/ffmpeg native libs can be extracted and executed
    packaging {
        jniLibs { useLegacyPackaging = true }
    }
}

dependencies {
    val ytdlp = "0.17.2" // check https://github.com/yausername/youtubedl-android for the latest
    implementation("com.github.yausername.youtubedl-android:library:$ytdlp")
    implementation("com.github.yausername.youtubedl-android:ffmpeg:$ytdlp")

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
