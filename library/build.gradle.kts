plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = "com.github.ismoy"
version = "1.0.1.3"

kotlin {
    androidTarget()
    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "BelZSpeedScan"
            isStatic = true
            linkerOpts += "-Xbundle-id=com.github.ismoy.BelZSpeedScan"
        }
    }

    applyDefaultHierarchyTemplate()

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
            create<MavenPublication>("kmm") {
                from(components["kotlin"])
                groupId = "com.github.ismoy"
                artifactId = "BelZSpeedScan"
                version = "1.0.1.3"

                // Esto es importante para evitar artefactos adicionales
                pom {
                    name.set("BelZSpeedScan")
                    description.set("Kotlin Multiplatform Library")
                }
            }
        }
    }
}


android {
    namespace = "com.github.ismoy.BelZSpeedScan"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    buildTypes {
        release { }
        debug { }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

}
tasks.withType<AbstractPublishToMaven>().configureEach {
    enabled = publication?.name == "kmm"
}