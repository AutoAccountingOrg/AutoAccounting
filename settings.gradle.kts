pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.xposed.info/") }
        maven { url = uri("https://jitpack.io") }
    }
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
}

rootProject.name = "AutoAccounting"
include(":app", ":dex", ":server")
include(":shell")
include(":ocr")
include(":test")
