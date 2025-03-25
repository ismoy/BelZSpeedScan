package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable

@Composable
expect fun RequestCameraPermission(
    titleDialogConfig:String = "Permission Required",
    descriptionDialogConfig:String = "To use the scanner we need access to the camera. Please enable the permission in the settings.",
    btnDialogConfig:String = "Open Settings",
    titleDialogDenied:String = "Permission denied",
    descriptionDialogDenied:String = "Camera is required to scan codes. Please grant permission to continue.",
    btnDialogDenied:String = "Grant Permission",
    customDeniedDialog: (@Composable (onRetry: () -> Unit) -> Unit)? = null,
    customSettingsDialog: (@Composable (onOpenSettings: () -> Unit) -> Unit)? = null,
    onPermissionPermanentlyDenied: () -> Unit = {},
    onResult: (Boolean) -> Unit
)