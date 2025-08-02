package io.github.ismoy.belzspeedscan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.ismoy.belzspeedscan.utils.CustomPermissionDialog

@Composable
actual fun RequestCameraPermission(
    titleDialogConfig: String,
    descriptionDialogConfig: String,
    btnDialogConfig: String,
    titleDialogDenied: String,
    descriptionDialogDenied: String,
    btnDialogDenied: String,
    customDeniedDialog: @Composable ((onRetry: () -> Unit) -> Unit)?,
    customSettingsDialog: @Composable ((onOpenSettings: () -> Unit) -> Unit)?,
    onPermissionPermanentlyDenied: () -> Unit,
    onResult: (Boolean) -> Unit,
    customPermissionHandler: (() -> Unit)?
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    var permissionDeniedPermanently by remember { mutableStateOf(false) }
    var permissionDeniedCount by remember { mutableIntStateOf(0) }

    if (permissionDeniedPermanently) {
        LaunchedEffect(Unit) {
            onPermissionPermanentlyDenied()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onResult(true)
        } else {
            permissionDeniedCount++
            when {
                permissionDeniedCount >= 2 -> {
                    permissionDeniedPermanently = true
                    showRationale = false
                }
                else -> {
                    showRationale = true
                    permissionDeniedPermanently = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val currentPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        when (currentPermission) {
            PackageManager.PERMISSION_GRANTED -> {
                onResult(true)
            }
            else -> {
                if (permissionDeniedCount == 0) {
                    if (customPermissionHandler != null) {
                        customPermissionHandler()
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }

    if (showRationale) {
        if (customDeniedDialog != null) {
            customDeniedDialog {
                showRationale = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } else {
            CustomPermissionDialog(
                title = titleDialogDenied,
                description = descriptionDialogDenied,
                confirmButtonText = btnDialogDenied,
                onConfirm = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
    }

    if (permissionDeniedPermanently) {
        if (customSettingsDialog != null) {
            customSettingsDialog { openAppSettings(context) }
        } else {
            CustomPermissionDialog(
                title = titleDialogConfig,
                description = descriptionDialogConfig,
                confirmButtonText = btnDialogConfig,
                onConfirm = {
                    openAppSettings(context)
                }
            )
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
