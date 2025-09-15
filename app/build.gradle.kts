plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    ////6.11김봉국 수정//
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" // 코틀린 버전에 맞는 ksp 버전 사용
// //6.11김봉국 수정//
}

android {
    namespace = "com.example.dpa"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dpa"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
    composeOptions {
        // Compose Compiler 버전은 보통 compose.bom을 통해 관리됩니다.
        // 특정 버전이 필요할 경우 여기에 명시할 수 있습니다.
    }
}

dependencies {
    // --- 코어 & 생명주기 라이브러리 ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0") // ViewModel 용
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")  // collectAsStateWithLifecycle 용

    // --- Jetpack Compose UI ---
    implementation(platform(libs.androidx.compose.bom)) // Compose BOM
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // --- 내비게이션 ---
    implementation("androidx.navigation:navigation-compose:2.7.0")

    // --- 데이터 저장 ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- CameraX (BOM을 사용하여 버전 관리) ---
    val cameraxVersion = "1.5.0-beta01" // 예시: 최신 버전은 [1] 링크 확인

    implementation("androidx.camera:camera-core:${cameraxVersion}")
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("androidx.camera:camera-extensions:${cameraxVersion}")



    // --- MediaPipe ---
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // --- 권한 요청 (Accompanist) ---
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    implementation(libs.androidx.camera.compose)

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0") // 사용 가능한 최신 버전으로 업데이트하세요

    // --- 테스트 라이브러리 ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    ////6.11김봉국 수정//
    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Chart Library (Vico)
    implementation("com.patrykandpatrick.vico:compose:1.14.0")
    ////6.12김봉국 수정//
    implementation("io.github.boguszpawlowski.composecalendar:composecalendar:1.4.0")

}