package com.f8scape.cards

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.f8scape.util.F8Log

data class CalendarEventInfo(
    val title: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val isAllDay: Boolean = false,
    val location: String? = null
)

class CalendarDataProvider(private val context: Context) {

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getNextUpcomingEvent(maskTitleForPrivacy: Boolean = false): CalendarEventInfo? {
        if (!hasCalendarPermission()) {
            return null
        }

        val now = System.currentTimeMillis()
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DELETED} = 0"
        val selectionArgs = arrayOf(now.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 1"

        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            if (cursor != null && cursor.moveToFirst()) {
                val rawTitle = cursor.getString(0) ?: "Event"
                val dtStart = cursor.getLong(1)
                val dtEnd = cursor.getLong(2)
                val isAllDay = cursor.getInt(3) == 1
                val location = cursor.getString(4)

                val displayTitle = if (maskTitleForPrivacy) "Upcoming Event" else rawTitle

                CalendarEventInfo(
                    title = displayTitle,
                    startTimeMs = dtStart,
                    endTimeMs = dtEnd,
                    isAllDay = isAllDay,
                    location = if (maskTitleForPrivacy) null else location
                )
            } else {
                null
            }
        } catch (e: Exception) {
            F8Log.e("CalendarDataProvider", "Failed to query calendar", e)
            null
        } finally {
            cursor?.close()
        }
    }
}
