package io.github.ismoy.belzspeedscan

import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.domain.ScannerEventManager
import io.github.ismoy.belzspeedscan.state.DefaultScannerStateManager

actual fun createBelSpeedScanCodeScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    config: ScannerConfig,
    eventManager: ScannerEventManager,
    stateManager: DefaultScannerStateManager
): CodeScanner {
    return object : CodeScanner {
        override fun startScanning() {
            // JVM implementation placeholder
        }

        override fun stopScanning() {
            // JVM implementation placeholder
        }

        override fun pauseScanning() {
            // JVM implementation placeholder
        }

        override fun resumeScanning() {
            // JVM implementation placeholder
        }
    }
}