import java.util.Calendar

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
    generatedClassFullName.set("net.ankio.auto.util.LangList")
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
        versionCode = calculateVersionCode()
        versionName = "4.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        flavorDimensions += "version"
        setProperty("archivesBaseName", "app-${versionName}(${versionCode})")



        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
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
                "META-INF/*"
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

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

}
fun calculateVersionCode(): Int {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return year * 10000 + month * 100 + day
}

configurations.configureEach {
    exclude("androidx.appcompat", "appcompat")
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

    // Html转换
    implementation(libs.html.ktx)

    // gson
    implementation(libs.gson)

    // toast
    implementation(libs.toaster)
    implementation(libs.preferenceKtx)
    implementation(libs.androidx.lifecycle.service)

    // xp依赖
    compileOnly(libs.xposed)

    // flexbox
    implementation(libs.flexbox)

    // 圆角
    implementation(libs.round)


    // okhttp
    implementation(libs.okhttp)

    // Dex工具
    implementation(project(":dex"))

    // xml2json
    implementation(libs.xmltojson)

    implementation(project(":server"))
    implementation(project(":shell"))
    implementation(project(":ocr"))
    // ocr 模块的运行时依赖
    implementation(files("ocr/libs/OcrLibrary-1.3.0-release.aar"))

    // debug依赖
    debugImplementation(libs.leakcanary.android)



    implementation(libs.rikkaMaterial)
    implementation(libs.rikkaMaterialPreference)
    implementation(libs.about)

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.tencent.bugly:crashreport:latest.release")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation(kotlin("reflect"))

    // Sora Editor - 代码编辑器
    val editorVersion = "0.23.7"
    implementation("io.github.rosemoe:editor:$editorVersion")
    implementation("io.github.rosemoe:language-textmate:$editorVersion")

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))


}
