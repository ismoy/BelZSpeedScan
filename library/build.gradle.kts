import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = "com.github.ismoy"
version = "1.0.0.6"

kotlin {
    jvm("desktop")
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.compose.runtime)
                implementation(libs.compose.ui)
                implementation(libs.compose.animation)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation (libs.androidx.camera.core)
                implementation (libs.androidx.camera.camera2)
                implementation (libs.androidx.camera.lifecycle)
                implementation (libs.androidx.camera.view)
                implementation (libs.accompanist.permissions)
                implementation(libs.zxing.android.embedded)
                implementation(libs.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.tooling.preview)
                implementation (libs.barcode.scanning)
            }
        }
        val iosResourcesDir = project.findProperty("iosResourcesDir") as? String ?: "src/iosMain/resources"
        iosMain{
            resources.srcDirs(iosResourcesDir)
            tasks.withType<ProcessResources> {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
        val desktopMain by getting {

        }
    }
    publishing {
        publications {
            register<MavenPublication>("multiplatform") {
                groupId = "com.github.ismoy"
                artifactId = "BelZSpeedScan"
                version = "1.0.0.6"
                from(components["kotlin"])
            }
        }
    }
}


android {
    namespace = "com.github.ismoy.BelZSpeedScan"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildTypes{
        release {  }
        debug {  }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

}

