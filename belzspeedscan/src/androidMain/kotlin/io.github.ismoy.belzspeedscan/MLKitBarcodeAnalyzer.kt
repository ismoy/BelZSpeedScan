package io.github.ismoy.belzspeedscan

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.domain.ScannerEvent
import io.github.ismoy.belzspeedscan.domain.ScannerEventManager
import io.github.ismoy.belzspeedscan.state.DefaultScannerStateManager
import io.github.ismoy.belzspeedscan.utils.LoggerFactory
import io.github.ismoy.belzspeedscan.utils.scanner

class MLKitBarcodeAnalyzer(
    private val config: ScannerConfig,
    private val eventManager: ScannerEventManager,
    private val stateManager: DefaultScannerStateManager,
    private val isScannerActive: () -> Boolean,
    private val onDistanceChanged: (CameraPositionDistance) -> Unit,
    private val onPlaySound: () -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private var consecutiveReadings = 0
    private var lastDistances = mutableListOf<CameraPositionDistance>()
    private val MALICIOUS_CODE_COOLDOWN = config.maliciousCodeCooldown
    private var lastMaliciousCode: String? = null
    private var lastMaliciousTime: Long = 0
    
    private val logger = LoggerFactory.getLogger()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        // Check if scanner is active before processing
        if (!isScannerActive()) {
            logger.scanner("Analyzer: Scanner inactive, skipping frame")
            imageProxy.close()
            return
        }
        
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    // Double check if scanner is still active
                    if (!isScannerActive()) {
                        logger.scanner("Analyzer: Scanner became inactive during processing")
                        return@addOnSuccessListener
                    }
                    
                    if (barcodes.isEmpty()) {
                        updateDistance(CameraPositionDistance.TOO_FAR)
                        return@addOnSuccessListener
                    }

                    processValidBarcodes(barcodes, imageProxy, System.currentTimeMillis())
                }
                .addOnFailureListener {
                    logger.error("MLKit", "Error processing barcode", it)
                    resetScanningState()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processValidBarcodes(barcodes: List<Barcode>, imageProxy: ImageProxy, currentTime: Long) {
        for (barcode in barcodes) {
            if (isValidBarcodeFormat(barcode)) {
                processBarcodeDistance(barcode, imageProxy)
                processBarcodeValue(barcode, currentTime)
            }
        }
    }

    private fun isValidBarcodeFormat(barcode: Barcode): Boolean {
        val validFormat = barcode.format in listOf(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_QR_CODE,
        )
        val rawValue = barcode.rawValue ?: return false

        if (rawValue.length > config.maxCodeLength) return false
        return validFormat
    }

    private fun processBarcodeDistance(barcode: Barcode, imageProxy: ImageProxy) {
        barcode.boundingBox?.let { bounds ->
            val codeArea = bounds.width() * bounds.height()
            val imageArea = imageProxy.width * imageProxy.height
            val areaRatio = codeArea.toFloat() / imageArea

            val distance = when {
                areaRatio < config.areaRatioThreshold -> CameraPositionDistance.TOO_FAR
                else -> CameraPositionDistance.OPTIMAL
            }
            updateDistance(distance)
        }
    }

    private fun processBarcodeValue(barcode: Barcode, currentTime: Long) {
        barcode.rawValue?.let { value ->
            processBarcodeNormalValue(value, currentTime)
        }
    }
    
    private fun processBarcodeNormalValue(value: String, currentTime: Long) {
        if (getSmoothedDistance() == CameraPositionDistance.OPTIMAL) {
            if (value == lastScannedCode) {
                consecutiveReadings++
                if (consecutiveReadings >= config.requiredConsecutiveReadings &&
                    (currentTime - lastScannedTime) > config.delayToNextScan
                ) {
                    lastScannedTime = currentTime
                    onPlaySound()
                    eventManager.emitEvent(ScannerEvent.CodeScanned(value))
                    stateManager.updateState(io.github.ismoy.belzspeedscan.state.ScannerState.CodeDetected(value))
                    consecutiveReadings = 0
                    logger.scanner("Code scan successfully: $value")
                }
            } else {
                consecutiveReadings = 1
                lastScannedCode = value
            }
        }
    }

    private fun updateDistance(newDistance: CameraPositionDistance) {
        lastDistances.add(newDistance)
        if (lastDistances.size > config.distanceBufferSize) {
            lastDistances.removeAt(0)
        }
        onDistanceChanged(getSmoothedDistance())
    }

    private fun getSmoothedDistance(): CameraPositionDistance {
        if (lastDistances.isEmpty()) return CameraPositionDistance.TOO_FAR
        val counts = lastDistances.groupingBy { it }.eachCount()
        return counts.maxByOrNull { it.value }?.key ?: CameraPositionDistance.TOO_FAR
    }

    private fun resetScanningState() {
        lastScannedCode = null
        consecutiveReadings = 0
        lastDistances.clear()
        updateDistance(CameraPositionDistance.TOO_FAR)
    }
}