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

    fun formatRelativeTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return ""
        val diffMs = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
        val diffMinutes = diffMs / (60 * 1000)
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))
        }
    }
}
