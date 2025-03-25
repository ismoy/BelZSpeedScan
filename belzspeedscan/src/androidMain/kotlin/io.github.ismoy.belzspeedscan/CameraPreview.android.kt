package io.github.ismoy.belzspeedscan

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.view.PreviewView
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
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.utils.ActiveScanningOverlay
import io.github.ismoy.belzspeedscan.utils.InactiveScanningOverlay
import kotlinx.coroutines.delay

@SuppressLint("ClickableViewAccessibility")
@Composable
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
) {
    var isCameraInactive by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastScanTime by remember { mutableLongStateOf(0L) }
    var isScanning by remember { mutableStateOf(false) }
    var hasUserInteraction by remember { mutableStateOf(false) }
    var scanDistance by remember { mutableStateOf(CameraPositionDistance.TOO_FAR) }

    fun updateActivity() {
        lastActivityTime = System.currentTimeMillis()
        hasUserInteraction = true
        if (isCameraInactive) {
            isCameraInactive = false
            scanner?.resumeScanning()
        }
    }

    LaunchedEffect(scanner) {
        (scanner as? AndroidScanner)?.let { androidScanner ->
            val originalCallback = androidScanner.onCodeScanned
            androidScanner.onCodeScanned = { code ->
                lastScanTime = System.currentTimeMillis()
                isScanning = true
                hasUserInteraction = true
                updateActivity()
                originalCallback(code)
            }
            androidScanner.scanDistance.collect { distance ->
                scanDistance = distance
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

            if (!isScanning &&
                hasUserInteraction &&
                timeSinceLastActivity > 30000 &&
                !isCameraInactive
            ) {
                isCameraInactive = true
                scanner?.pauseScanning()
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
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

        if (!isCameraInactive) {
            ActiveScanningOverlay(
                watermark = waterMark ?: "BelZSpeedScan",
                scanDistance = scanDistance,
                tooFarColor = tooFarColor ?: Color.Red,
                tooCloseColor = tooCloseColor ?: Color.Red,
                tooOptimalColor = tooOptimalColor ?: Color.Green,
                tooFarText = tooFarText ?: "Bring the code closer to the camera\nDistance too far",
                tooCloseText = tooCloseText ?: "Move the code away from the camera \nToo close",
                tooOptimalText = tooOptimalText ?: "Perfect distance!\nKeep the code with in the frame",
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
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isCameraInactive) {
                scanner?.pauseScanning()
            }
        }
    }
}