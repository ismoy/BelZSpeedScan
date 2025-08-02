import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.domain.ScannerEvent
import io.github.ismoy.belzspeedscan.domain.ScannerEventManager
import io.github.ismoy.belzspeedscan.state.DefaultScannerStateManager
import io.github.ismoy.belzspeedscan.state.ScannerState
import io.github.ismoy.belzspeedscan.utils.LoggerFactory
import io.github.ismoy.belzspeedscan.utils.camera
import io.github.ismoy.belzspeedscan.utils.currentTimeMillis
import io.github.ismoy.belzspeedscan.utils.scanner
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
    private val config: ScannerConfig,
    private val eventManager: ScannerEventManager,
    private val stateManager: DefaultScannerStateManager
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
    private var lastMaliciousCode: String? = null
    private var lastMaliciousTime: Long = 0
    
    private val logger = LoggerFactory.getLogger()

    init {
        if (config.playSound) {
            setupAudioPlayer()
        }
        logger.scanner("IOSScanner initialized with config: ${config.watermark}")
    }

    private fun setupAudioPlayer() {
        try {
            logger.scanner("Setting up audio player...")

            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionMixWithOthers,
                error = null
            )
            audioSession.setActive(true, error = null)
            logger.scanner("Audio session configured successfully")

            val bundlePath = NSBundle.mainBundle.bundlePath
            val resourcePath = "$bundlePath/compose-resources/${config.soundResourceName}.${config.soundResourceExtension}"
            logger.scanner("Searching for file in: $resourcePath")

            val soundUrl = NSURL.fileURLWithPath(resourcePath)
            logger.scanner("URL of the created file: ${soundUrl.absoluteString}")

            audioPlayer = AVAudioPlayer(contentsOfURL = soundUrl, error = null)

            audioPlayer?.let { player ->
                if (player.prepareToPlay()) {
                    logger.scanner("Player prepared correctly")
                    player.volume = 1.0f
                    player.numberOfLoops = 0

                    if (player.play()) {
                        logger.scanner("Sound check successful")
                        player.stop()
                        player.currentTime = 0.0
                    } else {
                        logger.error("Audio", "Error: Sound could not be played in the test")
                    }
                } else {
                    logger.error("Audio", "Error: Failed to prepare player")
                }
            } ?: logger.error("Audio", "Error: Failed to create audio player")

        } catch (e: Exception) {
            logger.error("Audio", "Error in AudioPlayer setup: ${e.message}", e)
        }
    }

    private fun playBeepSound() {
        if (config.playSound) {
            try {
                audioPlayer?.let { player ->
                    if (!player.playing) {
                        player.currentTime = 0.0
                        player.play()
                    }
                }

                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
            } catch (e: Exception) {
                logger.error("Audio", "Error playing sound: ${e.message}", e)
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
            if (!isScanning) {
                logger.scanner("MetadataDelegate: Scanner inactive, skipping processing")
                return
            }

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
                        if (timeSinceLastScan > config.delayToNextScan) {
                            lastScannedCode = code
                            lastScannedTime = currentTime
                            lastScannedTimes[code] = currentTime
                            if (isScanning) {
                                playBeepSound()
                                eventManager.emitEvent(ScannerEvent.CodeScanned(code))
                                stateManager.updateState(ScannerState.CodeDetected(code))
                                logger.scanner("Code scan successfully: $code")
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
            logger.error("Camera", "Error converting to view coordinates: ${e.message}", e)
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
            logger.camera("Setting up camera...")
            val newSession = AVCaptureSession()
            captureSession = newSession
            newSession.sessionPreset = AVCaptureSessionPresetHigh

            val device = getCameraDevice()
            if (device == null) {
                val errorMessage = "Cant find camera device"
                logger.error("Camera", errorMessage)
                eventManager.emitEvent(ScannerEvent.CameraError(errorMessage))
                stateManager.updateState(ScannerState.Error(errorMessage))
                return
            }

            configureDevice(device)
            setupCameraObserver(device)

            val input = AVCaptureDeviceInput(device = device, error = null)

            if (newSession.canAddInput(input)) {
                newSession.addInput(input)
            } else {
                val errorMessage = "Error to add input to session"
                logger.error("Camera", errorMessage)
                eventManager.emitEvent(ScannerEvent.CameraError(errorMessage))
                stateManager.updateState(ScannerState.Error(errorMessage))
                return
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
            
            logger.camera("Camera setup completed successfully")
        } catch (e: Exception) {
            val errorMessage = "Error to setup camera: ${e.message ?: "Error desconocido"}"
            logger.error("Camera", errorMessage, e)
            eventManager.emitEvent(ScannerEvent.CameraError(errorMessage))
            stateManager.updateState(ScannerState.Error(errorMessage))
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
            val errorMessage = "Error to configure camera: ${e.message ?: "Error unknow"}"
            logger.error("Camera", errorMessage, e)
            eventManager.emitEvent(ScannerEvent.CameraError(errorMessage))
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
                eventManager.emitEvent(ScannerEvent.DistanceChanged(newDistance))
            }
        }
    }
    
    private fun setupMetadataOutput(session: AVCaptureSession) {
        val metadataOutput = AVCaptureMetadataOutput()
        if (session.canAddOutput(metadataOutput)) {
            session.addOutput(metadataOutput)

            metadataOutput.metadataObjectTypes = listOf(
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
                AVMetadataObjectTypeQRCode
            )

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
        logger.scanner("Starting scanning")
        isScanning = true
        stateManager.updateState(ScannerState.StartingCamera)
        eventManager.emitEvent(ScannerEvent.ScanningStarted)
        
        if (captureSession == null) {
            setupCamera()
        } else {
            dispatch_async(sessionQueue) {
                this.captureSession?.startRunning()
            }
        }
        
        stateManager.updateState(ScannerState.CameraReady)
        stateManager.updateState(ScannerState.Scanning)
    }

    override fun stopScanning() {
        logger.scanner("Stopping scanning - iOS")
        isScanning = false
        
        // Stop audio
        audioPlayer?.stop()
        audioPlayer = null
        
        // Stop monitoring timer
        monitoringTimer?.invalidate()
        monitoringTimer = null
        
        // Stop capture session completely
        dispatch_async(sessionQueue) {
            this.captureSession?.stopRunning()
            this.captureSession = null
        }
        
        // Clear UI elements
        dispatch_async(dispatch_get_main_queue()) {
            clearAllHighlights()
            previewLayer?.removeFromSuperlayer()
            previewLayer = null
        }
        
        // Clear all references
        metadataDelegate = null
        
        stateManager.updateState(ScannerState.CameraReady)
        eventManager.emitEvent(ScannerEvent.ScanningStopped)
        logger.scanner("Scanner stopped completely - camera fully disabled")
    }

    private fun clearAllHighlights() {
        currentHighlightLayers.values.forEach { it.removeFromSuperlayer() }
        currentHighlightLayers.clear()
        lastScannedTimes.clear()
    }

    override fun pauseScanning() {
        logger.scanner("Pausing scanning")
        isScanning = false
        dispatch_async(sessionQueue) {
            this.captureSession?.stopRunning()
        }
        dispatch_async(dispatch_get_main_queue()) {
            clearAllHighlights()
            previewLayer?.setHidden(true)
        }
        stateManager.updateState(ScannerState.Paused)
        eventManager.emitEvent(ScannerEvent.ScanningPaused)
    }

    override fun resumeScanning() {
        logger.scanner("Resuming scanning")
        dispatch_async(dispatch_get_main_queue()) {
            previewLayer?.setHidden(false)
        }
        dispatch_async(sessionQueue) {
            this.captureSession?.startRunning()
            dispatch_async(dispatch_get_main_queue()) {
                isScanning = true
            }
        }
        stateManager.updateState(ScannerState.Scanning)
        eventManager.emitEvent(ScannerEvent.ScanningResumed)
    }
}