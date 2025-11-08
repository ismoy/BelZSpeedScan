package io.github.ismoy.belzspeedscan.utils

import kotlin.js.Date

actual fun getCurrentTimeMillis(): Long {
    return Date.now().toLong()
}
