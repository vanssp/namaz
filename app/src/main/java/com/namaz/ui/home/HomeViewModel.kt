package com.namaz.ui.home

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.namaz.NamazApp
import com.namaz.data.UserSettings
import com.namaz.domain.HijriCalendar
import com.namaz.domain.Prayer
import com.namaz.domain.PrayerCalculator
import com.namaz.domain.PrayerMoment
import com.namaz.domain.focusPrayer
import com.namaz.domain.formatClock
import com.namaz.domain.formatCoordinates
import com.namaz.domain.formatCountdown
import com.namaz.domain.formatDayMonth
import com.namaz.domain.formatWeekday
import com.namaz.reminder.PrayerSchedule
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class PrayerRowState(
    val title: String,
    val time: String,
    val highlighted: Boolean,
    val passed: Boolean,
    val quiet: Boolean,
)

data class SystemHints(
    val notificationsGranted: Boolean = true,
    val exactAlarmsGranted: Boolean = true,
)

data class HomeUiState(
    val weekday: String = formatWeekday(LocalDate.now()),
    val dateLine: String = formatDayMonth(LocalDate.now()),
    val hijriLine: String = HijriCalendar.from(LocalDate.now()).format(),
    val hasLocation: Boolean = false,
    val locating: Boolean = false,
    val place: String = "",
    val coordinates: String = "",
    val method: String = "",
    val prayers: List<PrayerRowState> = emptyList(),
    val heroTitle: String? = null,
    val heroTime: String? = null,
    val countdown: String = "",
    val banner: String? = null,
    val askNotifications: Boolean = false,
    val askExactAlarm: Boolean = false,
)

@OptIn(FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NamazApp
    private val locating = MutableStateFlow(false)
    private val banner = MutableStateFlow<String?>(null)
    private val hints = MutableStateFlow(SystemHints())
    private val clock = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(1_000)
        }
    }

    val state: StateFlow<HomeUiState> = combine(
        app.preferences.settings,
        clock,
        locating,
        banner,
        hints,
    ) { settings, now, isLocating, message, system ->
        build(settings, now, isLocating, message, system)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refreshSystemHints()
        viewModelScope.launch {
            app.preferences.settings.debounce(700).collect {
                PrayerSchedule.refresh(app)
            }
        }
        viewModelScope.launch {
            val current = app.preferences.snapshot()
            if (current.autoLocation && app.location.hasPermission()) {
                refreshLocation(quiet = current.latitude != null)
            }
        }
    }

    fun refreshSystemHints() {
        val context = getApplication<NamazApp>()
        val notifications = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val exact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } else {
            true
        }
        hints.value = SystemHints(notifications, exact)
    }

    fun onLocationPermission(granted: Boolean) {
        if (granted) {
            banner.value = null
            refreshLocation(quiet = false)
        } else {
            banner.value = "Без геолокации можно ввести координаты в настройках"
        }
    }

    fun refreshLocation(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) locating.value = true
            val fix = app.location.current()
            if (fix != null) {
                val previous = app.preferences.snapshot()
                val name = app.location.placeName(fix.latitude, fix.longitude)
                    ?: previous.locationName.ifBlank { "Моё место" }
                app.preferences.setLocation(fix.latitude, fix.longitude, name)
                banner.value = null
            } else if (!quiet) {
                banner.value = "Не удалось определить место"
            }
            locating.value = false
        }
    }

    private fun build(
        settings: UserSettings,
        now: ZonedDateTime,
        isLocating: Boolean,
        message: String?,
        system: SystemHints,
    ): HomeUiState {
        val zone = ZoneId.systemDefault()
        val today = now.withZoneSameInstant(zone).toLocalDate()
        val latitude = settings.latitude
        val longitude = settings.longitude
        val remindersOn = settings.remindersEnabled
        val base = HomeUiState(
            locating = isLocating,
            banner = message,
            place = settings.locationName,
            method = settings.method.title,
            askNotifications = remindersOn && !system.notificationsGranted,
            askExactAlarm = remindersOn && system.notificationsGranted && !system.exactAlarmsGranted,
        )
        if (latitude == null || longitude == null) {
            return base.copy(
                weekday = formatWeekday(today),
                dateLine = formatDayMonth(today),
                hijriLine = HijriCalendar.from(today).format(),
            )
        }
        val todayMoments = calculate(today, latitude, longitude, settings, zone)
        val tomorrow = today.plusDays(1)
        val tomorrowMoments = calculate(tomorrow, latitude, longitude, settings, zone)
        val focus = focusPrayer(todayMoments + tomorrowMoments, now.withZoneSameInstant(zone))
        val focusTime = focus?.time
        val showTomorrow = focusTime != null && focusTime.toLocalDate().isAfter(today) && focusTime.isAfter(now)
        val shownDate = if (showTomorrow) tomorrow else today
        val shown = if (showTomorrow) tomorrowMoments else todayMoments
        return base.copy(
            weekday = if (showTomorrow) "завтра · ${formatWeekday(shownDate)}" else formatWeekday(shownDate),
            dateLine = formatDayMonth(shownDate),
            hijriLine = HijriCalendar.from(shownDate).format(),
            hasLocation = true,
            coordinates = formatCoordinates(latitude, longitude),
            place = settings.locationName.ifBlank { formatCoordinates(latitude, longitude) },
            prayers = shown.map { row(it, focus, now.withZoneSameInstant(zone)) },
            heroTitle = focus?.prayer?.title,
            heroTime = focusTime?.let(::formatClock),
            countdown = when {
                focusTime == null -> ""
                focusTime.isAfter(now) -> formatCountdown(now, focusTime)
                else -> "время намаза"
            },
        )
    }

    private fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        settings: UserSettings,
        zone: java.time.ZoneId,
    ) = PrayerCalculator.calculate(
        date = date,
        latitude = latitude,
        longitude = longitude,
        zone = zone,
        method = settings.method,
        madhab = settings.madhab,
        highLatitude = settings.highLatitude,
        offsets = settings.offsets,
    )

    private fun row(moment: PrayerMoment, focus: PrayerMoment?, now: ZonedDateTime): PrayerRowState {
        val time = moment.time
        val highlighted = focus != null && moment.prayer == focus.prayer && moment.time == focus.time
        return PrayerRowState(
            title = moment.prayer.title,
            time = time?.let(::formatClock) ?: "—",
            highlighted = highlighted,
            passed = time != null && time.isBefore(now) && !highlighted,
            quiet = moment.prayer == Prayer.SUNRISE,
        )
    }
}
