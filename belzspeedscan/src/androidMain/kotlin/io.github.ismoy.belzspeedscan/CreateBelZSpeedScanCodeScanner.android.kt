package io.github.ismoy.belzspeedscan

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import io.github.ismoy.belzspeedscan.domain.CodeScanner

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
    return AndroidScanner(context as Context, lifecycleOwner as LifecycleOwner,
        previewView as PreviewView,isQRScanning,onCodeScanned,playSound,delayToNextScan)
}