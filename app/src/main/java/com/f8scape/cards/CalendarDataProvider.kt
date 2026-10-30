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

    /**
     * Retrieves all events in the specified month grouped by day of month (1..31).
     */
    fun getEventsForMonth(year: Int, month: Int, maskTitleForPrivacy: Boolean = false): Map<Int, List<CalendarEventInfo>> {
        if (!hasCalendarPermission()) return emptyMap()

        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        cal.add(java.util.Calendar.MONTH, 1)
        val endMs = cal.timeInMillis

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "(${CalendarContract.Events.DTSTART} < ?) AND (${CalendarContract.Events.DTEND} >= ? OR ${CalendarContract.Events.DTSTART} >= ?) AND ${CalendarContract.Events.DELETED} = 0"
        val selectionArgs = arrayOf(endMs.toString(), startMs.toString(), startMs.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val resultMap = mutableMapOf<Int, MutableList<CalendarEventInfo>>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            val eventCal = java.util.Calendar.getInstance()
            while (cursor != null && cursor.moveToNext()) {
                val rawTitle = cursor.getString(0) ?: "Event"
                val dtStart = cursor.getLong(1)
                val dtEnd = cursor.getLong(2)
                val isAllDay = cursor.getInt(3) == 1
                val location = cursor.getString(4)

                eventCal.timeInMillis = dtStart
                val eventMonth = eventCal.get(java.util.Calendar.MONTH)
                val eventYear = eventCal.get(java.util.Calendar.YEAR)
                if (eventYear == year && eventMonth == month) {
                    val day = eventCal.get(java.util.Calendar.DAY_OF_MONTH)
                    val info = CalendarEventInfo(
                        title = if (maskTitleForPrivacy) "Upcoming Event" else rawTitle,
                        startTimeMs = dtStart,
                        endTimeMs = dtEnd,
                        isAllDay = isAllDay,
                        location = if (maskTitleForPrivacy) null else location
                    )
                    resultMap.getOrPut(day) { mutableListOf() }.add(info)
                }
            }
        } catch (e: Exception) {
            F8Log.e("CalendarDataProvider", "Failed to query month events", e)
        } finally {
            cursor?.close()
        }
        return resultMap
    }

    /**
     * Retrieves all events for a specific day.
     */
    fun getEventsForDay(year: Int, month: Int, day: Int, maskTitleForPrivacy: Boolean = false): List<CalendarEventInfo> {
        if (!hasCalendarPermission()) return emptyList()

        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month)
            set(java.util.Calendar.DAY_OF_MONTH, day)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endMs = cal.timeInMillis

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "(${CalendarContract.Events.DTSTART} < ?) AND (${CalendarContract.Events.DTEND} >= ? OR ${CalendarContract.Events.DTSTART} >= ?) AND ${CalendarContract.Events.DELETED} = 0"
        val selectionArgs = arrayOf(endMs.toString(), startMs.toString(), startMs.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val list = mutableListOf<CalendarEventInfo>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            while (cursor != null && cursor.moveToNext()) {
                val rawTitle = cursor.getString(0) ?: "Event"
                val dtStart = cursor.getLong(1)
                val dtEnd = cursor.getLong(2)
                val isAllDay = cursor.getInt(3) == 1
                val location = cursor.getString(4)

                list.add(
                    CalendarEventInfo(
                        title = if (maskTitleForPrivacy) "Upcoming Event" else rawTitle,
                        startTimeMs = dtStart,
                        endTimeMs = dtEnd,
                        isAllDay = isAllDay,
                        location = if (maskTitleForPrivacy) null else location
                    )
                )
            }
        } catch (e: Exception) {
            F8Log.e("CalendarDataProvider", "Failed to query day events", e)
        } finally {
            cursor?.close()
        }
        return list
    }
}
