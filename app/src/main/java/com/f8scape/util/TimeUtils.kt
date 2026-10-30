package com.f8scape.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    fun formatClockTime(timestampMs: Long, is24Hour: Boolean = true): String {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestampMs))
    }

    fun formatDate(timestampMs: Long): String {
        return SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(timestampMs))
    }

    fun formatIsoTimestamp(timestampMs: Long): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date(timestampMs))
    }

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
