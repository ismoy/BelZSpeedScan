package io.github.ismoy.belzspeedscan

import io.github.ismoy.belzspeedscan.domain.CodeScanner
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.camera.core.AspectRatio
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidScanner(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val isQRScanning: Boolean,
    var onCodeScanned: (String) -> Unit,
    private val playSound: Boolean
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

    init {
        if (playSound) {
            setupSound()
        }
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
    }

    private fun playBeepSound() {
        if (playSound && scanBeepSound != 0) {
            soundPool?.play(scanBeepSound, 1f, 1f, 1, 0, 1f)
        }
    }

    override fun startScanning() {
        isScannerActive = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {
        if (!isScannerActive) return

        try {
            cameraProvider?.unbindAll()

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
                                onCodeScanned = { code ->
                                    playBeepSound()
                                    onCodeScanned(code)
                                },
                                isQRScanning = isQRScanning,
                                onDistanceChanged = {distance->
                                    _scanDistance.value = distance
                                }
                            )
                        )
                        isAnalyzerBound = true
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

        } catch (exc: Exception) {
            Log.e("CameraX", "Error al vincular casos de uso", exc)
        }
    }

    override fun pauseScanning() {
        try {
            isScannerActive = false
            isAnalyzerBound = false
            imageAnalysis?.clearAnalyzer()
        } catch (e: Exception) {
            Log.e("CameraX", "Error al pausar el escaner", e)
        }
    }

    override fun resumeScanning() {
        try {
            if (!isScannerActive) {
                isScannerActive = true
                bindCameraUseCases()
            }
        } catch (e: Exception) {
            Log.e("CameraX", "Error al reanudar el escaner", e)
        }
    }

    override fun stopScanning() {
        try {
            isScannerActive = false
            isAnalyzerBound = false
            imageAnalysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            soundPool?.release()
            soundPool = null
        } catch (e: Exception) {
            Log.e("CameraX", "Error al detener el escaner", e)
        }
    }
}