package io.github.ismoy.belzspeedscan.domain

import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance

sealed class ScannerEvent {
    data class CodeScanned(val code: String) : ScannerEvent()
    data class DistanceChanged(val distance: CameraPositionDistance) : ScannerEvent()
    data class CameraError(val error: String) : ScannerEvent()
    data class PermissionResult(val granted: Boolean) : ScannerEvent()
    data class PermissionPermanentlyDenied(val reason: String) : ScannerEvent()
    data class FlashStateChanged(val isEnabled: Boolean, val mode: String) : ScannerEvent()
    data class TorchAvailabilityChanged(val isAvailable: Boolean) : ScannerEvent()
    object ScanningStarted : ScannerEvent()
    object ScanningStopped : ScannerEvent()
    object ScanningPaused : ScannerEvent()
    object ScanningResumed : ScannerEvent()
}

interface ScannerEventListener {
    fun onEvent(event: ScannerEvent)
}

class ScannerEventManager {
    private val listeners = mutableListOf<ScannerEventListener>()
    
    fun addListener(listener: ScannerEventListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: ScannerEventListener) {
        listeners.remove(listener)
    }
    
    fun emitEvent(event: ScannerEvent) {
        listeners.forEach { it.onEvent(event) }
    }
    
    fun clear() {
        listeners.clear()
    }
} 