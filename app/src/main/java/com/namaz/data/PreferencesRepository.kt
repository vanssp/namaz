package com.namaz.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.namaz.domain.AsrMadhab
import com.namaz.domain.CalculationMethod
import com.namaz.domain.HighLatitudeRule
import com.namaz.domain.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "namaz_settings")

data class UserSettings(
    val method: CalculationMethod = CalculationMethod.RUSSIA,
    val madhab: AsrMadhab = AsrMadhab.HANAFI,
    val highLatitude: HighLatitudeRule = HighLatitudeRule.ANGLE_BASED,
    val autoLocation: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String = "",
    val remindersEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val leadMinutes: Int = 0,
    val reminders: Map<Prayer, Boolean> = Prayer.entries.associateWith { it.isObligatory },
    val offsets: Map<Prayer, Int> = Prayer.entries.associateWith { 0 },
    val calendarEnabled: Boolean = false,
    val calendarId: Long? = null,
    val calendarFingerprint: String = "",
    val hijriOffset: Int = 0,
)

fun UserSettings.reminds(prayer: Prayer): Boolean = reminders[prayer] ?: prayer.isObligatory

fun UserSettings.offsetMinutes(prayer: Prayer): Int = offsets[prayer] ?: 0

class PreferencesRepository(private val context: Context) {
    val settings: Flow<UserSettings> = context.dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    suspend fun snapshot(): UserSettings = settings.first()

    suspend fun setMethod(method: CalculationMethod) = update { it[Keys.method] = method.name }

    suspend fun setMadhab(madhab: AsrMadhab) = update { it[Keys.madhab] = madhab.name }

    suspend fun setHighLatitude(rule: HighLatitudeRule) = update { it[Keys.highLatitude] = rule.name }

    suspend fun setAutoLocation(enabled: Boolean) = update { it[Keys.autoLocation] = enabled }

    suspend fun setLocation(latitude: Double, longitude: Double, name: String) = update {
        it[Keys.latitude] = latitude.toString()
        it[Keys.longitude] = longitude.toString()
        it[Keys.locationName] = name
    }

    suspend fun setManualLocation(latitude: Double, longitude: Double) = update {
        it[Keys.autoLocation] = false
        it[Keys.latitude] = latitude.toString()
        it[Keys.longitude] = longitude.toString()
        if (it[Keys.locationName].isNullOrBlank()) {
            it[Keys.locationName] = "Своя точка"
        }
    }

    suspend fun setLocationName(name: String) = update { it[Keys.locationName] = name }

    suspend fun setRemindersEnabled(enabled: Boolean) = update { it[Keys.reminders] = enabled }

    suspend fun setSoundEnabled(enabled: Boolean) = update { it[Keys.sound] = enabled }

    suspend fun setLeadMinutes(minutes: Int) = update { it[Keys.lead] = minutes }

    suspend fun setReminder(prayer: Prayer, enabled: Boolean) = update {
        it[Keys.remind(prayer)] = enabled
    }

    suspend fun setOffset(prayer: Prayer, minutes: Int) = update {
        it[Keys.offset(prayer)] = minutes.coerceIn(-30, 30)
    }

    suspend fun setCalendarEnabled(enabled: Boolean) = update { it[Keys.calendarEnabled] = enabled }

    suspend fun setCalendarId(id: Long?) = update {
        if (id == null) it.remove(Keys.calendarId) else it[Keys.calendarId] = id
    }

    suspend fun setCalendarFingerprint(value: String) = update { it[Keys.fingerprint] = value }

    suspend fun setHijriOffset(days: Int) = update { it[Keys.hijriOffset] = days.coerceIn(-2, 2) }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private fun Preferences.toSettings(): UserSettings {
        val method = enumOr(this[Keys.method], CalculationMethod.RUSSIA)
        val madhab = enumOr(this[Keys.madhab], AsrMadhab.HANAFI)
        val highLatitude = enumOr(this[Keys.highLatitude], HighLatitudeRule.ANGLE_BASED)
        return UserSettings(
            method = method,
            madhab = madhab,
            highLatitude = highLatitude,
            autoLocation = this[Keys.autoLocation] ?: true,
            latitude = this[Keys.latitude]?.toDoubleOrNull(),
            longitude = this[Keys.longitude]?.toDoubleOrNull(),
            locationName = this[Keys.locationName].orEmpty(),
            remindersEnabled = this[Keys.reminders] ?: true,
            soundEnabled = this[Keys.sound] ?: true,
            leadMinutes = this[Keys.lead] ?: 0,
            reminders = Prayer.entries.associateWith { prayer ->
                this[Keys.remind(prayer)] ?: prayer.isObligatory
            },
            offsets = Prayer.entries.associateWith { prayer -> this[Keys.offset(prayer)] ?: 0 },
            calendarEnabled = this[Keys.calendarEnabled] ?: false,
            calendarId = this[Keys.calendarId],
            calendarFingerprint = this[Keys.fingerprint].orEmpty(),
            hijriOffset = this[Keys.hijriOffset] ?: 0,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T {
        return name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
    }

    private object Keys {
        val method = stringPreferencesKey("method")
        val madhab = stringPreferencesKey("madhab")
        val highLatitude = stringPreferencesKey("high_lat")
        val autoLocation = booleanPreferencesKey("auto_loc")
        val latitude = stringPreferencesKey("lat")
        val longitude = stringPreferencesKey("lng")
        val locationName = stringPreferencesKey("loc_name")
        val reminders = booleanPreferencesKey("reminders")
        val sound = booleanPreferencesKey("sound")
        val lead = intPreferencesKey("lead")
        val calendarEnabled = booleanPreferencesKey("cal_on")
        val calendarId = longPreferencesKey("cal_id")
        val fingerprint = stringPreferencesKey("cal_fp")
        val hijriOffset = intPreferencesKey("hijri_offset")

        fun remind(prayer: Prayer) = booleanPreferencesKey("remind_${prayer.name}")
        fun offset(prayer: Prayer) = intPreferencesKey("offset_${prayer.name}")
    }
}
