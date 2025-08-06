### BelZSpeedScan — A Cross-Platform QR & Barcode Scanner for Kotlin Multiplatform (Android & iOS)

[![Contributions welcome](https://img.shields.io/badge/contributions-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.ismoy/belzspeedscan.svg)](https://search.maven.org/artifact/io.github.ismoy/belzspeedscan)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg)](https://android-arsenal.com/api?level=21)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-1.8.0-green.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Platform](https://img.shields.io/badge/Platform-Android%20|%20iOS-orange.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![KMP](https://img.shields.io/badge/KMP-Kotlin%20Multiplatform-7F52FF.svg)](https://kotlinlang.org/docs/multiplatform.html)
![Issues](https://img.shields.io/github/issues/ismoy/BelZSpeedScan)

BelZSpeedScan is a lightweight and easy-to-use library for scanning QR codes and barcodes. It supports both Kotlin Multiplatform (KMP) and native Android development, providing a consistent API across platforms. This allows you to use the same scanning logic in your shared KMP code and seamlessly integrate it into your Android application.

## 📂 Project Files
- [📄 License (MIT)](./LICENSE)
- [🤝 Contributing Guide](./CONTRIBUTING.md)

## 📱 Demonstrations
| Android | iOS |
|---------|-----|
| ![Android](https://github.com/ismoy/DemoBelzSpeedScan/blob/main/demoCamera_compressed.gif) | ![Demo](https://github.com/ismoy/BelZSpeedScan/blob/main/images/iosDemo%20(1).gif) |

## Installation:

### Using BelZSpeedScan in Kotlin Multiplatform / Compose Multiplatform

## Step 1: Add the dependency
```gradle
In your commonMain build.gradle.kts:
```
```gradle
implementation("io.github.ismoy:belzspeedscan:1.0.12")
```
Don’t forget to configure iOS-specific permissions in your Info.plist file:
```gradle
<key>NSCameraUsageDescription</key>
<string>We need access to the camera to scan QR.</string>
```
## Step 2: Launch the Scanner
```kotlin
 var showScanner by remember { mutableStateOf(false) }
```
```
if (showScanner) {
    BelZSpeedScanner(
     onCodeScanned = { code ->}//result scan   
   )
}

Button(onClick = { showScanner = true }) {
    Text("Scan")
}
```
Using BelZSpeedScan in Android Native (Jetpack Compose)
## Step 1: Add the dependency
```gradlew
implementation("io.github.ismoy:belzspeedscan:1.0.12")
```
## Step 2: Launch the Scanner
```
var showScanner by remember { mutableStateOf(false) }
```
```
if (showScanner) {
    BelZSpeedScanner(
     onCodeScanned = { code ->}//result scan   
   )
}

Button(onClick = { showScanner = true }) {
    Text("Scan")
}
```
### Analytics Integration

BelZSpeedScan includes an optional analytics system designed with privacy in mind:

- **Privacy-focused**: No personal data is collected
- **Configurable**: Granular control over what data is collected
- **Multi-platform**: Works on both Android and iOS
- **Custom Callbacks**: Support for custom analytics implementations

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
