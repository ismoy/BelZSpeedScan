package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.domain.CodeScanner

@Deprecated(
    message = "This API has been deprecated in favor of the new BelZSpeedScanner composable. " +
            "The new API provides automatic context handling, better configuration management, and improved UX with inactivity detection.",
    replaceWith = ReplaceWith(
        "BelZSpeedScanner(\n" +
        "    onCodeScanned = { code -> },\n" +
        ")",
        imports = ["io.github.ismoy.belzspeedscan.BelZSpeedScanner"]
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun CameraPreview(
    onPreviewViewReady: (Any) -> Unit = {},
    scanner:CodeScanner? = null,
    modifier: Modifier = Modifier,
    waterMark:String? = "BelZSpeedScan",
    tooFarColor: Color? = Color.Red,
    tooCloseColor: Color? = Color.Red,
    tooOptimalColor: Color? = Color.Green,
    tooFarText:String? = "Bring the code closer to the camera\n"+"Distance too far",
    tooCloseText:String? = "Move the code away from the camera \n" + "Too close",
    tooOptimalText:String? = "Perfect distance!\n" + "Keep the code with in the frame",
    titleDialogConfig: String = "Permission Required",
    descriptionDialogConfig: String = "To use the scanner we need access to the camera. Please enable the permission in the settings.",
    btnDialogConfig: String = "Open Settings",
    titleDialogDenied: String = "Permission denied",
    descriptionDialogDenied: String = "Camera is required to scan codes. Please grant permission to continue.",
    btnDialogDenied: String = "Grant Permission",
    customDeniedDialog: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
    customSettingsDialog: (@Composable (onOpenSettings: () -> Unit) -> Unit)? = null,
    onPermissionPermanentlyDenied: () -> Unit = {},
    customPermissionHandler: (() -> Unit)? = null
) {
    BelZSpeedScanner(
        modifier = modifier,
        config = ScannerConfig(
            watermark = waterMark ?: "BelZSpeedScan",
            tooFarColor = tooFarColor ?: Color.Red,
            tooCloseColor = tooCloseColor ?: Color.Red,
            tooOptimalColor = tooOptimalColor ?: Color.Green,
            tooFarText = tooFarText ?: "Bring the code closer to the camera\nDistance too far",
            tooCloseText = tooCloseText ?: "Move the code away from the camera\nToo close",
            tooOptimalText = tooOptimalText ?: "Perfect distance!\nKeep the code with in the frame",
            titleDialogConfig = titleDialogConfig,
            descriptionDialogConfig = descriptionDialogConfig,
            btnDialogConfig = btnDialogConfig,
            titleDialogDenied = titleDialogDenied,
            descriptionDialogDenied = descriptionDialogDenied,
            btnDialogDenied = btnDialogDenied
        ),
        onCodeScanned = { code ->
            // Legacy support - no callback needed for deprecated function  
        },
        onPermissionPermanentlyDenied = onPermissionPermanentlyDenied,
        customOverlay = null,
        customPermissionDialog = null
    )
}