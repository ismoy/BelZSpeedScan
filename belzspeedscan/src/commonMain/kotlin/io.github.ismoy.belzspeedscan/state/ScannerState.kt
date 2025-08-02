package io.github.ismoy.belzspeedscan.state

import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance

sealed class ScannerState {
    object Initial : ScannerState()
    object RequestingPermission : ScannerState()
    object PermissionDenied : ScannerState()
    object PermissionGranted : ScannerState()
    object StartingCamera : ScannerState()
    object CameraReady : ScannerState()
    object Scanning : ScannerState()
    object Paused : ScannerState()
    data class Error(val message: String) : ScannerState()
    data class CodeDetected(val code: String) : ScannerState()
}

data class ScannerUiState(
    val scannerState: ScannerState = ScannerState.Initial,
    val distance: CameraPositionDistance = CameraPositionDistance.TOO_FAR,
    val lastScannedCode: String? = null,
    val scanCount: Int = 0,
    val isFlashEnabled: Boolean = false,
    val isTorchAvailable: Boolean = false,
    val cameraResolution: String? = null,
    val scanningDuration: Long = 0L,
    val flashMode: String = "OFF" // "ON" or "OFF"
)

interface ScannerStateManager {
    fun updateState(newState: ScannerState)
    fun getCurrentState(): ScannerState
    fun getUiState(): ScannerUiState
    fun addStateListener(listener: (ScannerState) -> Unit)
    fun removeStateListener(listener: (ScannerState) -> Unit)
}

class DefaultScannerStateManager : ScannerStateManager {
    private var currentState: ScannerState = ScannerState.Initial
    private val stateListeners = mutableListOf<(ScannerState) -> Unit>()
    private var uiState = ScannerUiState()
    
    override fun updateState(newState: ScannerState) {
        currentState = newState
        updateUiState(newState)
        stateListeners.forEach { it(newState) }
    }
    
    override fun getCurrentState(): ScannerState = currentState
    
    override fun getUiState(): ScannerUiState = uiState
    
    override fun addStateListener(listener: (ScannerState) -> Unit) {
        stateListeners.add(listener)
    }
    
    override fun removeStateListener(listener: (ScannerState) -> Unit) {
        stateListeners.remove(listener)
    }
    
    private fun updateUiState(newState: ScannerState) {
        uiState = when (newState) {
            is ScannerState.CodeDetected -> uiState.copy(
                scannerState = newState,
                lastScannedCode = newState.code,
                scanCount = uiState.scanCount + 1
            )
            else -> uiState.copy(scannerState = newState)
        }
    }
    
    fun updateDistance(distance: CameraPositionDistance) {
        uiState = uiState.copy(distance = distance)
    }
    
    fun updateScanningDuration(duration: Long) {
        uiState = uiState.copy(scanningDuration = duration)
    }
    
    fun toggleFlash() {
        val newFlashMode = if (uiState.flashMode == "ON") "OFF" else "ON"
        uiState = uiState.copy(
            isFlashEnabled = newFlashMode == "ON",
            flashMode = newFlashMode
        )
    }
    
    fun setFlashMode(mode: String) {
        uiState = uiState.copy(
            isFlashEnabled = mode == "ON",
            flashMode = mode
        )
    }
    
    fun setTorchAvailable(available: Boolean) {
        uiState = uiState.copy(isTorchAvailable = available)
    }
    
    fun setCameraResolution(resolution: String) {
        uiState = uiState.copy(cameraResolution = resolution)
    }
} 