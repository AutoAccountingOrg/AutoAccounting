plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    id("com.google.devtools.ksp")
    id("com.bugsnag.android.gradle")
}

android {
    namespace = "net.ankio.auto"
    compileSdk = 35


    defaultConfig {
        applicationId = "net.ankio.auto"
        minSdk = 29
        versionCode = 212
        versionName = "4.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        flavorDimensions += "version"
        setProperty("archivesBaseName", "app-${versionName}(${versionCode})")
        productFlavors {
            create("lsposed") {
                dimension = "version"
                applicationIdSuffix = ".xposed"
                versionNameSuffix = " - Xposed"
            }
            create("ocr") {
                dimension = "version"
                applicationIdSuffix = ".ocr"
                versionNameSuffix = " - Ocr"
            }
        }


        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        // 普通资源（非 .so）放这里
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/ASL2.0",
                "META-INF/gson/FieldAttributes.txt",
                "META-INF/gson/LongSerializationPolicy.txt",
                "META-INF/gson/annotations.txt",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }

        // 如果以后要过滤 .so，改用 jniLibs.excludes += "lib/**/foo.so"
        // jniLibs { excludes += "lib/**/yourNative.so" }
    }






    androidResources {
        additionalParameters.addAll(
            listOf("--allow-reserved-package-id", "--package-id", "0x65")
        )
    }

}


dependencies {
    implementation(libs.androidx.swiperefreshlayout)


    // 打包依赖
    implementation(libs.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // 主题库
    implementation(libs.themeEngine)

    // Html转换
    implementation(libs.html.ktx)

    // gson
    implementation(libs.gson)

    // toast
    implementation(libs.toaster)

    // xp依赖
    compileOnly(libs.xposed)

    // flexbox
    implementation(libs.flexbox)

    // 圆角
    implementation(libs.round)

    // bug
    implementation(libs.bugsnag.android)

    // okhttp
    implementation(libs.okhttp)

    // Dex工具
    implementation(project(":dex"))

    // xml2json
    implementation(libs.xmltojson)

    implementation(project(":server"))
    implementation(libs.markdownj.core)

    // debug依赖
    debugImplementation(libs.leakcanary.android)
}
