plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.vibelock"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vibelock"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "WS_BASE_URL", "\"wss://your-server.com/ws\"")
        buildConfigField("String", "WS_DEV_URL", "\"ws://10.0.2.2:3001/ws\"")

        // AdMob 테스트 앱 ID (출시 시 실제 ID로 교체)
        buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
        buildConfigField("String", "AD_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
        buildConfigField("String", "AD_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        buildConfigField("String", "AD_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")

        // Play Billing 상품 ID
        buildConfigField("String", "SKU_PREMIUM_MONTHLY", "\"vibelock_premium_monthly\"")
        buildConfigField("String", "SKU_PREMIUM_YEARLY", "\"vibelock_premium_yearly\"")
        buildConfigField("String", "SKU_COINS_100", "\"vibelock_coins_100\"")
        buildConfigField("String", "SKU_COINS_500", "\"vibelock_coins_500\"")
        buildConfigField("String", "SKU_COINS_2000", "\"vibelock_coins_2000\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            buildConfigField("String", "WS_BASE_URL", "\"ws://10.0.2.2:3001/ws\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splash)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // Ktor WebSocket
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.content.negotiation)

    // Serialization
    implementation(libs.kotlinx.serialization)

    // DataStore
    implementation(libs.datastore.preferences)

    // ── 수익화 ──────────────────────────────────────────────────────
    // AdMob 광고
    implementation(libs.admob)

    // Google Play Billing (구독 + 인앱결제)
    implementation(libs.billing)

    // WorkManager (매일 자정 스킵 횟수 초기화)
    implementation(libs.work.runtime)
}
