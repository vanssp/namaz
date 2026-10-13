package com.namaz.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.namaz.NamazApp
import com.namaz.data.CalendarRepository
import com.namaz.data.DeviceCalendar
import com.namaz.data.UserSettings
import com.namaz.domain.AsrMadhab
import com.namaz.domain.CalculationMethod
import com.namaz.domain.HighLatitudeRule
import com.namaz.domain.Prayer
import com.namaz.reminder.PrayerSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NamazApp
    val settings: StateFlow<UserSettings> = app.preferences.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(),
    )
    val calendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val message = MutableStateFlow<String?>(null)

    fun reloadCalendars() {
        calendars.value = runCatching { app.calendars.calendars() }.getOrDefault(emptyList())
    }

    fun setMethod(method: CalculationMethod) = save { app.preferences.setMethod(method) }
    fun setMadhab(madhab: AsrMadhab) = save { app.preferences.setMadhab(madhab) }
    fun setHighLatitude(rule: HighLatitudeRule) = save { app.preferences.setHighLatitude(rule) }
    fun setAutoLocation(enabled: Boolean) = save { app.preferences.setAutoLocation(enabled) }
    fun setReminders(enabled: Boolean) = save { app.preferences.setRemindersEnabled(enabled) }
    fun setSound(enabled: Boolean) = save { app.preferences.setSoundEnabled(enabled) }
    fun setLead(minutes: Int) = save { app.preferences.setLeadMinutes(minutes) }
    fun setReminder(prayer: Prayer, enabled: Boolean) = save { app.preferences.setReminder(prayer, enabled) }
    fun setOffset(prayer: Prayer, minutes: Int) = save { app.preferences.setOffset(prayer, minutes) }

    fun setManualLocation(latitude: String, longitude: String) {
        val lat = latitude.replace(',', '.').toDoubleOrNull()
        val lng = longitude.replace(',', '.').toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            message.value = "Широта от −90 до 90, долгота от −180 до 180"
            return
        }
        save {
            app.preferences.setManualLocation(lat, lng)
            message.value = "Координаты сохранены"
        }
    }

    fun setCalendarEnabled(enabled: Boolean) = save { app.preferences.setCalendarEnabled(enabled) }

    fun selectCalendar(id: Long) = save { app.preferences.setCalendarId(id) }

    fun syncNow() {
        viewModelScope.launch {
            message.value = "Записываю намазы…"
            app.preferences.setCalendarFingerprint("")
            PrayerSchedule.refresh(app)
            val updated = app.preferences.snapshot()
            message.value = if (updated.calendarFingerprint.isNotEmpty()) {
                "Намазы записаны на ${CalendarRepository.WINDOW_DAYS} дней"
            } else {
                "Не получилось записать. Проверьте место и выбранный календарь."
            }
        }
    }

    fun clearCalendar() {
        viewModelScope.launch {
            val id = app.preferences.snapshot().calendarId
            app.preferences.setCalendarEnabled(false)
            if (id != null && app.calendars.hasPermission()) {
                runCatching { app.calendars.deleteOurs(id) }
            }
            app.preferences.setCalendarFingerprint("")
            message.value = "События намаза удалены из календаря"
        }
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
