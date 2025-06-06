package io.github.ismoy.belzspeedscan.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/** Simple [AnalyticsLogger] implementation that forwards events to Firebase Analytics. */
class FirebaseAnalyticsLogger(private val firebaseAnalytics: FirebaseAnalytics) : AnalyticsLogger {
    override fun logEvent(event: AnalyticsEvent) {
        val name: String
        val params = Bundle()
        when (event) {
            AnalyticsEvent.ScanStarted -> {
                name = "scan_started"
            }
            is AnalyticsEvent.ScanSucceeded -> {
                name = "scan_succeeded"
                params.putLong("duration", event.durationMillis)
            }
            is AnalyticsEvent.ScanError -> {
                name = "scan_error"
                params.putString("message", event.message)
            }
            is AnalyticsEvent.SecurityAlert -> {
                name = "security_alert"
                params.putString("reason", event.reason)
            }
        }
        firebaseAnalytics.logEvent(name, params)
    }
}
