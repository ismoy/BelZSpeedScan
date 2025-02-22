@file:Suppress("DEPRECATION")

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("maven-publish")
    //`maven-publish`
}
/*group = "io.github.ismoy"
version = "1.0.3"*/


kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    targetHierarchy.default()
    androidTarget {
        publishLibraryVariants("release")
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "belzspeedscan"
            isStatic = true
        }
        target.mavenPublication {
            // Configuración específica si es necesario
        }
    }
    withSourcesJar(true)
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
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.camera.core)
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.view)
                implementation(libs.accompanist.permissions)
                implementation(libs.zxing.android.embedded)
                implementation(libs.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.tooling.preview)
                implementation(libs.barcode.scanning)
                implementation(libs.startup.runtime)
                implementation(libs.browser)
            }
        }
        val iosResourcesDir =
            project.findProperty("iosResourcesDir") as? String ?: "src/iosMain/resources"
        iosMain {
            resources.srcDirs(iosResourcesDir)
            tasks.withType<ProcessResources> {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
        }
        all {
            languageSettings {
                optIn("kotlin.ExperimentalMultiplatform")
                optIn("kotlin.ExperimentalUnsignedTypes")
            }
        }
    }
    metadata {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs += "-Xexport-kdoc"
            }
        }
    }

}


android {
    namespace = "io.github.ismoy"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}
/*publishing{
    repositories{
        mavenLocal()
    }
}*/
mavenPublishing{
    coordinates(
        groupId = "io.github.ismoy",
        artifactId = "belzspeedscan",
        version = "1.0.8"
    )
    pom {
        name.set("BelZSpeedScan")
        description.set("BelZSpeedScan is a comprehensive multiplatform barcode and QR code scanning" +
                " library for Android and iOS. It features optimized camera handling, automatic " +
                "distance adjustment, support for codes with reflective surfaces, and customizable " +
                "UI components with Compose. Easily integrate high-performance code scanning into " +
                "your Kotlin Multiplatform, Android, or iOS applications with a unified API.")
        inceptionYear.set("2025")
        url.set("https://github.com/ismoy/DemoBelZSpeedScan")
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("https://github.com/ismoy/DemoBelZSpeedScan/blob/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("ismoy")
                name.set("Ismoy Belizaire")
                email.set("belizairesmoy72@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/ismoy/BelZSpeedScan")
        }
    }
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
afterEvaluate {
    publishing {
        publications.forEach { publication ->
            val mavenPublication = publication as? MavenPublication
            if (mavenPublication != null) {
                // Solo modifica kotlinMultiplatform para tener el artifactId base
                if (mavenPublication.name == "kotlinMultiplatform") {
                    mavenPublication.artifactId = "belzspeedscan"
                } else {
                    // Mantén los sufijos específicos para cada plataforma
                    // Estos serán como 'belzspeedscan-iosarm64', 'belzspeedscan-android', etc.
                    println("Leaving platform-specific artifactId: ${mavenPublication.artifactId}")
                }

                println("Configured publication: ${mavenPublication.name}, artifactId: ${mavenPublication.artifactId}")
            }
        }
    }
}