package io.github.ismoy.belzspeedscan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.ismoy.belzspeedscan.utils.CustomPermissionDialog
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDevice

@Composable
actual fun RequestCameraPermission(
    titleDialogConfig: String,
    descriptionDialogConfig: String,
    btnDialogConfig: String,
    titleDialogDenied: String,
    descriptionDialogDenied: String,
    btnDialogDenied: String,
    customDeniedDialog: (@Composable (onRetry: () -> Unit) -> Unit)?,
    customSettingsDialog: (@Composable (onOpenSettings: () -> Unit) -> Unit)?,
    onPermissionPermanentlyDenied: () -> Unit,
    onResult: (Boolean) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var isPermissionDeniedPermanently by remember { mutableStateOf(false) }

    LaunchedEffect(isPermissionDeniedPermanently) {
        if (isPermissionDeniedPermanently) {
            onPermissionPermanentlyDenied()
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestPermission(
            onGranted = { onResult(true) },
            onDenied = {
                isPermissionDeniedPermanently = true
                showDialog = true
            }
        )
    }

    if (showDialog) {
        if (isPermissionDeniedPermanently) {
            if (customSettingsDialog != null) {
                customSettingsDialog { openSettings() }
            } else {
                CustomPermissionDialog(
                    title = titleDialogConfig,
                    description = descriptionDialogConfig,
                    confirmButtonText = btnDialogConfig,
                    onConfirm = {
                        openSettings()
                        showDialog = false
                    }
                )
            }
        } else {
            if (customDeniedDialog != null) {
                customDeniedDialog {
                    showDialog = false
                    requestCameraAccess { granted ->
                        onResult(granted)
                    }
                }
            } else {
                CustomPermissionDialog(
                    title = titleDialogDenied,
                    description = descriptionDialogDenied,
                    confirmButtonText = btnDialogDenied,
                    onConfirm = {
                        showDialog = false
                        requestCameraAccess { granted ->
                            onResult(granted)
                        }
                    }
                )
            }
        }
    }
}

private fun checkAndRequestPermission(
    onGranted: () -> Unit,
    onDenied: () -> Unit
) {
    when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
        AVAuthorizationStatusAuthorized -> {
            onGranted()
        }
        AVAuthorizationStatusNotDetermined -> {
            requestCameraAccess { granted ->
                if (granted) onGranted() else onDenied()
            }
        }
        else -> {
            onDenied()
        }
    }
}

private fun requestCameraAccess(callback: (Boolean) -> Unit) {
    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
        callback(granted)
    }
}

private fun openSettings() {
    val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
    if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
        if (UIDevice.currentDevice.systemVersion.toDouble() >= 10.0) {
            UIApplication.sharedApplication.openURL(
                settingsUrl,
                options = mapOf<Any?, Any?>(),
                completionHandler = null
            )
        } else {
            UIApplication.sharedApplication.openURL(settingsUrl)
        }
    }
}
