package io.github.ismoy.belzspeedscan.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARNING, ERROR
}

data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

interface Logger {
    fun verbose(tag: String, message: String, throwable: Throwable? = null)
    fun debug(tag: String, message: String, throwable: Throwable? = null)
    fun info(tag: String, message: String, throwable: Throwable? = null)
    fun warning(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
    
    val logs: StateFlow<List<LogEntry>>
    fun clearLogs()
    fun setLogLevel(level: LogLevel)
}

class BelZSpeedLogger(
    private var logLevel: LogLevel = LogLevel.INFO
) : Logger {
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    
    override fun verbose(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.VERBOSE, tag, message, throwable)
    }
    
    override fun debug(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.DEBUG, tag, message, throwable)
    }
    
    override fun info(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.INFO, tag, message, throwable)
    }
    
    override fun warning(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.WARNING, tag, message, throwable)
    }
    
    override fun error(tag: String, message: String, throwable: Throwable?) {
        log(LogLevel.ERROR, tag, message, throwable)
    }
    
    override val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    override fun clearLogs() {
        _logs.value = emptyList()
    }
    
    override fun setLogLevel(level: LogLevel) {
        logLevel = level
    }
    
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal >= logLevel.ordinal) {
            val entry = LogEntry(
                timestamp = getCurrentTimeMillis(),
                level = level,
                tag = tag,
                message = message,
                throwable = throwable
            )
            
            _logs.value = _logs.value + entry
            
            // Keep only last 1000 logs to prevent memory issues
            if (_logs.value.size > 1000) {
                _logs.value = _logs.value.takeLast(1000)
            }
        }
    }
}

object LoggerFactory {
    private var logger: Logger = BelZSpeedLogger()
    
    fun getLogger(): Logger = logger
    
    fun setLogger(newLogger: Logger) {
        logger = newLogger
    }
    
    fun createLogger(logLevel: LogLevel = LogLevel.INFO): Logger {
        return BelZSpeedLogger(logLevel)
    }
}

// Extension functions for easier logging
fun Logger.scanner(message: String, throwable: Throwable? = null) {
    info("Scanner", message, throwable)
}

fun Logger.camera(message: String, throwable: Throwable? = null) {
    info("Camera", message, throwable)
}

fun Logger.security(message: String, throwable: Throwable? = null) {
    warning("Security", message, throwable)
}

fun Logger.permission(message: String, throwable: Throwable? = null) {
    info("Permission", message, throwable)
} 