pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    // 建议先抽一个统一变量，后面好维护
    val kotlinVersion = "2.1.20"

    plugins {
        // Kotlin 全家桶（含 android / jvm / kapt 等）都能复用这个号
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.android") version kotlinVersion

        // KSP 的前缀必须与 kotlinVersion 完全一致！
        id("com.google.devtools.ksp") version "${kotlinVersion}-2.0.0"
    }
}

plugins {
    // 这条原来就有，保持不变
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.xposed.info/") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AutoAccounting"
include(":app", ":dex", ":server")
