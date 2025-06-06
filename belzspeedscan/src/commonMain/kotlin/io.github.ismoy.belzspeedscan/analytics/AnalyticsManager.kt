package io.github.ismoy.belzspeedscan.analytics

/**
 * Simple analytics configuration for BelZSpeedScan.
 *
 * Tracking is disabled by default. Applications can enable it by setting
 * [enabled] to true and providing a custom [logger].
 */
object AnalyticsManager {
    /** Indicates if analytics events should be forwarded to [logger]. */
    var enabled: Boolean = false

    /** Implementation used to log analytics events. */
    var logger: AnalyticsLogger? = null

    /** Log an analytics [event] if tracking is enabled. */
    fun log(event: AnalyticsEvent) {
        if (enabled) {
            logger?.logEvent(event)
        }
    }
}

/** Types of analytics events emitted by the library. */
sealed class AnalyticsEvent {
    /** Called when scanning starts. */
    object ScanStarted : AnalyticsEvent()

    /** Called when a code has been successfully scanned. */
    data class ScanSucceeded(val durationMillis: Long) : AnalyticsEvent()

    /** Called when scanning fails with an error. */
    data class ScanError(val message: String) : AnalyticsEvent()

    /** Called when a potential malicious code is detected. */
    data class SecurityAlert(val reason: String) : AnalyticsEvent()
}

/** Interface that must be implemented to receive analytics events. */
interface AnalyticsLogger {
    fun logEvent(event: AnalyticsEvent)
}
