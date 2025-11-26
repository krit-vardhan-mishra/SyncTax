import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    id("com.google.devtools.ksp")
    id("com.chaquo.python") version "16.1.0"
    id ("kotlin-parcelize")
}

android {
    namespace = "com.just_for_fun.synctax"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.just_for_fun.synctax"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load API keys from local.properties (not committed to git)
        val localProperties = project.rootProject.file("local.properties")
        val properties = Properties()
        if (localProperties.exists()) {
            properties.load(FileInputStream(localProperties))
        }
        
        // Add API keys to BuildConfig (default to empty strings if not found)
        buildConfigField("String", "MUSIC_API_KEY", "\"${properties.getProperty("MUSIC_API_KEY") ?: ""}\"")
        buildConfigField("String", "PLAYER_API_KEY", "\"${properties.getProperty("PLAYER_API_KEY") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_API_KEY", "\"${properties.getProperty("YOUTUBE_API_KEY") ?: ""}\"")
        buildConfigField("String", "YOUTUBE_CLIENT_ID", "\"${properties.getProperty("YOUTUBE_CLIENT_ID") ?: ""}\"")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }
}

chaquopy {
    defaultConfig {
        version = "3.10"
        pip {
<<<<<<< HEAD
            install("yt-dlp")
=======
            install("yt-dlp==2025.11.12")
            install("ffmpeg")
            install("ffprobe")
            install("pillow")
>>>>>>> 82345c57d1908bd520574ec613b639ffa202a238
            install("mutagen")
            install("requests") // Add requests library
            install("urllib3")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Palette for extracting colors from images
    implementation("androidx.palette:palette-ktx:1.0.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // kotlinx.serialization for JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Media Player
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("androidx.media:media:1.6.0")

    // AndroidX Startup (explicit to ensure compatible version and resources)
    implementation("androidx.startup:startup-runtime:1.2.0")


    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // NewPipe extractor - used to decode signatureCipher-obfuscated stream URLs
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.8")

    // Progress bar dependency
    implementation("ir.mahozad.multiplatform:wavy-slider:2.2.0")

    // Retrofit for API calls (LRCLIB lyrics API)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Liquid library for shader effects
    implementation("io.github.fletchmckee.liquid:liquid:1.0.1")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")

    // Worker
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // FFmpeg for audio processing
    // implementation("com.github.arthenica:ffmpeg-kit:v6.0-2")
<<<<<<< HEAD
=======
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
//implementation "io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1"
    implementation("io.github.junkfood02.youtubedl-android:aria2c:0.18.1")
//    implementation "io.github.junkfood02.youtubedl-android:library:0.17.4"
>>>>>>> 82345c57d1908bd520574ec613b639ffa202a238
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")    // Commented out due to jitpack.io 401 error - app handles FFmpeg absence gracefully
    // implementation("com.github.arthenica:ffmpeg-kit-full:4.5.1-1")

    implementation("com.afollestad.material-dialogs:core:3.3.0")
}