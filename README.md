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

## 📂 Project Files

- [📘 README](./README.md)
- [📄 License (MIT)](./LICENSE)
- [🧭 Roadmap](./ROADMAP.md)
- [🤝 Contributing Guide](./CONTRIBUTING.md)

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
        implementation("io.github.ismoy:belzspeedscan:1.0.11") // Replace with the actual version
    }
    // ... other dependencies
}
```
## Android Native
For native Android development, include the dependency in your module's build.gradle file:
```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.ismoy:belzspeedscan:1.0.11") // Replace with the actual version
    // ... other dependencies
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
- Email: belizairesmoy72@gmail.com

## ⭐ Show your support

Give a ⭐️ if this project helped you!

# Use in your KMP/CMP
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
### Custom Request Camera Permission
```kotlin
  RequestCameraPermission(
        titleDialogConfig = "Your app needs camera permission",
        descriptionDialogConfig = "Your app needs camera permission to scan QR codes",
        btnDialogConfig = "Open Settings",
        titleDialogDenied = "Camera permission denied",
        descriptionDialogDenied = "You need to grant camera permission to scan QR codes",
        btnDialogDenied = "Grant Permission",
        customDeniedDialog = {
            //Your compose custom dialog
        },
        customSettingsDialog = {
            //Your compose custom dialog
        }
    ) {granted-> 
        println("CameraManagerUtils: $granted")
    }

```
# Use in your Android Native App
### Create CameraManager function
```kotlin
  @Composable
fun CameraManagerUtils(
    onCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
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
                                    resourceName = "sounds",
                                    resourceExtension = "mp3",
                                    delayToNextScan = 1000,
                                    onCodeScanned = { scannedText ->
                                        onCodeScanned(scannedText)
                                    }
                                ).also {
                                    it.startScanning()
                                }
                            },
                            scanner = currentScanner,
                            modifier = Modifier.fillMaxHeight(1F),
                            waterMark = "",
                            tooFarText = "",
                            tooOptimalText = "",
                        )
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
 fun CameraScreen() {
  CameraManagerUtils() { codeScanned ->
      // Scan Result                  
}
}

```
## Camera Permissions

BelZSpeedScan handles camera permissions automatically on Android. However, on iOS, you need to add the camera usage description to your `Info.plist` file.

### iOS Permission
If you don't have an `Info.plist` file, you need to create it. Then, go to `composeApp/iosMain/iosApp/iosApp/Info.plist` and add the following:
```xml
<key>NSCameraUsageDescription</key>
<string>We need access to the camera to scan QR codes and barcodes.</string>
```
### Add your Scanner Sound
If you need emit a sound when the code scan., go to `composeApp/iosMain/resources/beep.mp3` remember respect this exactly name resources/beep.mp3

## Explanation:
1. **Imports:** Import necessary classes, including CodeScanner from the library.
2. **Function:** This function serves as the entry point for using the scanner. The context parameter is crucial for Android integration.
3. ## State Variables:
• **scannedCode**: Stores the result of the scan.  
• **hasCameraPermission**: Tracks whether the user has granted camera permission.  
• **scanner**: Holds an instance of the CodeScanner.

4. **RequestCameraPermission:** This function handles requesting camera permissions. The default implementation provides a standard dialog. You can customize the dialogs by using the customDeniedDialog and customSettingsDialog parameters. The onResult lambda provides a boolean indicating whether permission was granted.
5. **Conditional Rendering:** The if (hasCameraPermission) block ensures that the camera preview and scanner are only initialized if permission has been granted.
6. **CameraPreview:** This composable displays the camera preview. The onPreviewViewReady lambda is called when the PreviewView is ready, allowing you to initialize the CodeScanner.
7. **createBelSpeedScanCodeScanner:** This function creates an instance of the CodeScanner. It takes the context, LifecycleOwner, PreviewView, isQRScanning (true for QR code scanning only, false for barcode scanning only. No default value), playSound, and a lambda for handling the scanned code.
8. **startScanning():** Starts the scanning process.
9. **Scanned Code Handling:** The lambda passed to createBelSpeedScanCodeScanner is called when a code is scanned. The scannedCode parameter contains the scanned data.

## Native iOS Support

Good news! Native iOS support is on the way. We're working hard to bring the functionality of BelZSpeedScan to the iOS platform, allowing you to use the same scanning logic in your iOS applications. Stay tuned for future updates and announcements regarding the availability of iOS support.
## This detailed explanation should help you integrate BelZSpeedScan into your KMP and Android projects effectively. Remember to replace placeholder version numbers and adapt the code to your specific UI and application logic.
