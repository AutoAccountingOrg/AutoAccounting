/*
 * 根项目如使用 Version Catalog，可保留 libs 引用。
 * 如果没有，则给插件写 version("…")，给依赖写完整坐标。
 */
plugins {
    `java-library`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm")    // 如果有 libs.plugins，可以写 alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {         // JavaExtension
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {       // KotlinJvmDsl
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dexlib2)      // Version Catalog 示例
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk17")
}

publishing {
    publications {
        // Kotlin DSL 用 register 而不是直接调用构造函数
        register<MavenPublication>("release") {
            groupId = "net.ankio.dex"
            artifactId = "DexLib"
            version = "1.0.0"

            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri(
                // 等价于 new File(System.getProperty("user.home"), ".m2/repository")
                File(System.getProperty("user.home"))
                    .resolve(".m2/repository")
            )
        }
    }
}
