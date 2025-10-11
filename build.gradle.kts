// Top-level build file where you can add configuration options common to all sub-projects/modules.


plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.materialThemeBuilder)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.autoresconfig) apply false
    alias(libs.plugins.kotlinJvm) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}