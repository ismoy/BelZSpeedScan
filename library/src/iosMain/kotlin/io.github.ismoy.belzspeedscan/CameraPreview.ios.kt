package io.github.ismoy.belzspeedscan

import IOSScanner
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
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.domain.CodeScanner
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
    waterMark:String?,
    tooFarColor: Color?,
    tooCloseColor: Color?,
    tooOptimalColor: Color?,
    tooFarText:String?,
    tooCloseText:String?,
    tooOptimalText:String?
) {
    var previewView by remember { mutableStateOf<UIView?>(null) }
    var isCameraInactive by remember { mutableStateOf(false) }
    var lastActivityTime by remember { mutableLongStateOf(currentTimeMillis()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanDistance by remember { mutableStateOf(CameraPositionDistance.TOO_FAR) }


    fun updateActivity() {
        lastActivityTime = currentTimeMillis()
        if (isCameraInactive) {
            isCameraInactive = false
            scanner?.resumeScanning()
        }
    }
    LaunchedEffect(scanner) {
        (scanner as? IOSScanner)?.scanDistance?.collect { distance ->
            scanDistance = distance
        }
    }

    LaunchedEffect(scanner) {
        (scanner as? IOSScanner)?.let { iosScanner ->
            val originalCallback = iosScanner.onQrCodeScanned
            iosScanner.onQrCodeScanned = { code ->
                updateActivity()
                originalCallback(code)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            val timeSinceLastActivity = currentTimeMillis() - lastActivityTime

            if (timeSinceLastActivity > 30000 && isScanning) {
                isCameraInactive = true
                isScanning = false
                scanner?.pauseScanning()
            }
        }
    }

    Box(modifier = modifier) {
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

        if (!isCameraInactive) {
            val currentDistance by remember { derivedStateOf { scanDistance } }
            ActiveScanningOverlay(
                watermark = waterMark!!,
                scanDistance = currentDistance,
                tooFarColor = tooFarColor!!,
                tooCloseColor = tooCloseColor!!,
                tooOptimalColor = tooOptimalColor!!,
                tooFarText = tooFarText!!,
                tooCloseText = tooCloseText!!,
                tooOptimalText = tooOptimalText!!
            )
        }

        if (isCameraInactive) {
            InactiveScanningOverlay(
                onTap = {
                    updateActivity()
                    isScanning = true
                },
            )
        }
    }

    LaunchedEffect(previewView, scanner) {
        previewView?.let {
            (scanner as? IOSScanner)?.let { iosScanner ->
                iosScanner.startScanning()
                isScanning = true
                updateActivity()
            }
        }
    }
}
