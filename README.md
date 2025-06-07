# [BelZSpeedScan](https://github.com/ismoy/BelZSpeedScan)
### A Cross-Platform QR Code and Barcode Scanning Library, using MLKIT for decoding.

[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ismoy/kmpswipe.svg)](https://search.maven.org/artifact/io.github.ismoy/kmpswipe)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.5.0-green.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android%20|%20iOS-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![KMP](https://img.shields.io/badge/KMP-Kotlin%20Multiplatform-7F52FF.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Swipe](https://img.shields.io/badge/UI-Swipe%20Gestures-red.svg)](https://github.com/ismoy/kmpswipe)
[![UX](https://img.shields.io/badge/UX-Haptic%20Feedback-blueviolet.svg)](https://github.com/ismoy/kmpswipe)
![Issues](https://img.shields.io/github/issues/ismoy/BelZSpeedScan)

BelZSpeedScan is a lightweight and easy-to-use library for scanning QR codes and barcodes. It supports both Kotlin Multiplatform (KMP) and native Android development, providing a consistent API across platforms. This allows you to use the same scanning logic in your shared KMP code and seamlessly integrate it into your Android application.

## 🌟 Features

- 📱 Cross-platform support (Android & iOS)
- 🚀 High-performance scanning using MLKit
- 🎨 Customizable UI components
- 🔒 Security alerts for suspicious codes
- 🔊 Audio feedback on successful scans
- 🎯 Multiple barcode formats support
- ⚡ Real-time scanning
- 🛠️ Easy integration with KMP projects

## 📱 Demonstrations

| Android | iOS |
|---------|-----|
| ![Android](https://github.com/ismoy/DemoBelzSpeedScan/blob/main/demoCamera_compressed.gif) | ![Demo](https://github.com/ismoy/BelZSpeedScan/blob/main/images/iosDemo%20(1).gif) |

## 🚀 Quick Start

### Kotlin Multiplatform (KMP)

Add the BelZSpeedScan dependency to your `commonMain` source set in your project's `build.gradle` file:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    commonMain {
        implementation("io.github.ismoy:belzspeedscan:1.0.11")
    }
}
```

### Android Native

For native Android development, include the dependency in your module's build.gradle file:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.ismoy:belzspeedscan:1.0.11")
}
```

## 📖 Documentation

For detailed documentation and examples, please visit our [documentation site](https://github.com/ismoy/BelZSpeedScan/wiki).

## 🤝 Contributing

We love your input! We want to make contributing to BelZSpeedScan as easy and transparent as possible, whether it's:

- Reporting a bug
- Discussing the current state of the code
- Submitting a fix
- Proposing new features
- Becoming a maintainer

Please read our [Contributing Guide](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## 📋 Roadmap

Check out our [Roadmap](ROADMAP.md) to see what's coming next and how you can help!

## 🐛 Known Issues

Please report any bugs or issues you find in the [issues section](https://github.com/ismoy/BelZSpeedScan/issues).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Thanks to all our contributors
- Special thanks to the MLKit team for their amazing work
- The Kotlin Multiplatform community for their support

## 📞 Contact

- GitHub Issues: [Create an issue](https://github.com/ismoy/BelZSpeedScan/issues)
- Email: [Your email]
- Twitter: [@YourTwitter]

## ⭐ Show your support

Give a ⭐️ if this project helped you!

# Use in your KMP
#### App.kt
```kotlin
import io.github.ismoy.belzspeedscan.domain.CodeScanner // Import CodeScanner
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
fun App(context: Any? = null) {
    CameraScreen(context)
}
```
### composeApp/src/androidMain/MainActivity.kt
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(this)
        }
    }
}
```
### composeApp/src/iosApp/iosApp/Info.plist
```xml
<key>NSCameraUsageDescription</key>
<string>Necesitamos acceso a la cámara para escanear códigos QR y códigos de barras</string>
<key>UIBackgroundModes</key>

<array>
<string>audio</string>
</array>
```
### Create this resource composeApp/src/iosMain/resources/beep.mp3
### Create CameraManager function
```kotlin
@Composable
fun CameraManagerUtils(
    context: Any?,
    onCodeScanned: (String) -> Unit
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    val scanner: CodeScanner? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        onDispose {
            scanner?.stopScanning()
        }
    }

    RequestCameraPermission { granted ->
        hasCameraPermission = granted
    }
    var securityAlertVisible by remember { mutableStateOf(false) }
    var securityAlertMessage by remember { mutableStateOf("") }
    Scaffold(
        content = { innerPadding ->
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    Box {
                        var currentScanner by remember { mutableStateOf<CodeScanner?>(null) }
                        CameraPreview(
                            onPreviewViewReady = { preview ->
                                currentScanner = createBelSpeedScanCodeScanner(
                                    context = context,
                                    lifecycleOwner = lifecycleOwner,
                                    previewView = preview,
                                    playSound = true,
                                    resourceName = "beep",
                                    resourceExtension = "mp3",
                                    delayToNextScan = 3000,
                                    onCodeScanned = { scannedText ->
                                        onCodeScanned(scannedText)
                                    },
                                    onSecurityAlert = {securityAlertInfo->
                                        securityAlertMessage = "${securityAlertInfo.message}\n${securityAlertInfo.codeValue}\nRazón: ${securityAlertInfo.reason}"
                                        securityAlertVisible = true
                                    }
                                ).also {
                                    it.startScanning()
                                }
                            },
                            scanner = currentScanner,
                            modifier = Modifier.fillMaxHeight(1F),
                        )
                    }
                    if (securityAlertVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomTooltip(
                                icon = Icons.Filled.Warning,
                                text = securityAlertMessage,
                                bottomImage = HorizontalLinePainter(),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                            )
                        }
                    }
                    GlobalScope.launch {
                        delay(2000)
                        securityAlertVisible = false
                    }



                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxHeight(1F)
                        .padding(innerPadding)
                        .background(Color.Black.copy(alpha = 0.8f))
                )
            }
        }
    )
}
```
### Create a Camera Screen
```kotlin
 fun CameraScreen(context: Any?) {
    CameraManagerUtils(context) { codeScanned ->
        // Scan result    
    }
}

```
### Default Request Camera Permission
```kotlin
  RequestCameraPermission { granted ->
    hasCameraPermission = granted
}

```