package io.github.ismoy.belzspeedscan

import io.github.ismoy.belzspeedscan.domain.CodeScanner

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
    TODO("Not yet implemented")
}