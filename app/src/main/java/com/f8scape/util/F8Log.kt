package com.f8scape.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Local in-memory rolling diagnostic log.
 * Never transmits data off-device. All entries are local-only and redactable.
 */
object F8Log {
    private const val MAX_ENTRIES = 200

    data class LogEntry(
        val timestampMs: Long,
        val tag: String,
        val message: String,
        val level: Level = Level.INFO
    ) {
        enum class Level { DEBUG, INFO, WARN, ERROR }

        fun formatted(): String {
            val date = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestampMs))
            return "[$date] [${level.name}] $tag: $message"
        }
    }

    private val entries = ConcurrentLinkedDeque<LogEntry>()

    fun d(tag: String, message: String) = log(LogEntry.Level.DEBUG, tag, message)
    fun i(tag: String, message: String) = log(LogEntry.Level.INFO, tag, message)
    fun w(tag: String, message: String) = log(LogEntry.Level.WARN, tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message: ${throwable.message}" else message
        log(LogEntry.Level.ERROR, tag, fullMsg)
    }

    private fun log(level: LogEntry.Level, tag: String, message: String) {
        val entry = LogEntry(
            timestampMs = System.currentTimeMillis(),
            tag = tag,
            message = redact(message),
            level = level
        )
        entries.add(entry)
        while (entries.size > MAX_ENTRIES) {
            entries.pollFirst()
        }
    }

    /**
     * Redacts accidental PII, serial numbers, or email strings.
     */
    private fun redact(input: String): String {
        return input
            .replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}"), "[REDACTED_EMAIL]")
            .replace(Regex("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})"), "[REDACTED_MAC]")
    }

    fun getEntries(): List<LogEntry> = entries.toList()

    fun clear() = entries.clear()
}
