package com.namaz.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.namaz.NamazApp
import com.namaz.data.CalendarRepository
import com.namaz.data.UserSettings
import com.namaz.data.reminds
import com.namaz.domain.Prayer
import com.namaz.domain.PrayerCalculator
import com.namaz.domain.PrayerMoment
import com.namaz.domain.focusPrayer
import com.namaz.domain.formatClock
import com.namaz.widget.WidgetUpdater
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object PrayerSchedule {
    const val ACTION_PRAYER = "com.namaz.action.PRAYER"
    const val ACTION_REFRESH = "com.namaz.action.REFRESH"
    const val EXTRA_PRAYER = "prayer"
    const val EXTRA_CLOCK = "clock"
    const val EXTRA_LEAD = "lead"

    private const val REQUEST_PRAYER = 20
    private const val REQUEST_MIDNIGHT = 1000
    private const val REQUEST_BOUNDARY = 1001

    suspend fun refresh(context: Context) {
        val app = context.applicationContext as NamazApp
        val settings = app.preferences.snapshot()
        if (settings.latitude == null || settings.longitude == null) {
            cancelAll(context)
            WidgetUpdater.render(context, settings, emptyList(), ZonedDateTime.now())
            return
        }
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val today = now.toLocalDate()
        val moments = day(today, settings, zone) + day(today.plusDays(1), settings, zone)
        schedule(context, settings, moments, now, today, zone)
        WidgetUpdater.render(context, settings, moments, now)
        syncCalendar(app, settings, zone, today)
    }

    suspend fun onPrayer(context: Context, prayerName: String, clock: String, lead: Int) {
        val app = context.applicationContext as NamazApp
        val settings = app.preferences.snapshot()
        val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrNull()
            ?.takeIf { settings.remindersEnabled && settings.reminds(it) }
        val wake = context.getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "namaz:adhan")
        wake?.acquire(20_000L)
        try {
            if (prayer != null) {
                Notifier.show(context, prayer, clock, lead, settings.soundEnabled)
            }
            refresh(context)
            if (prayer != null && settings.soundEnabled) delay(8_000)
        } finally {
            if (wake?.isHeld == true) wake.release()
        }
    }

    private fun day(date: LocalDate, settings: UserSettings, zone: ZoneId): List<PrayerMoment> {
        val latitude = settings.latitude ?: return emptyList()
        val longitude = settings.longitude ?: return emptyList()
        return PrayerCalculator.calculate(
            date = date,
            latitude = latitude,
            longitude = longitude,
            zone = zone,
            method = settings.method,
            madhab = settings.madhab,
            highLatitude = settings.highLatitude,
            offsets = settings.offsets,
        )
    }

    private fun schedule(
        context: Context,
        settings: UserSettings,
        moments: List<PrayerMoment>,
        now: ZonedDateTime,
        today: LocalDate,
        zone: ZoneId,
    ) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        Prayer.entries.forEach { manager.cancel(prayerPending(context, it, "", 0)) }
        manager.cancel(refreshPending(context, REQUEST_MIDNIGHT))
        manager.cancel(refreshPending(context, REQUEST_BOUNDARY))

        if (settings.remindersEnabled) {
            val lead = settings.leadMinutes.coerceIn(0, 120)
            Prayer.entries.forEach { prayer ->
                if (!settings.reminds(prayer)) return@forEach
                val moment = moments
                    .filter { it.prayer == prayer && it.time != null }
                    .mapNotNull { it.time }
                    .filter { trigger ->
                        trigger.minusMinutes(lead.toLong()).isAfter(now.plusSeconds(5))
                    }
                    .minOrNull()
                    ?: return@forEach
                val trigger = moment.minusMinutes(lead.toLong())
                arm(
                    manager,
                    trigger.toInstant().toEpochMilli(),
                    prayerPending(context, prayer, formatClock(moment), lead),
                )
            }
        }

        val upcoming = moments.mapNotNull { it.time }.filter { it.isAfter(now.plusSeconds(20)) }
        val boundaries = mutableListOf<ZonedDateTime>()
        upcoming.minOrNull()?.let { boundaries += it }
        val active = focusPrayer(moments, now)?.time
        if (active != null && !active.isAfter(now)) {
            val release = active.plusMinutes(15)
            if (release.isAfter(now.plusSeconds(20))) boundaries += release
        }
        boundaries.minOrNull()?.let { boundary ->
            arm(manager, boundary.toInstant().toEpochMilli(), refreshPending(context, REQUEST_BOUNDARY))
        }
        val midnight = today.plusDays(1).atTime(0, 5).atZone(zone)
        val anchor = if (midnight.isAfter(now.plusSeconds(30))) {
            midnight
        } else {
            today.plusDays(2).atTime(0, 5).atZone(zone)
        }
        arm(manager, anchor.toInstant().toEpochMilli(), refreshPending(context, REQUEST_MIDNIGHT))
    }

    private suspend fun syncCalendar(app: NamazApp, settings: UserSettings, zone: ZoneId, today: LocalDate) {
        val calendarId = settings.calendarId
        if (!settings.calendarEnabled) {
            if (calendarId != null && settings.calendarFingerprint.isNotEmpty() && app.calendars.hasPermission()) {
                runCatching { app.calendars.deleteOurs(calendarId) }
                app.preferences.setCalendarFingerprint("")
            }
            return
        }
        if (calendarId == null || settings.latitude == null || !app.calendars.hasPermission()) return
        val fingerprint = fingerprint(settings, today)
        if (fingerprint == settings.calendarFingerprint) return
        val written = runCatching {
            app.calendars.replaceWindow(settings, zone, today, CalendarRepository.WINDOW_DAYS)
        }.getOrDefault(0)
        if (written > 0) app.preferences.setCalendarFingerprint(fingerprint)
    }

    private fun fingerprint(settings: UserSettings, day: LocalDate): String = buildString {
        append(day)
        append('|').append(settings.method.name)
        append('|').append(settings.madhab.name)
        append('|').append(settings.highLatitude.name)
        append('|').append(settings.latitude)
        append('|').append(settings.longitude)
        append('|').append(settings.calendarId)
        Prayer.entries.forEach { append('|').append(settings.offsets[it] ?: 0) }
    }

    private fun arm(manager: AlarmManager, triggerAt: Long, pending: PendingIntent) {
        val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
        try {
            if (exactAllowed) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun cancelAll(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        Prayer.entries.forEach { manager.cancel(prayerPending(context, it, "", 0)) }
        manager.cancel(refreshPending(context, REQUEST_MIDNIGHT))
        manager.cancel(refreshPending(context, REQUEST_BOUNDARY))
    }

    private fun prayerPending(context: Context, prayer: Prayer, clock: String, lead: Int): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER
            putExtra(EXTRA_PRAYER, prayer.name)
            putExtra(EXTRA_CLOCK, clock)
            putExtra(EXTRA_LEAD, lead)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_PRAYER + prayer.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun refreshPending(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply { action = ACTION_REFRESH }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
