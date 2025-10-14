plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "net.ankio.ocr"
    compileSdk = 35

    defaultConfig {
        minSdk = 29

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // 只保留 arm64-v8a 架构的 native library，减小体积
        ndk {
            abiFilters.add("arm64-v8a")
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

}

dependencies {
    // AAR library 不能直接 implementation 本地 .aar，使用 compileOnly
    // 运行时依赖由 app 模块提供
    compileOnly(files("libs/OcrLibrary-1.3.0-release.aar"))
    implementation(libs.core.ktx)
}
