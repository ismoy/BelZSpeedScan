package io.github.ismoy.belzspeedscan

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.domain.ScannerEvent
import io.github.ismoy.belzspeedscan.domain.ScannerEventManager
import io.github.ismoy.belzspeedscan.state.DefaultScannerStateManager
import io.github.ismoy.belzspeedscan.state.ScannerState
import io.github.ismoy.belzspeedscan.utils.LoggerFactory
import io.github.ismoy.belzspeedscan.utils.camera
import io.github.ismoy.belzspeedscan.utils.scanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidScanner(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val config: ScannerConfig,
    private val eventManager: ScannerEventManager,
    private val stateManager: DefaultScannerStateManager,
) : CodeScanner {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private var isScannerActive = true
    private var isAnalyzerBound = false

    private var soundPool: SoundPool? = null
    private var scanBeepSound: Int = 0
    private val _scanDistance = MutableStateFlow(CameraPositionDistance.TOO_FAR)
    val scanDistance: StateFlow<CameraPositionDistance> = _scanDistance.asStateFlow()
    
    private val logger = LoggerFactory.getLogger()

    init {
        if (config.playSound) {
            setupSound()
        }
        logger.scanner("AndroidScanner initialized with config: ${config.watermark}")
    }

    private fun setupSound() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()

        scanBeepSound = soundPool?.load(context, com.google.zxing.client.android.R.raw.zxing_beep, 1) ?: 0
        logger.scanner("Sound setup completed")
    }

    private fun playBeepSound() {
        if (config.playSound && scanBeepSound != 0) {
            soundPool?.play(scanBeepSound, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun startScanning() {
        logger.scanner("Starting scanning")
        isScannerActive = true
        stateManager.updateState(ScannerState.StartingCamera)
        eventManager.emitEvent(ScannerEvent.ScanningStarted)
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        if (!isScannerActive) return

        try {
            val cameraProvider = cameraProvider ?: return
            cameraProvider.unbindAll()

            preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    if (isScannerActive && !isAnalyzerBound) {
                        it.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            MLKitBarcodeAnalyzer(
                                config = config,
                                eventManager = eventManager,
                                stateManager = stateManager,
                                isScannerActive = { isScannerActive },
                                onDistanceChanged = { distance ->
                                    _scanDistance.value = distance
                                    eventManager.emitEvent(ScannerEvent.DistanceChanged(distance))
                                },
                                onPlaySound = { playBeepSound() }
                            )
                        )
                        isAnalyzerBound = true
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            checkTorchAvailability()
            
            stateManager.updateState(ScannerState.CameraReady)
            stateManager.updateState(ScannerState.Scanning)
            logger.camera("Camera bound successfully")

        } catch (exc: Exception) {
            logger.error("CameraX", "Error al vincular casos de uso", exc)
            val errorMessage = "No se pudo iniciar la cámara: ${exc.localizedMessage}"
            eventManager.emitEvent(ScannerEvent.CameraError(errorMessage))
            stateManager.updateState(ScannerState.Error(errorMessage))
        }
    }

    override fun pauseScanning() {
        try {
            logger.scanner("Pausing scanning")
            isScannerActive = false
            isAnalyzerBound = false
            imageAnalysis?.clearAnalyzer()
            stateManager.updateState(ScannerState.Paused)
            eventManager.emitEvent(ScannerEvent.ScanningPaused)
        } catch (e: Exception) {
            logger.error("CameraX", "Error al pausar el escaner", e)
        }
    }

    override fun resumeScanning() {
        try {
            logger.scanner("Resuming scanning")
            if (!isScannerActive && camera != null) {
                isScannerActive = true
                imageAnalysis?.let { analysis ->
                    if (!isAnalyzerBound) {
                        analysis.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            MLKitBarcodeAnalyzer(
                                config = config,
                                eventManager = eventManager,
                                stateManager = stateManager,
                                isScannerActive = { isScannerActive },
                                onDistanceChanged = { distance ->
                                    _scanDistance.value = distance
                                    eventManager.emitEvent(ScannerEvent.DistanceChanged(distance))
                                },
                                onPlaySound = { playBeepSound() }
                            )
                        )
                        isAnalyzerBound = true
                    }
                }
                stateManager.updateState(ScannerState.Scanning)
                eventManager.emitEvent(ScannerEvent.ScanningResumed)
            }
        } catch (e: Exception) {
            logger.error("CameraX", "Error al reanudar el escaner", e)
        }
    }

    override fun stopScanning() {
        try {
            logger.scanner("Stopping scanning")
            isScannerActive = false
            isAnalyzerBound = false

            try {
                camera?.cameraControl?.enableTorch(false)
                println("DEBUG: Torch turned off in stopScanning")
            } catch (e: Exception) {
                println("DEBUG: Error turning off torch in stopScanning: ${e.message}")
            }
            
            imageAnalysis?.clearAnalyzer()
            camera?.cameraControl?.cancelFocusAndMetering()
            cameraProvider?.unbindAll()
            camera = null
            preview = null
            imageAnalysis = null
            cameraProvider = null
            soundPool?.release()
            soundPool = null
            
            stateManager.updateState(ScannerState.CameraReady)
            eventManager.emitEvent(ScannerEvent.ScanningStopped)
            logger.scanner("Scanner stopped completely - camera fully disabled")
        } catch (e: Exception) {
            logger.error("CameraX", "Error al detener el escaner", e)
        }
    }
    fun toggleTorch() {
        try {
            val currentUiState = stateManager.getUiState()
            val newFlashMode = if (currentUiState.flashMode == "ON") "OFF" else "ON"
            camera?.cameraControl?.enableTorch(newFlashMode == "ON")
            stateManager.setFlashMode(newFlashMode)
            eventManager.emitEvent(ScannerEvent.FlashStateChanged(
                isEnabled = newFlashMode == "ON",
                mode = newFlashMode
            ))
            
            logger.camera("Torch toggled to: $newFlashMode")
        } catch (e: Exception) {
            logger.error("CameraX", "Error toggling torch", e)
        }
    }
    fun setTorch(enabled: Boolean) {
        try {
            val newFlashMode = if (enabled) "ON" else "OFF"
            camera?.cameraControl?.enableTorch(enabled)
            stateManager.setFlashMode(newFlashMode)
            eventManager.emitEvent(ScannerEvent.FlashStateChanged(
                isEnabled = enabled,
                mode = newFlashMode
            ))
            
            logger.camera("Torch set to: $newFlashMode")
            println("DEBUG: AndroidScanner.setTorch($enabled) - camera: ${camera != null}")
        } catch (e: Exception) {
            logger.error("CameraX", "Error setting torch", e)
            println("DEBUG: Error setting torch: ${e.message}")
        }
    }

    fun checkTorchAvailability() {
        try {
            val cameraInfo = camera?.cameraInfo
            val hasFlashUnit = cameraInfo?.hasFlashUnit() ?: false
            stateManager.setTorchAvailable(hasFlashUnit)
            eventManager.emitEvent(ScannerEvent.TorchAvailabilityChanged(hasFlashUnit))
            logger.camera("Torch availability: $hasFlashUnit")
        } catch (e: Exception) {
            logger.error("CameraX", "Error checking torch availability", e)
            stateManager.setTorchAvailable(false)
            eventManager.emitEvent(ScannerEvent.TorchAvailabilityChanged(false))
        }
    }
    fun restoreFlashState() {
        try {
            val currentUiState = stateManager.getUiState()
            println("DEBUG: RestoreFlashState - current flash mode: ${currentUiState.flashMode}")
            println("DEBUG: RestoreFlashState - camera available: ${camera != null}")
            
            if (currentUiState.flashMode == "ON") {
                camera?.cameraControl?.enableTorch(true)
                stateManager.setFlashMode("ON")
                eventManager.emitEvent(ScannerEvent.FlashStateChanged(
                    isEnabled = true,
                    mode = "ON"
                ))
                
                logger.camera("Flash state restored to ON")
                println("DEBUG: Flash state restored to ON - camera: ${camera != null}")
            } else {
                println("DEBUG: Flash state was OFF, keeping it OFF")
                println("DEBUG: This means the state was not saved correctly before going inactive")
            }
        } catch (e: Exception) {
            logger.error("CameraX", "Error restoring flash state", e)
            println("DEBUG: Error restoring flash state: ${e.message}")
        }
    }
    fun emergencyTurnOffTorch() {
        try {
            camera?.cameraControl?.enableTorch(false)
            stateManager.setFlashMode("OFF")
            eventManager.emitEvent(ScannerEvent.FlashStateChanged(
                isEnabled = false,
                mode = "OFF"
            ))
            try {
                cameraProvider?.unbindAll()
                println("DEBUG: Camera unbound to force torch off")
            } catch (e: Exception) {
                println("DEBUG: Error in emergency torch off approach 4: ${e.message}")
            }
            
            logger.camera("Emergency torch turn off executed")
            println("DEBUG: Emergency torch turn off - camera: ${camera != null}")
        } catch (e: Exception) {
            logger.error("CameraX", "Error in emergency torch turn off", e)
            println("DEBUG: Error in emergency torch turn off: ${e.message}")
        }
    }
}