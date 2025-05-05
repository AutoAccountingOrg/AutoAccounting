/*
 * 根项目如使用 Version Catalog，可保留 libs 引用。
 * 如果没有，则给插件写 version("…")，给依赖写完整坐标。
 */
plugins {
    `java-library`
    //alias(libs.plugins.kotlinJvm)   apply false // 如果有 libs.plugins，可以写 alias(libs.plugins.jetbrains.kotlin.jvm)
    id(libs.plugins.kotlinJvm.get().pluginId)
}

java {         // JavaExtension
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {       // KotlinJvmDsl
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dexlib2)
}

