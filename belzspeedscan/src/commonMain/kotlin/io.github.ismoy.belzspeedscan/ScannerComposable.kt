package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.domain.ScannerEvent
import io.github.ismoy.belzspeedscan.domain.ScannerEventListener
import io.github.ismoy.belzspeedscan.domain.ScannerEventManager
import io.github.ismoy.belzspeedscan.state.DefaultScannerStateManager
import io.github.ismoy.belzspeedscan.state.ScannerState

@Composable
fun BelZSpeedScanner(
    modifier: Modifier = Modifier,
    config: ScannerConfig = ScannerConfig.default(),
    onCodeScanned: (String) -> Unit = {},
    onCameraError: (String) -> Unit = {},
    onPermissionResult: (Boolean) -> Unit = {},
    onPermissionPermanentlyDenied: () -> Unit = {},
    onStateChanged: (ScannerState) -> Unit = {},
    customOverlay: @Composable (() -> Unit)? = null,
    customPermissionDialog: @Composable (() -> Unit)? = null
) {
    var scanner by remember { mutableStateOf<CodeScanner?>(null) }

    val eventManager = remember { ScannerEventManager() }
    val stateManager = remember { DefaultScannerStateManager() }

    val context = getLocalContext()
    val lifecycleOwner = getLocalLifecycleOwner()
    
    val eventListener = remember {
        object : ScannerEventListener {
            override fun onEvent(event: ScannerEvent) {
                when (event) {
                    is ScannerEvent.CodeScanned -> onCodeScanned(event.code)
                    is ScannerEvent.CameraError -> onCameraError(event.error)
                    is ScannerEvent.PermissionResult -> onPermissionResult(event.granted)
                    is ScannerEvent.PermissionPermanentlyDenied -> onPermissionPermanentlyDenied()
                    is ScannerEvent.DistanceChanged -> stateManager.updateDistance(event.distance)
                    is ScannerEvent.FlashStateChanged -> { /* Flash state handled by UI */ }
                    is ScannerEvent.TorchAvailabilityChanged -> { /* Torch availability handled by UI */ }
                    is ScannerEvent.ScanningStarted -> stateManager.updateState(ScannerState.Scanning)
                    is ScannerEvent.ScanningStopped -> stateManager.updateState(ScannerState.CameraReady)
                    is ScannerEvent.ScanningPaused -> stateManager.updateState(ScannerState.Paused)
                    is ScannerEvent.ScanningResumed -> stateManager.updateState(ScannerState.Scanning)
                }
            }
        }
    }
    
    val stateListener = remember { { state: ScannerState -> onStateChanged(state) } }
    
    DisposableEffect(Unit) {
        eventManager.addListener(eventListener)
        stateManager.addStateListener(stateListener)
        
        onDispose {
            eventManager.removeListener(eventListener)
            stateManager.removeStateListener(stateListener)
            scanner?.stopScanning()
        }
    }
    
    LaunchedEffect(scanner) {
        scanner?.startScanning()
    }
    
    CameraPreview(
        onPreviewViewReady = { preview ->
            scanner = createScanner(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = preview
            ) {
                withConfig(config)
                withEventManager(eventManager)
                withStateManager(stateManager)
            }
        },
        scanner = scanner,
        modifier = modifier,
        config = config,
        customOverlay = customOverlay,
        customPermissionDialog = customPermissionDialog
    )
}

@Composable
fun SimpleScanner(
    modifier: Modifier = Modifier,
    onCodeScanned: (String) -> Unit,
    playSound: Boolean = true,
    watermark: String = "BelZSpeedScan"
) {
    BelZSpeedScanner(
        modifier = modifier,
        config = ScannerConfig(
            playSound = playSound,
            watermark = watermark
        ),
        onCodeScanned = onCodeScanned
    )
}

@Composable
expect fun CameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner: CodeScanner?,
    modifier: Modifier,
    config: ScannerConfig,
    customOverlay: @Composable (() -> Unit)? = null,
    customPermissionDialog: @Composable (() -> Unit)? = null
) 