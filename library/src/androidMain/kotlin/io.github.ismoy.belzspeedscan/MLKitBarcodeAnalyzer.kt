package io.github.ismoy.belzspeedscan

import android.annotation.SuppressLint
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance

class MLKitBarcodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
    private val isQRScanning: Boolean,
    private val onDistanceChanged: (CameraPositionDistance) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private val SCAN_DELAY = 2000L
    private var consecutiveReadings = 0
    private val REQUIRED_CONSECUTIVE_READINGS = 2
    private val MAX_CONSECUTIVE_FAILURES = 3
    private var consecutiveFailures = 0

    private var lastDistances = mutableListOf<CameraPositionDistance>()
    private val DISTANCE_BUFFER_SIZE = 3

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
                    val imageWidth = imageProxy.width.toFloat()
                    val imageHeight = imageProxy.height.toFloat()
                    val currentTime = System.currentTimeMillis()
                    var validBarcodeFound = false

                    for (barcode in barcodes) {
                        barcode.boundingBox?.let { bounds ->
                            if (bounds.left <= 0 || bounds.top <= 0 ||
                                bounds.right >= imageWidth || bounds.bottom >= imageHeight) {
                                updateDistance(CameraPositionDistance.TOO_CLOSE)
                                return@addOnSuccessListener
                            }
                        }
                    }

                    if (barcodes.isEmpty()) {
                        val brightness = calculateAverageImageBrightness(mediaImage)
                        if (brightness < 50) {
                            updateDistance(CameraPositionDistance.TOO_CLOSE)
                        } else {
                            updateDistance(CameraPositionDistance.TOO_FAR)
                        }
                        return@addOnSuccessListener
                    }

                    for (barcode in barcodes) {

                        val format = barcode.format
                        val isValidFormat = if (isQRScanning) {
                            format == Barcode.FORMAT_QR_CODE
                        } else {
                            format in listOf(
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
                                Barcode.FORMAT_CODABAR
                            )
                        }

                        if (isValidFormat) {
                            barcode.boundingBox?.let { bounds ->
                                val codeArea = bounds.width() * bounds.height()
                                val imageArea = imageProxy.width * imageProxy.height
                                val areaRatio = codeArea.toFloat() / imageArea
                                val distance = when {
                                    areaRatio > 0.50f -> CameraPositionDistance.TOO_CLOSE
                                    areaRatio < 0.05f -> CameraPositionDistance.TOO_FAR
                                    else -> CameraPositionDistance.OPTIMAL
                                }
                                updateDistance(distance)
                            }

                            barcode.rawValue?.let { value ->
                                validBarcodeFound = true
                                if (getSmoothedDistance() == CameraPositionDistance.OPTIMAL) {
                                    if (value == lastScannedCode) {
                                        consecutiveReadings++
                                        if (consecutiveReadings >= REQUIRED_CONSECUTIVE_READINGS &&
                                            (currentTime - lastScannedTime) > SCAN_DELAY
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
                        }
                    }

                    if (!validBarcodeFound) {
                        resetScanningState()
                    }
                }
                .addOnFailureListener { e ->
                    resetScanningState()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    private fun calculateAverageImageBrightness(image: Image): Int {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        var total = 0
        for (byte in data) {
            total += byte.toInt() and 0xFF
        }
        return total / data.size
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
        consecutiveFailures++
        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
            lastScannedCode = null
            consecutiveReadings = 0
            consecutiveFailures = 0
            lastDistances.clear()
            updateDistance(CameraPositionDistance.TOO_FAR)
        }
    }
}