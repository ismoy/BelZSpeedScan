package io.github.ismoy.belzspeedscan

import IOSScanner
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import platform.UIKit.UIView

actual fun createBelSpeedScanCodeScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    playSound: Boolean,
    resourceName:String,
    resourceExtension:String,
    delayToNextScan:Long,
    areaRatioThreshold:Float,
    onCodeScanned: (String) -> Unit,
): CodeScanner {
    return IOSScanner(previewView as UIView,playSound,resourceName,resourceExtension,delayToNextScan,onCodeScanned)
}