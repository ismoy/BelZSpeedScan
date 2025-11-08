package io.github.ismoy.belzspeedscan

import io.github.ismoy.belzspeedscan.config.ScannerConfig
import io.github.ismoy.belzspeedscan.domain.CodeScanner
import io.github.ismoy.belzspeedscan.domain.ScannerEventManager
import io.github.ismoy.belzspeedscan.state.DefaultScannerStateManager

class ScannerBuilder {
    private var config: ScannerConfig = ScannerConfig.default()
    private var eventManager: ScannerEventManager = ScannerEventManager()
    private var stateManager: DefaultScannerStateManager = DefaultScannerStateManager()

    fun withConfig(config: ScannerConfig): ScannerBuilder {
        this.config = config
        return this
    }
    
    fun withCustomConfig(block: ScannerConfig.() -> ScannerConfig): ScannerBuilder {
        this.config = block(ScannerConfig())
        return this
    }
    
    fun withEventManager(eventManager: ScannerEventManager): ScannerBuilder {
        this.eventManager = eventManager
        return this
    }
    
    fun withStateManager(stateManager: DefaultScannerStateManager): ScannerBuilder {
        this.stateManager = stateManager
        return this
    }
    
    fun build(
        context: Any?,
        lifecycleOwner: Any?,
        previewView: Any
    ): CodeScanner {
        return createBelSpeedScanCodeScanner(
            context = context,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            config = config,
            eventManager = eventManager,
            stateManager = stateManager
        )
    }
}

fun createScanner(
    context: Any?,
    lifecycleOwner: Any?,
    previewView: Any,
    block: ScannerBuilder.() -> Unit = {}
): CodeScanner {
    return ScannerBuilder().apply(block).build(context, lifecycleOwner, previewView)
}
