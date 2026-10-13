package com.namaz.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.namaz.domain.Prayer
import com.namaz.domain.PrayerCalculator
import com.namaz.domain.PrayerMoment
import com.namaz.domain.formatClock
import java.time.LocalDate
import java.time.ZoneId

data class DeviceCalendar(
    val id: Long,
    val name: String,
    val account: String,
)

class CalendarRepository(private val context: Context) {
    fun hasPermission(): Boolean {
        val read = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        val write = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        return read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }

    fun calendars(): List<DeviceCalendar> {
        if (!hasPermission()) return emptyList()
        val result = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?"
        val args = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            args,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            while (cursor.moveToNext()) {
                result += DeviceCalendar(
                    id = cursor.getLong(idIndex),
                    name = cursor.getString(nameIndex).orEmpty().ifBlank { "Календарь" },
                    account = cursor.getString(accountIndex).orEmpty(),
                )
            }
        }
        return result
    }

    fun deleteOurs(calendarId: Long) {
        if (!hasPermission()) return
        context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events.CALENDAR_ID}=? AND ${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf(calendarId.toString(), "$MARKER%"),
        )
    }

    fun replaceWindow(settings: UserSettings, zone: ZoneId, start: LocalDate, days: Int): Int {
        val calendarId = settings.calendarId ?: return 0
        if (!hasPermission()) return 0
        val latitude = settings.latitude ?: return 0
        val longitude = settings.longitude ?: return 0
        deleteOurs(calendarId)
        var written = 0
        for (offset in 0 until days) {
            val date = start.plusDays(offset.toLong())
            val moments = PrayerCalculator.calculate(
                date = date,
                latitude = latitude,
                longitude = longitude,
                zone = zone,
                method = settings.method,
                madhab = settings.madhab,
                highLatitude = settings.highLatitude,
                offsets = settings.offsets,
            )
            moments.filter { it.prayer.isObligatory && it.time != null }.forEach { moment ->
                insert(calendarId, date, moment, zone)
                written += 1
            }
        }
        return written
    }

    private fun insert(calendarId: Long, date: LocalDate, moment: PrayerMoment, zone: ZoneId) {
        val start = moment.time ?: return
        val end = start.plusMinutes(15)
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, "Намаз — ${moment.prayer.title}")
            put(
                CalendarContract.Events.DESCRIPTION,
                "$MARKER${date}:${moment.prayer.name}\n${moment.prayer.title} в ${formatClock(start)}",
            )
            put(CalendarContract.Events.DTSTART, start.toInstant().toEpochMilli())
            put(CalendarContract.Events.DTEND, end.toInstant().toEpochMilli())
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            put(CalendarContract.Events.EVENT_END_TIMEZONE, zone.id)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.HAS_ALARM, 0)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
            put(CalendarContract.Events.CUSTOM_APP_URI, "namaz://${date}/${moment.prayer.name}")
        }
        context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)?.let { uri ->
            ContentUris.parseId(uri)
        }
    }

    companion object {
        const val MARKER = "namaz:"
        const val WINDOW_DAYS = 7
    }
}
