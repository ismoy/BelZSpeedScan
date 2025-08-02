package io.github.ismoy.belzspeedscan

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.domain.ScannerEvent
import io.github.ismoy.belzspeedscan.domain.ScannerEventListener
import io.github.ismoy.belzspeedscan.domain.ScannerEventManager
import io.github.ismoy.belzspeedscan.state.DefaultScannerStateManager

actual fun createBelSpeedScanCodeScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    config: ScannerConfig,
    eventManager: ScannerEventManager,
    stateManager: DefaultScannerStateManager
): CodeScanner {
    return AndroidScanner(
        context = context as Context,
        lifecycleOwner = lifecycleOwner as LifecycleOwner,
        previewView = previewView as PreviewView,
        config = config,
        eventManager = eventManager,
        stateManager = stateManager
    )
}

@Deprecated(
    message = "This API has been deprecated in favor of the new BelZSpeedScanner composable and ScannerBuilder pattern. " +
            "The new API provides better configuration management, event handling, security analysis, and state management.",
    replaceWith = ReplaceWith(
        ""
    ),
    level = DeprecationLevel.WARNING
)
actual fun createBelSpeedScanCodeScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    playSound: Boolean,
    resourceName: String,
    resourceExtension: String,
    delayToNextScan: Long,
    areaRatioThreshold: Float,
    onCodeScanned: (String) -> Unit,
    onCameraError: ((String) -> Unit)?
): CodeScanner {
    val config = ScannerConfig(
        playSound = playSound,
        soundResourceName = resourceName,
        soundResourceExtension = resourceExtension,
        delayToNextScan = delayToNextScan,
        areaRatioThreshold = areaRatioThreshold
    )

    val eventManager = ScannerEventManager()
    val stateManager = DefaultScannerStateManager()

    eventManager.addListener(object : ScannerEventListener {
        override fun onEvent(event: ScannerEvent) {
            when (event) {
                is ScannerEvent.CodeScanned -> onCodeScanned(event.code)
                is ScannerEvent.CameraError -> onCameraError?.invoke(event.error)
                else -> {}
            }
        }
    })

    return createBelSpeedScanCodeScanner(
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        config = config,
        eventManager = eventManager,
        stateManager = stateManager
    )
}
