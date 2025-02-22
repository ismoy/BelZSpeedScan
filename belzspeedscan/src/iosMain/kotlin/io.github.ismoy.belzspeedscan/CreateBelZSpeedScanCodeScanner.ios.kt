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
    onCodeScanned: (String) -> Unit,
    delayToNextScan:Long
): CodeScanner {
    return IOSScanner(previewView as UIView, isQRScanning,playSound,resourceName,resourceExtension,onCodeScanned,delayToNextScan)
}