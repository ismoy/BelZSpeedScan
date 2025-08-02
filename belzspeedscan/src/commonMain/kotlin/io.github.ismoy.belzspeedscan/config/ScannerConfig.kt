package io.github.ismoy.belzspeedscan.config

import androidx.compose.ui.graphics.Color

data class ScannerConfig(
    // Audio configuration
    val playSound: Boolean = true,
    val soundResourceName: String = "beep",
    val soundResourceExtension: String = "mp3",
    
    // Scanning behavior
    val delayToNextScan: Long = 2000L,
    val areaRatioThreshold: Float = 0.008f,
    val requiredConsecutiveReadings: Int = 2,
    val distanceBufferSize: Int = 3,
    val maliciousCodeCooldown: Long = 3000L,
    
    // UI customization
    val watermark: String = "BelZSpeedScan",
    val tooFarColor: Color = Color.Red,
    val tooCloseColor: Color = Color.Red,
    val tooOptimalColor: Color = Color.Green,
    val tooFarText: String = "Bring the code closer to the camera\nDistance too far",
    val tooCloseText: String = "Move the code away from the camera\nToo close",
    val tooOptimalText: String = "Perfect distance!\nKeep the code within the frame",
    val inactiveModeText: String = "Tap to continue",
    
    // Permission dialogs
    val titleDialogConfig: String = "Permission Required",
    val descriptionDialogConfig: String = "To use the scanner we need access to the camera. Please enable the permission in the settings.",
    val btnDialogConfig: String = "Open Settings",
    val titleDialogDenied: String = "Permission denied",
    val descriptionDialogDenied: String = "Camera is required to scan codes. Please grant permission to continue.",
    val btnDialogDenied: String = "Grant Permission",
    
    // Security settings
    val maxCodeLength: Int = 2000,
    val maxSpecialCharRatio: Float = 0.15f,
    val enableSecurityAlerts: Boolean = true,
    
    // Flash/Torch settings
    val enableFlashControl: Boolean = true,
    val showFlashButton: Boolean = true,
    
    // Inactivity settings
    val inactivityDelay: Long = 30000L
) {
    companion object {
        fun default() = ScannerConfig()
        
        fun minimal() = ScannerConfig(
            playSound = false,
            watermark = "",
            enableSecurityAlerts = false
        )
        
        fun highSecurity() = ScannerConfig(
            delayToNextScan = 3000L,
            requiredConsecutiveReadings = 3,
            maliciousCodeCooldown = 5000L,
            maxCodeLength = 1000,
            maxSpecialCharRatio = 0.10f
        )
    }
} 