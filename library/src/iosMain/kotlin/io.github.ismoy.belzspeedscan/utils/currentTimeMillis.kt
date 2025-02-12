package io.github.ismoy.belzspeedscan.utils

import platform.Foundation.NSDate
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970

fun currentTimeMillis(): Long {
    return (NSDate.date().timeIntervalSince1970 * 1000).toLong()
}