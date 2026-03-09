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
    TODO("Not yet implemented")
}