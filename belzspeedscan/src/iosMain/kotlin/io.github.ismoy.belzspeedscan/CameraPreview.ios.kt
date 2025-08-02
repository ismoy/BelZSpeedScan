package io.github.ismoy.belzspeedscan

import IOSScanner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.state.ScannerState
import io.github.ismoy.belzspeedscan.utils.ActiveScanningOverlay
import io.github.ismoy.belzspeedscan.utils.InactiveScanningOverlay
import io.github.ismoy.belzspeedscan.utils.currentTimeMillis
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.UIKit.UIColor
import platform.UIKit.UIView

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun CameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner: CodeScanner?,
    modifier: Modifier,
    config: ScannerConfig,
    customOverlay: @Composable (() -> Unit)?,
    customPermissionDialog: @Composable (() -> Unit)?
) {
    var previewView by remember { mutableStateOf<UIView?>(null) }
    var isCameraInactive by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableLongStateOf(currentTimeMillis()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanDistance by remember { mutableStateOf(CameraPositionDistance.TOO_FAR) }
    var hasPermission by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf<ScannerState>(ScannerState.Initial) }

    fun updateActivity() {
        lastActivityTime = currentTimeMillis()
        if (isCameraInactive) {
            isCameraInactive = false
            scanner?.startScanning()
        }
    }

    LaunchedEffect(scanner) {
        (scanner as? IOSScanner)?.scanDistance?.collect { distance ->
            scanDistance = distance
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val timeSinceLastActivity = currentTimeMillis() - lastActivityTime

            if (timeSinceLastActivity > 15000 && !isCameraInactive) {
                isCameraInactive = true
                isScanning = false
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
            .background(if (isCameraInactive) Color.Black else Color.Transparent)
    ) {
        // Only show camera preview when not inactive
        if (!isCameraInactive) {
            UIKitView(
                factory = {
                    UIView().apply {
                        backgroundColor = UIColor.blackColor
                        onPreviewViewReady(this)
                        previewView = this
                        (scanner as? IOSScanner)?.updatePreviewFrame(this, this.bounds)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        updateActivity()
                    },
                update = { view ->
                    (scanner as? IOSScanner)?.updatePreviewFrame(view, view.bounds)
                },
                properties = UIKitInteropProperties(
                    isInteractive = true,
                    isNativeAccessibilityEnabled = true
                )
            )
        }

        if (!isCameraInactive) {
            val currentDistance by remember { derivedStateOf { scanDistance } }
            if (customOverlay != null) {
                customOverlay()
            } else {
                ActiveScanningOverlay(
                    watermark = config.watermark,
                    scanDistance = currentDistance,
                    tooFarColor = config.tooFarColor,
                    tooCloseColor = config.tooCloseColor,
                    tooOptimalColor = config.tooOptimalColor,
                    tooFarText = config.tooFarText,
                    tooCloseText = config.tooCloseText,
                    tooOptimalText = config.tooOptimalText,
                )
            }
        }

        if (isCameraInactive) {
            InactiveScanningOverlay(
                onTap = {
                    updateActivity()
                    isScanning = true
                },
                tapText = config.inactiveModeText
            )
        }
    }

    LaunchedEffect(previewView, scanner) {
        if (!isCameraInactive && previewView != null) {
            (scanner as? IOSScanner)?.let { iosScanner ->
                iosScanner.startScanning()
                isScanning = true
                updateActivity()
            }
        }
    }
}

@Deprecated(
    message = "This API has been deprecated in favor of the new BelZSpeedScanner composable. " +
            "The new API provides automatic context handling, better configuration management, and improved UX with inactivity detection.",
    replaceWith = ReplaceWith(
        "BelZSpeedScanner(\n" +
        "    onCodeScanned = { code -> /* your code */ },\n" +
        ")",
        imports = ["io.github.ismoy.belzspeedscan.BelZSpeedScanner"]
    ),
    level = DeprecationLevel.WARNING
)
@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun CameraPreview(
    onPreviewViewReady: (Any) -> Unit,
    scanner: CodeScanner?,
    modifier: Modifier,
    waterMark: String?,
    tooFarColor: Color?,
    tooCloseColor: Color?,
    tooOptimalColor: Color?,
    tooFarText: String?,
    tooCloseText: String?,
    tooOptimalText: String?,
    titleDialogConfig: String,
    descriptionDialogConfig: String,
    btnDialogConfig: String,
    titleDialogDenied: String,
    descriptionDialogDenied: String,
    btnDialogDenied: String,
    customDeniedDialog: @Composable ((onRetry: () -> Unit) -> Unit)?,
    customSettingsDialog: @Composable ((onOpenSettings: () -> Unit) -> Unit)?,
    onPermissionPermanentlyDenied: () -> Unit,
    customPermissionHandler: (() -> Unit)?
) {
    val config = ScannerConfig(
        watermark = waterMark ?: "BelZSpeedScan",
        tooFarColor = tooFarColor ?: Color.Red,
        tooCloseColor = tooCloseColor ?: Color.Red,
        tooOptimalColor = tooOptimalColor ?: Color.Green,
        tooFarText = tooFarText ?: "Bring the code closer to the camera\nDistance too far",
        tooCloseText = tooCloseText ?: "Move the code away from the camera \nToo close",
        tooOptimalText = tooOptimalText ?: "Perfect distance!\nKeep the code with in the frame",
        titleDialogConfig = titleDialogConfig,
        descriptionDialogConfig = descriptionDialogConfig,
        btnDialogConfig = btnDialogConfig,
        titleDialogDenied = titleDialogDenied,
        descriptionDialogDenied = descriptionDialogDenied,
        btnDialogDenied = btnDialogDenied
    )
    
    CameraPreview(
        onPreviewViewReady = onPreviewViewReady,
        scanner = scanner,
        modifier = modifier,
        config = config,
        customOverlay = null,
        customPermissionDialog = null
    )
}
