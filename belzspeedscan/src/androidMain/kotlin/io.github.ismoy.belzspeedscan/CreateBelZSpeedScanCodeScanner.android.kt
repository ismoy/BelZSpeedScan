package io.github.ismoy.belzspeedscan

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import io.github.ismoy.belzspeedscan.domain.CodeScanner

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
    return AndroidScanner(context as Context, lifecycleOwner as LifecycleOwner,
        previewView as PreviewView,onCodeScanned,playSound,delayToNextScan,areaRatioThreshold)
}