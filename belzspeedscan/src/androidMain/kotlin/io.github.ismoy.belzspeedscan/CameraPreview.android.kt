package io.github.ismoy.belzspeedscan

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.data.models.FlashMode
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.state.ScannerState
import io.github.ismoy.belzspeedscan.utils.ActiveScanningOverlay
import io.github.ismoy.belzspeedscan.utils.FlashToggleButton
import io.github.ismoy.belzspeedscan.utils.InactiveScanningOverlay
import kotlinx.coroutines.delay

@SuppressLint("ClickableViewAccessibility")
@Composable
actual fun PlatformCameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner: CodeScanner?,
    modifier: Modifier,
    config: ScannerConfig,
    customOverlay: @Composable (() -> Unit)?,
    customPermissionDialog: @Composable (() -> Unit)?
) {
    var isCameraInactive by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastScanTime by remember { mutableLongStateOf(0L) }
    var isScanning by remember { mutableStateOf(false) }
    var hasUserInteraction by remember { mutableStateOf(false) }
    var scanDistance by remember { mutableStateOf(CameraPositionDistance.TOO_FAR) }
    var hasPermission by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf<ScannerState>(ScannerState.Initial) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var isTorchAvailable by remember { mutableStateOf(false) }
    var previousFlashState by remember { mutableStateOf(FlashMode.OFF) }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
        hasUserInteraction = true
        if (isCameraInactive) {
            println("DEBUG: Reactivating scanner from inactive mode")
            isCameraInactive = false
            scanner?.startScanning()
        }
    }

    LaunchedEffect(scanner) {
        (scanner as? AndroidScanner)?.let { androidScanner ->
            androidScanner.scanDistance.collect { distance ->
                scanDistance = distance
            }
        }

        scanner?.let { scanner ->
            isScanning = true
            hasUserInteraction = true
        }
    }

    LaunchedEffect(scanner) {
        (scanner as? AndroidScanner)?.let { androidScanner ->
            androidScanner.checkTorchAvailability()

            if (!isCameraInactive && previousFlashState == FlashMode.ON) {
                delay(2000)
                try {
                    androidScanner.restoreFlashState()
                    println("DEBUG: Flash restored after scanner became available")
                } catch (e: Exception) {
                    println("DEBUG: Error restoring flash after scanner available: ${e.message}")
                }
            }
        }
    }

    LaunchedEffect(isCameraInactive) {
        if (isCameraInactive) {
            println("DEBUG: Camera became inactive, turning off flash")
            (scanner as? AndroidScanner)?.let { androidScanner ->
                try {
                    androidScanner.emergencyTurnOffTorch()
                    println("DEBUG: Emergency flash turn off executed for inactive mode")
                } catch (e: Exception) {
                    println("DEBUG: Error in emergency flash turn off: ${e.message}")
                }
            }
        } else {
            println("DEBUG: Camera became active, turning off flash")
            flashMode = FlashMode.OFF
            delay(300)
            (scanner as? AndroidScanner)?.let { androidScanner ->
                try {
                    androidScanner.setTorch(false)
                    println("DEBUG: Flash turned OFF after reactivation")
                } catch (e: Exception) {
                    println("DEBUG: Error turning off flash: ${e.message}")
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val currentTime = System.currentTimeMillis()
            val timeSinceLastActivity = currentTime - lastActivityTime
            val timeSinceLastScan = currentTime - lastScanTime

            if (isScanning && timeSinceLastScan > 10000) {
                isScanning = false
            }
            if (timeSinceLastActivity > 8000) {
                println("DEBUG: Time since last activity: ${timeSinceLastActivity}ms, isCameraInactive: $isCameraInactive")
            }

            if (timeSinceLastActivity > config.inactivityDelay && !isCameraInactive) {
                println("DEBUG: Activating inactive mode!")
                println("DEBUG: Current flash mode before inactive: $flashMode")

                isCameraInactive = true
                scanner?.stopScanning()
            }
        }
    }

    if (!hasPermission) {
        RequestCameraPermission(
            titleDialogConfig = config.titleDialogConfig,
            descriptionDialogConfig = config.descriptionDialogConfig,
            btnDialogConfig = config.btnDialogConfig,
            titleDialogDenied = config.titleDialogDenied,
            descriptionDialogDenied = config.descriptionDialogDenied,
            btnDialogDenied = config.btnDialogDenied,
            customDeniedDialog = null,
            customSettingsDialog = null,
            onPermissionPermanentlyDenied = {},
            onResult = { granted -> hasPermission = granted },
            customPermissionHandler = null
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isCameraInactive) Color.Black else Color.Transparent)
    ) {
        if (!isCameraInactive) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context: Context ->
                    PreviewView(context).also { previewView ->
                        onPreviewViewReady(previewView)
                    }
                },
                update = { previewView ->
                    previewView.setOnTouchListener { _, _ ->
                        updateActivity()
                        false
                    }
                }
            )
        }

        if (!isCameraInactive) {
            if (customOverlay != null) {
                customOverlay()
            } else {
                ActiveScanningOverlay(
                    watermark = config.watermark,
                    scanDistance = scanDistance,
                    tooFarColor = config.tooFarColor,
                    tooCloseColor = config.tooCloseColor,
                    tooOptimalColor = config.tooOptimalColor,
                    tooFarText = config.tooFarText,
                    tooCloseText = config.tooCloseText,
                    tooOptimalText = config.tooOptimalText,
                )
            }
        }

        if (config.enableFlashControl && config.showFlashButton && !isCameraInactive) {
            FlashToggleButton(
                flashMode = flashMode,
                iconColor = Color.White,
                onToggle = {
                    (scanner as? AndroidScanner)?.toggleTorch()
                    flashMode = if (flashMode == FlashMode.ON) FlashMode.OFF else FlashMode.ON
                    println("DEBUG: Flash toggled to: $flashMode")
                }
            )
        }

        if (isCameraInactive) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                InactiveScanningOverlay(
                    onTap = {
                        updateActivity()
                        isScanning = false
                    },
                    tapText = config.inactiveModeText
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isCameraInactive) {
                scanner?.stopScanning()
            }
        }
    }
}

