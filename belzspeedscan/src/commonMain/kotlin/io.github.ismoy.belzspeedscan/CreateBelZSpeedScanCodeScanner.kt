package io.github.ismoy.belzspeedscan

import io.github.ismoy.belzspeedscan.data.model.SecurityAlertInfo
import io.github.ismoy.belzspeedscan.domain.CodeScanner

expect fun createBelSpeedScanCodeScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    playSound: Boolean,
    resourceName:String,
    resourceExtension:String,
    delayToNextScan:Long = 2000,
    areaRatioThreshold:Float = 0.008f,
    onCodeScanned: (String) -> Unit,
    onSecurityAlert: (SecurityAlertInfo) -> Unit,
    onCameraError: ((String) -> Unit)? = null

): CodeScanner