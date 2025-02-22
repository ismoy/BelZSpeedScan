import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.utils.currentTimeMillis
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVCaptureAutoFocusRangeRestrictionNear
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInUltraWideCamera
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureExposureModeContinuousAutoExposure
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeAztecCode
import platform.AVFoundation.AVMetadataObjectTypeCodabarCode
import platform.AVFoundation.AVMetadataObjectTypeCode128Code
import platform.AVFoundation.AVMetadataObjectTypeCode39Code
import platform.AVFoundation.AVMetadataObjectTypeCode93Code
import platform.AVFoundation.AVMetadataObjectTypeDataMatrixCode
import platform.AVFoundation.AVMetadataObjectTypeEAN13Code
import platform.AVFoundation.AVMetadataObjectTypeEAN8Code
import platform.AVFoundation.AVMetadataObjectTypeITF14Code
import platform.AVFoundation.AVMetadataObjectTypePDF417Code
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.AVMetadataObjectTypeUPCECode
import platform.AVFoundation.autoFocusRangeRestriction
import platform.AVFoundation.deviceType
import platform.AVFoundation.exposureMode
import platform.AVFoundation.focusMode
import platform.AVFoundation.focusPointOfInterest
import platform.AVFoundation.isAutoFocusRangeRestrictionSupported
import platform.AVFoundation.isExposureModeSupported
import platform.AVFoundation.isFocusModeSupported
import platform.AVFoundation.isFocusPointOfInterestSupported
import platform.AVFoundation.lensPosition
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.Foundation.NSBundle
import platform.Foundation.NSTimer
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
    private val playSound: Boolean,
    private val resourceName:String,
    private val resourceExtension:String,
    private val delayToNextScan: Long,
    var onCodeScanned: (String) -> Unit,
) : CodeScanner {

    private var captureSession: AVCaptureSession? = null
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var isScanning = false
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private var metadataDelegate: ScannerMetadataDelegate? = null
    private var currentHighlightLayers = mutableMapOf<String, CALayer>()
    private var lastScannedTimes = mutableMapOf<String, Long>()
    private var audioPlayer: AVAudioPlayer? = null
    private val sessionQueue = dispatch_queue_create("com.tikonsil.scanner.session", null)
    private val _scanDistance = MutableStateFlow(CameraPositionDistance.TOO_FAR)
    val scanDistance: StateFlow<CameraPositionDistance> = _scanDistance.asStateFlow()
    private var monitoringTimer: NSTimer? = null


    init {
        if (playSound) {
            setupAudioPlayer()
        }
    }

    init {
        if (playSound) {
            setupAudioPlayer()
        }
    }

    private fun setupAudioPlayer() {
        try {
            println("Setting up audio player...")

            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionMixWithOthers,
                error = null
            )
            audioSession.setActive(true, error = null)
            println("Audio session configured successfully")

            val bundlePath = NSBundle.mainBundle.bundlePath
            val resourcePath = "$bundlePath/compose-resources/${resourceName}.${resourceExtension}"
            println("Searching for file in: $resourcePath")

            val soundUrl = NSURL.fileURLWithPath(resourcePath)
            println("URL of the created file: ${soundUrl.absoluteString}")

            audioPlayer = AVAudioPlayer(contentsOfURL = soundUrl, error = null)

            audioPlayer?.let { player ->
                if (player.prepareToPlay()) {
                    println("Player prepared correctly")
                    player.volume = 1.0f
                    player.numberOfLoops = 0

                    if (player.play()) {
                        println("Sound check successful")
                        player.stop()
                        player.currentTime = 0.0
                    } else {
                        println("Error: Sound could not be played in the test ")
                    }
                } else {
                    println("Error: Failed to prepare player")
                }
            } ?: println("Error: Failed to create audio player")

        } catch (e: Exception) {
            println("Error in AudioPlayer setup: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun playBeepSound() {
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
                println("Error playing sound: ${e.message}")
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private inner class ScannerMetadataDelegate : NSObject(),
        AVCaptureMetadataOutputObjectsDelegateProtocol {
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
                        if (timeSinceLastScan > delayToNextScan) {
                            lastScannedCode = code
                            lastScannedTime = currentTime
                            lastScannedTimes[code] = currentTime
                            if (isScanning) {
                                playBeepSound()
                                onCodeScanned(code)
                            }

                            val bounds = convertToViewCoordinates(it)
                            if (bounds != null) {
                                updateHighlight(code, bounds)
                            }
                        } else {
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
            val visualCode =
                previewLayer?.transformedMetadataObjectForMetadataObject(readableObject) as? AVMetadataMachineReadableCodeObject
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

    private fun getCameraDevice(): AVCaptureDevice? {
        val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
            deviceTypes = listOf(
                AVCaptureDeviceTypeBuiltInUltraWideCamera,
                AVCaptureDeviceTypeBuiltInWideAngleCamera
            ),
            mediaType = AVMediaTypeVideo,
            position = AVCaptureDevicePositionBack
        )

        return (discoverySession.devices.firstOrNull { device ->
            (device as? AVCaptureDevice)?.deviceType == AVCaptureDeviceTypeBuiltInUltraWideCamera
        } as? AVCaptureDevice) ?: (discoverySession.devices.firstOrNull() as? AVCaptureDevice)
    }

    private fun setupCamera() {
        try {
            val newSession = AVCaptureSession()
            captureSession = newSession
            newSession.sessionPreset = AVCaptureSessionPresetHigh

            val device = getCameraDevice()
                ?: throw RuntimeException("No camera available")

            configureDevice(device)
            setupCameraObserver(device)

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

    private fun setupCameraObserver(device: AVCaptureDevice) {
        dispatch_async(dispatch_get_main_queue()) {
            startLensPositionMonitoring(device)
        }
    }

    private fun startLensPositionMonitoring(device: AVCaptureDevice) {
        monitoringTimer = NSTimer.scheduledTimerWithTimeInterval(
            0.05,
            block = { _ ->
                if (isScanning) {
                    val position = device.lensPosition
                    updateScanDistanceFromLens(position)
                }
            },
            repeats = true
        )
    }

    private fun configureDevice(device: AVCaptureDevice) {
        try {
            device.lockForConfiguration(null)

            if (device.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                device.focusMode = AVCaptureFocusModeContinuousAutoFocus
                val currentLensPosition = device.lensPosition
                updateScanDistanceFromLens(currentLensPosition)
            }
            if (device.isFocusPointOfInterestSupported()) {
                device.focusPointOfInterest = CGPointMake(0.5, 0.5)
            }
            if (device.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
                device.exposureMode = AVCaptureExposureModeContinuousAutoExposure
            }
            if (device.isAutoFocusRangeRestrictionSupported()) {
                device.autoFocusRangeRestriction = AVCaptureAutoFocusRangeRestrictionNear
            }

            device.unlockForConfiguration()
        } catch (e: Exception) {
            device.unlockForConfiguration()
        }
    }

    private fun updateScanDistanceFromLens(lensPosition: Float) {
        val newDistance = when {
            lensPosition < 0.3F -> {
                CameraPositionDistance.TOO_CLOSE
            }

            lensPosition > 0.7F -> {
                CameraPositionDistance.TOO_FAR
            }

            else -> {
                CameraPositionDistance.OPTIMAL
            }
        }

        if (_scanDistance.value != newDistance) {
            dispatch_async(dispatch_get_main_queue()) {
                _scanDistance.value = newDistance
            }
        }


        if (_scanDistance.value != newDistance) {
            dispatch_async(dispatch_get_main_queue()) {
                _scanDistance.value = newDistance
            }
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
                    AVMetadataObjectTypeUPCECode,
                    AVMetadataObjectTypeDataMatrixCode,
                    AVMetadataObjectTypeAztecCode,
                    AVMetadataObjectTypePDF417Code,
                    AVMetadataObjectTypeITF14Code,
                    AVMetadataObjectTypeCodabarCode,
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
        monitoringTimer?.invalidate()
        monitoringTimer = null
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