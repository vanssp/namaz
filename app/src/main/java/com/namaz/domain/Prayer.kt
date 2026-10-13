package com.namaz.domain

import java.time.ZonedDateTime

enum class Prayer(val title: String, val isObligatory: Boolean) {
    FAJR("Фаджр", true),
    SUNRISE("Восход", false),
    DHUHR("Зухр", true),
    ASR("Аср", true),
    MAGHRIB("Магриб", true),
    ISHA("Иша", true),
}

enum class CalculationMethod(
    val title: String,
    val detail: String,
    val fajrAngle: Double,
    val ishaAngle: Double?,
    val ishaMinutes: Int = 0,
    val maghribAngle: Double? = null,
) {
    RUSSIA("Россия", "Духовное управление: фаджр 16°, иша 15°", 16.0, 15.0),
    MWL("Всемирная лига", "Фаджр 18°, иша 17°", 18.0, 17.0),
    ISNA("Северная Америка", "Фаджр 15°, иша 15°", 15.0, 15.0),
    EGYPT("Египет", "Фаджр 19,5°, иша 17,5°", 19.5, 17.5),
    UMM_AL_QURA("Умм аль-Кура", "Фаджр 18,5°, иша через 90 минут", 18.5, null, 90),
    KARACHI("Карачи", "Фаджр 18°, иша 18°", 18.0, 18.0),
    TEHRAN("Тегеран", "Фаджр 17,7°, магриб 4,5°, иша 14°", 17.7, 14.0, 0, 4.5),
}

enum class AsrMadhab(val title: String, val factor: Double) {
    SHAFI("Шафиитский", 1.0),
    HANAFI("Ханафитский", 2.0),
}

enum class HighLatitudeRule(val title: String) {
    NONE("Без поправки"),
    MIDDLE_OF_NIGHT("Середина ночи"),
    ANGLE_BASED("По углу"),
    ONE_SEVENTH("Седьмая часть ночи"),
}

data class PrayerMoment(
    val prayer: Prayer,
    val time: ZonedDateTime?,
)

private const val CURRENT_HOLD_MINUTES = 15L

fun focusPrayer(moments: List<PrayerMoment>, now: ZonedDateTime): PrayerMoment? {
    val prayers = moments.filter { it.prayer.isObligatory && it.time != null }
    val active = prayers
        .filter { moment ->
            val time = moment.time ?: return@filter false
            !time.isAfter(now) && !time.plusMinutes(CURRENT_HOLD_MINUTES).isBefore(now)
        }
        .maxByOrNull { it.time!! }
    if (active != null) return active
    return prayers.filter { it.time!!.isAfter(now) }.minByOrNull { it.time!! }
}
