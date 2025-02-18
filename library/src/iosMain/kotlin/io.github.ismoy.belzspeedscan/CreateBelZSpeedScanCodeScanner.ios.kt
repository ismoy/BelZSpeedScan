package io.github.ismoy.belzspeedscan

import IOSScanner
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import platform.UIKit.UIView

actual fun createBelSpeedScanCodeScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    isQRScanning: Boolean,
    playSound: Boolean,
    resourceName:String,
    resourceExtension:String,
    onCodeScanned: (String) -> Unit
): CodeScanner {
    return IOSScanner(previewView as UIView, isQRScanning, onCodeScanned,playSound,resourceName,resourceExtension)
}