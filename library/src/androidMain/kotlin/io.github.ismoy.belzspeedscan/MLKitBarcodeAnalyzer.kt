package io.github.ismoy.belzspeedscan

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
class MLKitBarcodeAnalyzer(
    private val onCodeScanned: (String) -> Unit,
    private val isQRScanning: Boolean
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var lastScannedCode: String? = null
    private var lastScannedTime: Long = 0
    private val SCAN_DELAY = 2000L
    private var consecutiveReadings = 0
    private val REQUIRED_CONSECUTIVE_READINGS = 2
    private val MAX_CONSECUTIVE_FAILURES = 3
    private var consecutiveFailures = 0

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
                    val currentTime = System.currentTimeMillis()
                    var validBarcodeFound = false

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
                            barcode.rawValue?.let { value ->
                                validBarcodeFound = true

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

                    if (!validBarcodeFound) {
                        consecutiveFailures++
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            lastScannedCode = null
                            consecutiveReadings = 0
                            consecutiveFailures = 0
                        }
                    } else {
                        consecutiveFailures = 0
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MLKitAnalyzer", "Error en el escaneo", e)
                    consecutiveFailures++
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}