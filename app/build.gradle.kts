plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.materialThemeBuilder)
    alias(libs.plugins.autoresconfig)
}

autoResConfig {
    generateClass.set(true)
    generateRes.set(false)
    generatedClassFullName.set("org.ezbook.util.LangList")
    generatedArrayFirstItem.set("SYSTEM")
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Default" to "6750A4",
            "Red" to "F44336",
            "Pink" to "E91E63",
            "Purple" to "9C27B0",
            "DeepPurple" to "673AB7",
            "Indigo" to "3F51B5",
            "Blue" to "2196F3",
            "LightBlue" to "03A9F4",
            "Cyan" to "00BCD4",
            "Teal" to "009688",
            "Green" to "4FAF50",
            "LightGreen" to "8BC3A4",
            "Lime" to "CDDC39",
            "Yellow" to "FFEB3B",
            "Amber" to "FFC107",
            "Orange" to "FF9800",
            "DeepOrange" to "FF5722",
            "Brown" to "795548",
            "BlueGrey" to "607D8F",
            "Sakura" to "FF9CA8"
        )) {
            create("Material$name") {
                lightThemeFormat = "ThemeOverlay.Light.%s"
                darkThemeFormat = "ThemeOverlay.Dark.%s"
                primaryColor = "#$color"
            }
        }
    }
    // Add Material Design 3 color tokens (such as palettePrimary100) in generated theme
    // rikka.material >= 2.0.0 provides such attributes
    generatePalette = true
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
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
