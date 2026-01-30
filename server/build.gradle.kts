/* ---------- plugins ---------- */
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
}

/* ---------- Android config ---------- */
android {
    namespace = "org.ezbook.server"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = "21" }

    /* AGP 8.x 新写法：packaging → resources.excludes / jniLibs.excludes */
    packaging {
        resources.excludes += setOf(
            "META-INF/*"
        )
    }
}

/* ---------- repositories ---------- */
repositories {
    google() // Required for Android dependencies
    mavenCentral() // Required for KSP and other dependencies
    maven { url = uri("https://www.jitpack.io") }
}

/* ---------- dependencies ---------- */
dependencies {

    // Gson & OkHttp
    implementation(libs.gson)
    implementation(libs.okhttp)

    // Room with KSP (instead of KAPT)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler) // Use KSP instead of KAPT
    implementation(libs.androidx.room.ktx)

    // QuickJS
    implementation(libs.quickjs.android)

    // Ktor
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.gson)

    // Kotlin反射 - Ktor的reified泛型需要
    implementation(kotlin("reflect"))
}
