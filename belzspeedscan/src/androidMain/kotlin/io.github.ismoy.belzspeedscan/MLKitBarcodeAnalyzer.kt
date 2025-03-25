package io.github.ismoy.belzspeedscan

import android.annotation.SuppressLint
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.ismoy.belzspeedscan.core.determineReason
import io.github.ismoy.belzspeedscan.core.isValidDataPattern
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance
import io.github.ismoy.belzspeedscan.data.model.SecurityAlertInfo

class MLKitBarcodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
    private val onDistanceChanged: (CameraPositionDistance) -> Unit,
    private val delayToNextScan: Long,
    private val areaRatioThreshold: Float,
    private val onSecurityAlert: (SecurityAlertInfo) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private var consecutiveReadings = 0
    private val REQUIRED_CONSECUTIVE_READINGS = 2
    private var lastDistances = mutableListOf<CameraPositionDistance>()
    private val DISTANCE_BUFFER_SIZE = 3
    private val MALICIOUS_CODE_COOLDOWN = 3000L
    private var lastMaliciousCode: String? = null
    private var lastMaliciousTime: Long = 0
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isEmpty()) {
                        updateDistance(CameraPositionDistance.TOO_FAR)
                        return@addOnSuccessListener
                    }

                    processValidBarcodes(barcodes, imageProxy, System.currentTimeMillis())
                }
                .addOnFailureListener {
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
        val  validFormat = barcode.format in listOf(
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

        if (rawValue.length > 1000) return false
        return validFormat
    }

    private fun processBarcodeDistance(barcode: Barcode, imageProxy: ImageProxy) {
        barcode.boundingBox?.let { bounds ->
            val codeArea = bounds.width() * bounds.height()
            val imageArea = imageProxy.width * imageProxy.height
            val areaRatio = codeArea.toFloat() / imageArea

            val distance = when {
                areaRatio < areaRatioThreshold -> CameraPositionDistance.TOO_FAR
                else -> CameraPositionDistance.OPTIMAL
            }
            updateDistance(distance)
        }
    }

    private fun processBarcodeValue(barcode: Barcode, currentTime: Long) {
        barcode.rawValue?.let { value ->
            if (!isValidDataPattern(value)) {
                if (value == lastMaliciousCode &&
                    (currentTime - lastMaliciousTime) < MALICIOUS_CODE_COOLDOWN) {
                    return
                }
                val reason = determineReason(value)
                val alertInfo = SecurityAlertInfo(
                    message = "¡Advertencia! Se ha detectado un código potencialmente malicioso",
                    codeValue = value,
                    reason = reason
                )
                onSecurityAlert(alertInfo)

                lastMaliciousCode = value
                lastMaliciousTime = currentTime

                lastScannedCode = value
                lastScannedTime = currentTime
            } else {
                processBarcodeNormalValue(value, currentTime)
            }
        }
    }
    private fun processBarcodeNormalValue(value: String, currentTime: Long) {
        if (getSmoothedDistance() == CameraPositionDistance.OPTIMAL) {
            if (value == lastScannedCode) {
                consecutiveReadings++
                if (consecutiveReadings >= REQUIRED_CONSECUTIVE_READINGS &&
                    (currentTime - lastScannedTime) > delayToNextScan
                ) {
                    lastScannedTime = currentTime
                    onCodeScanned(value)
                    consecutiveReadings = 0
                }
            } else {
                consecutiveReadings = 1
                lastScannedCode = value
            }
        }
    }

    private fun updateDistance(newDistance: CameraPositionDistance) {
        lastDistances.add(newDistance)
        if (lastDistances.size > DISTANCE_BUFFER_SIZE) {
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