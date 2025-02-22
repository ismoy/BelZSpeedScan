package io.github.ismoy.belzspeedscan

import android.annotation.SuppressLint
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.ismoy.belzspeedscan.data.model.BrightnessInfo
import io.github.ismoy.belzspeedscan.data.model.CameraPositionDistance

class MLKitBarcodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
    private val isQRScanning: Boolean,
    private val onDistanceChanged: (CameraPositionDistance) -> Unit,
    private val delayToNextScan:Long
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private var consecutiveReadings = 0
    private val REQUIRED_CONSECUTIVE_READINGS = 2
    private val MAX_CONSECUTIVE_FAILURES = 3
    private var consecutiveFailures = 0

    private var lastDistances = mutableListOf<CameraPositionDistance>()
    private val DISTANCE_BUFFER_SIZE = 3

    private val BRIGHTNESS_SAMPLES = 9
    private val BRIGHTNESS_THRESHOLD_LOW = 40
    private val BRIGHTNESS_THRESHOLD_HIGH = 215
    private val CONTRAST_THRESHOLD = 30
    private var lastBrightnessReadings = mutableListOf<Int>()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            // Analizar el brillo y contraste de la imagen
            val brightnessInfo = analyzeImageBrightness(mediaImage)

            // Ajustar la imagen si es necesario
            val adjustedImage = if (needsImageAdjustment(brightnessInfo)) {
                adjustImageForTape(mediaImage)
            } else {
                mediaImage
            }

            val image = InputImage.fromMediaImage(
                adjustedImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val imageWidth = imageProxy.width.toFloat()
                    val imageHeight = imageProxy.height.toFloat()
                    for (barcode in barcodes) {
                        barcode.boundingBox?.let { bounds ->
                            if (isCodeTooCloseToEdge(bounds, imageWidth, imageHeight)) {
                                updateDistance(CameraPositionDistance.TOO_CLOSE)
                                return@addOnSuccessListener
                            }
                        }
                    }

                    if (barcodes.isEmpty()) {
                        handleNoBarcodes(brightnessInfo)
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

    private fun analyzeImageBrightness(image: Image): BrightnessInfo {
        val buffer = image.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)

        val samplePoints = getSamplePoints(data.size, image.width)
        val brightnessSamples = samplePoints.map { point ->
            data[point].toInt() and 0xFF
        }

        val averageBrightness = brightnessSamples.average().toInt()
        val contrast = brightnessSamples.maxOrNull()!! - brightnessSamples.minOrNull()!!

        lastBrightnessReadings.add(averageBrightness)
        if (lastBrightnessReadings.size > BRIGHTNESS_SAMPLES) {
            lastBrightnessReadings.removeAt(0)
        }

        return BrightnessInfo(
            averageBrightness = averageBrightness,
            contrast = contrast,
            hasReflection = detectReflection(brightnessSamples)
        )
    }

    private fun getSamplePoints(dataSize: Int, imageWidth: Int): List<Int> {
        val rowStride = imageWidth
        val points = mutableListOf<Int>()
        val rows = dataSize / rowStride

        for (i in 1..3) {
            for (j in 1..3) {
                val point = (rows / 4 * i) * rowStride + (imageWidth / 4 * j)
                if (point < dataSize) points.add(point)
            }
        }
        return points
    }

    private fun detectReflection(brightnessSamples: List<Int>): Boolean {
        val brightPixels = brightnessSamples.count { it > BRIGHTNESS_THRESHOLD_HIGH }
        return brightPixels > brightnessSamples.size / 3
    }

    private fun needsImageAdjustment(brightnessInfo: BrightnessInfo): Boolean {
        return brightnessInfo.hasReflection ||
                brightnessInfo.averageBrightness < BRIGHTNESS_THRESHOLD_LOW ||
                brightnessInfo.averageBrightness > BRIGHTNESS_THRESHOLD_HIGH ||
                brightnessInfo.contrast < CONTRAST_THRESHOLD
    }

    private fun adjustImageForTape(image: Image): Image {
        return image
    }

    private fun isCodeTooCloseToEdge(bounds: android.graphics.Rect, width: Float, height: Float): Boolean {
        val margin = 0.05f
        return bounds.left < width * margin ||
                bounds.top < height * margin ||
                bounds.right > width * (1 - margin) ||
                bounds.bottom > height * (1 - margin)
    }

    private fun handleNoBarcodes(brightnessInfo: BrightnessInfo) {
        when {
            brightnessInfo.hasReflection -> updateDistance(CameraPositionDistance.ADJUST_ANGLE)
            brightnessInfo.averageBrightness < BRIGHTNESS_THRESHOLD_LOW -> updateDistance(CameraPositionDistance.TOO_DARK)
            brightnessInfo.averageBrightness > BRIGHTNESS_THRESHOLD_HIGH -> updateDistance(CameraPositionDistance.TOO_BRIGHT)
            else -> updateDistance(CameraPositionDistance.TOO_FAR)
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
        return if (isQRScanning) {
            barcode.format == Barcode.FORMAT_QR_CODE
        } else {
            barcode.format in listOf(
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
    }

    private fun processBarcodeDistance(barcode: Barcode, imageProxy: ImageProxy) {
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
    }

    private fun processBarcodeValue(barcode: Barcode, currentTime: Long) {
        barcode.rawValue?.let { value ->
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