package io.github.ismoy.belzspeedscan.core

fun isValidDataPattern(data: String): Boolean {
    if (data.isBlank()) return false

    if (data.length > 2000) return false


    val standardPattern = Regex("^[A-Za-z0-9\\-_.:/+\\s]*$")
    if (standardPattern.matches(data)) {
        return true
    }

    val validUrlPattern = Regex("^(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?$", RegexOption.IGNORE_CASE)
    val isValidUrl = validUrlPattern.matches(data)
    if (isValidUrl) {
        val suspiciousDomains = listOf(
            ".ru", ".cn", ".xyz", ".top", ".cc", ".tk", ".ml", ".ga", ".cf",
            "bit.ly", "goo.gl", "tinyurl.com", "t.co", "is.gd", "cli.gs", "ow.ly"
        )
        val hasSuspiciousDomain = suspiciousDomains.any { data.contains(it, ignoreCase = true) }
        return !hasSuspiciousDomain
    }

    val suspiciousChars = setOf(
        '%', '$', '´', '`', '^', '~', '{', '}', '[', ']', '|', '<', '>',
        '\\', '©', '®', '™', '℠', '§', '¶', '†', '‡', '±', '¿', '¡', '«', '»'
    )
    val containsSuspiciousChars = data.any { it in suspiciousChars }

    val dangerousCommandChars = setOf('&', ';', '|', '`', '#')
    val containsDangerousCommandChars = data.any { it in dangerousCommandChars }

    val scriptPatterns = listOf(
        "<script", "javascript:", "onerror=", "onclick=", "onload=", "eval(",
        "document.cookie", "window.location", "fetch(", "setTimeout(", "setInterval(",
        "ajax", "xhr", "XMLHttpRequest", "$.post", "$.get", "$(document)", "alert("
    )
    val containsScriptPatterns = scriptPatterns.any { pattern ->
        data.contains(pattern, ignoreCase = true)
    }

    val sqlInjectionPatterns = listOf(
        "SELECT ", "INSERT ", "UPDATE ", "DELETE ", "DROP ", "UNION ",
        "1=1", "OR 1=1", "' OR '", "' OR 1=1", "--", "/*", "*/"
    )
    val containsSqlInjection = sqlInjectionPatterns.any { pattern ->
        data.contains(pattern, ignoreCase = true)
    }

    val encodingPatterns = listOf(
        "\\u", "&#", "%u", "%3C", "%3E", "%22", "%27", "%28", "%29"
    )
    val containsEncodedContent = encodingPatterns.any { pattern ->
        data.contains(pattern, ignoreCase = true)
    }

    val hasAbnormalCombinations = hasAbnormalCharacterCombinations(data)

    val allSpecialChars = "!@#$%^&*()+={}[]<>~`'\"|\\;:,./?"
    val specialCharCount = data.count { it in allSpecialChars }
    val specialCharRatio = specialCharCount.toFloat() / data.length
    val tooManySpecialChars = specialCharRatio > 0.15

    return !(containsSuspiciousChars ||
            containsDangerousCommandChars ||
            containsScriptPatterns ||
            containsSqlInjection ||
            containsEncodedContent ||
            hasAbnormalCombinations ||
            tooManySpecialChars)
}

private fun hasAbnormalCharacterCombinations(data: String): Boolean {
    val abnormalCombinations = listOf(
        "\\x", "\\'", "\\\"", "\\n", "\\r", "\\t", "\\b", "\\f",
        "http/", "https/", "www/", ":´", "%$"
    )

    return abnormalCombinations.any { data.contains(it) }
}
 fun determineReason(value: String): String {
    return when {
        value.contains("<script", ignoreCase = true) -> "Contiene código JavaScript sospechoso"
        value.length > 2000 -> "Longitud del código excesiva"
        else -> "Patrón inusual detectado"
    }
}