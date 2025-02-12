package io.github.ismoy.belzspeedscan

import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.utils.currentTimeMillis
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.*
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.CoreGraphics.CGRect
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.QuartzCore.CALayer
import platform.QuartzCore.CATransaction
import platform.UIKit.UIColor
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

@OptIn(ExperimentalForeignApi::class)
class IOSScanner(
    private var previewView: UIView,
    private val isQRScanning: Boolean,
    var onQrCodeScanned: (String) -> Unit,
    private val playSound: Boolean
) : CodeScanner {

    private var captureSession: AVCaptureSession? = null
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var isScanning = false
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private var metadataDelegate: ScannerMetadataDelegate? = null
    private var currentHighlightLayers = mutableMapOf<String, CALayer>()
    private var scannedCodes = mutableSetOf<String>()
    private var lastScannedTimes = mutableMapOf<String, Long>()
    private var audioPlayer: AVAudioPlayer? = null
    private val sessionQueue = dispatch_queue_create("com.tikonsil.scanner.session", null)


    init {
        if (playSound) {
            setupAudioPlayer()
        }
    }
    private fun setupAudioPlayer() {
        try {
            println("Configurando reproductor de audio...")

            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionMixWithOthers,
                error = null
            )
            audioSession.setActive(true, error = null)

            val bundlePath = NSBundle.mainBundle.bundlePath
            val resourcePath = "$bundlePath/compose-resources/beep.mp3"
            val soundUrl = NSURL.fileURLWithPath(resourcePath)
            audioPlayer = AVAudioPlayer(contentsOfURL = soundUrl, error = null)

            audioPlayer?.let { player ->
                if (player.prepareToPlay()) {
                    player.volume = 1.0f
                    player.numberOfLoops = 0

                    if (player.play()) {
                        player.stop()
                        player.currentTime = 0.0
                    } else {
                        println("Error: No se pudo reproducir el sonido en la prueba")
                    }
                } else {
                    println("Error: No se pudo preparar el reproductor")
                }
            } ?: println("Error: No se pudo crear el reproductor de audio")

        } catch (e: Exception) {
            println("Error en setupAudioPlayer: ${e.message}")
            e.printStackTrace()
        }
    }    private fun playBeepSound() {
        if (playSound) {
            try {
                audioPlayer?.let { player ->
                    if (!player.playing) {
                        player.currentTime = 0.0
                        player.play()
                    }
                }

                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            } catch (e: Exception) {
                println("Error reproduciendo sonido: ${e.message}")
            }
        }
    }

    private inner class ScannerMetadataDelegate : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {
        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputMetadataObjects: List<*>,
            fromConnection: AVCaptureConnection
        ) {
            if (!isScanning) return

            dispatch_async(dispatch_get_main_queue()) {
                val currentTime = currentTimeMillis()
                val currentCodes = didOutputMetadataObjects.mapNotNull {
                    (it as? AVMetadataMachineReadableCodeObject)?.stringValue
                }
                clearOldHighlights(currentCodes)

                for (metadata in didOutputMetadataObjects) {
                    val readableObject = metadata as? AVMetadataMachineReadableCodeObject
                    readableObject?.let {
                        val code = it.stringValue ?: return@let

                        val lastScanTime = lastScannedTimes[code] ?: 0L
                        val timeSinceLastScan = currentTime - lastScanTime

                        if (!scannedCodes.contains(code) && timeSinceLastScan > 5000) {
                            lastScannedCode = code
                            lastScannedTime = currentTime
                            lastScannedTimes[code] = currentTime
                            if (isScanning){
                                playBeepSound()
                                onQrCodeScanned(code)
                            }

                            scannedCodes.add(code)

                            val bounds = convertToViewCoordinates(it)
                            if (bounds != null) {
                                updateHighlight(code, bounds)
                            }
                        } else if (scannedCodes.contains(code)) {
                            val bounds = convertToViewCoordinates(it)
                            if (bounds != null) {
                                updateHighlightPosition(code, bounds)
                            }
                        }
                    }
                }
            }
        }
    }
    @OptIn(ExperimentalForeignApi::class)
    private fun convertToViewCoordinates(readableObject: AVMetadataMachineReadableCodeObject): CValue<CGRect>? {
        return try {
            val visualCode = previewLayer?.transformedMetadataObjectForMetadataObject(readableObject) as? AVMetadataMachineReadableCodeObject
            visualCode?.bounds
        } catch (e: Exception) {
            println("Error convirtiendo coordenadas: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun updateHighlight(code: String, bounds: CValue<CGRect>) {
        if (!currentHighlightLayers.containsKey(code)) {
            val highlightBox = CALayer()
            highlightBox.frame = bounds
            highlightBox.borderColor = UIColor.greenColor.CGColor
            highlightBox.borderWidth = 3.0
            highlightBox.backgroundColor = UIColor.clearColor.CGColor

            previewLayer?.addSublayer(highlightBox)
            currentHighlightLayers[code] = highlightBox
        }
    }

    private fun updateHighlightPosition(code: String, bounds: CValue<CGRect>) {
        currentHighlightLayers[code]?.let { layer ->
            CATransaction.begin()
            CATransaction.setDisableActions(true)
            layer.frame = bounds
            CATransaction.commit()
        }
    }

    private fun clearOldHighlights(currentCodes: List<String>) {
        val codesToRemove = currentHighlightLayers.keys.filter { it !in currentCodes }
        codesToRemove.forEach { code ->
            currentHighlightLayers[code]?.removeFromSuperlayer()
            currentHighlightLayers.remove(code)
            lastScannedTimes.remove(code)
        }
    }

    private fun setupCamera() {
        try {
            val newSession = AVCaptureSession()
            captureSession = newSession
            newSession.sessionPreset = AVCaptureSessionPresetHigh

            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                ?: throw RuntimeException("No camera available")

            configureDevice(device)

            val input = AVCaptureDeviceInput(device = device, error = null)

            if (newSession.canAddInput(input)) {
                newSession.addInput(input)
            }

            setupMetadataOutput(newSession)

            val newPreviewLayer = AVCaptureVideoPreviewLayer(session = newSession)
            previewLayer = newPreviewLayer
            newPreviewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            newPreviewLayer.frame = previewView.bounds

            previewView.layer.sublayers?.forEach {
                if (it is AVCaptureVideoPreviewLayer) {
                    it.removeFromSuperlayer()
                }
            }
            previewView.layer.addSublayer(newPreviewLayer)

            dispatch_async(sessionQueue) {
                newSession.startRunning()
            }
        } catch (e: Exception) {
            println("IOSScanner: Error en setupCamera: ${e.message}")
        }

    }

    private fun configureDevice(device: AVCaptureDevice) {
        try {
            device.lockForConfiguration(null)
            if (device.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                device.focusMode = AVCaptureFocusModeContinuousAutoFocus
            }
            device.unlockForConfiguration()
        } catch (e: Exception) {
            println("IOSScanner: Error configurando dispositivo: ${e.message}")
        }
    }

    private fun setupMetadataOutput(session: AVCaptureSession) {
        val metadataOutput = AVCaptureMetadataOutput()
        if (session.canAddOutput(metadataOutput)) {
            session.addOutput(metadataOutput)

            metadataOutput.metadataObjectTypes = if (isQRScanning) {
                listOf(AVMetadataObjectTypeQRCode)
            } else {
                listOf(
                    AVMetadataObjectTypeEAN13Code,
                    AVMetadataObjectTypeEAN8Code,
                    AVMetadataObjectTypeCode128Code,
                    AVMetadataObjectTypeCode39Code,
                    AVMetadataObjectTypeCode93Code,
                    AVMetadataObjectTypeUPCECode
                )
            }

            metadataDelegate = ScannerMetadataDelegate()
            metadataOutput.setMetadataObjectsDelegate(
                metadataDelegate,
                dispatch_get_main_queue()
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun updatePreviewFrame(view: UIView, rect: CValue<CGRect>) {
        previewView = view
        previewLayer?.frame = rect
    }

    override fun startScanning() {
        isScanning = true
        if (captureSession == null) {
            setupCamera()
        } else {
            dispatch_async(sessionQueue) {
                this.captureSession?.startRunning()
            }
        }
    }


    override fun stopScanning() {
        isScanning = false
        audioPlayer?.stop()
        audioPlayer = null
        dispatch_async(sessionQueue) {
            this.captureSession?.stopRunning()
        }
        clearAllHighlights()
    }


    private fun clearAllHighlights() {
        currentHighlightLayers.values.forEach { it.removeFromSuperlayer() }
        currentHighlightLayers.clear()
        lastScannedTimes.clear()
    }

    override fun pauseScanning() {
        isScanning = false
        dispatch_async(sessionQueue) {
            this.captureSession?.stopRunning()
        }
        dispatch_async(dispatch_get_main_queue()) {
            clearAllHighlights()
            previewLayer?.setHidden(true)
        }
    }

    override fun resumeScanning() {
        dispatch_async(dispatch_get_main_queue()) {
            previewLayer?.setHidden(false)
        }
        dispatch_async(sessionQueue) {
            this.captureSession?.startRunning()
            dispatch_async(dispatch_get_main_queue()) {
                isScanning = true
            }
        }
    }
}