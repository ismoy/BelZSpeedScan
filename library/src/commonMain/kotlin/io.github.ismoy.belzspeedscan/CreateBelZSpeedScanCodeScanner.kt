package io.github.ismoy.belzspeedscan

import io.github.ismoy.belzspeedscan.domain.CodeScanner

expect fun createBelSpeedScanCodeScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    isQRScanning:Boolean,
    playSound: Boolean,
    onCodeScanned: (String) -> Unit
): CodeScanner