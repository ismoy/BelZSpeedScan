pluginManagement {
    repositories {
       // mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
       // maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        //mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "BelZSpeedScan"
include(":library")
