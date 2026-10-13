package com.namaz.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Время намаза по астрономической модели PrayTimes:
 * положение солнца, угол фаджра и иша, аср по мазхабу.
 */
object PrayerCalculator {
    private const val SUNRISE_ANGLE = 0.833

    fun calculate(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        method: CalculationMethod,
        madhab: AsrMadhab,
        highLatitude: HighLatitudeRule,
        offsets: Map<Prayer, Int> = emptyMap(),
    ): List<PrayerMoment> {
        val hours = computeHours(date, latitude, longitude, zone, method, madhab, highLatitude)
        return Prayer.entries.map { prayer ->
            val raw = hours[prayer]
            val time = if (raw == null || raw.isNaN()) {
                null
            } else {
                hoursToDateTime(date, zone, raw).plusMinutes((offsets[prayer] ?: 0).toLong())
            }
            PrayerMoment(prayer, time)
        }
    }

    private fun computeHours(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        zone: ZoneId,
        method: CalculationMethod,
        madhab: AsrMadhab,
        highLatitude: HighLatitudeRule,
    ): Map<Prayer, Double> {
        val noon = date.atTime(12, 0).atZone(zone)
        val timeZone = noon.offset.totalSeconds / 3600.0
        val jDate = julianDay(date.year, date.monthValue, date.dayOfMonth) - longitude / (15.0 * 24.0)
        val portion = mapOf(
            Prayer.FAJR to 5.0 / 24.0,
            Prayer.SUNRISE to 6.0 / 24.0,
            Prayer.DHUHR to 12.0 / 24.0,
            Prayer.ASR to 13.0 / 24.0,
            Prayer.MAGHRIB to 18.0 / 24.0,
            Prayer.ISHA to 18.0 / 24.0,
        )

        fun midDay(time: Double): Double {
            val equation = sunPosition(jDate + time).equation
            return fix(12.0 - equation, 24.0)
        }

        fun sunAngleTime(angle: Double, time: Double, ccw: Boolean): Double {
            val declination = sunPosition(jDate + time).declination
            val noonHours = midDay(time)
            val numerator = -dsin(angle) - dsin(latitude) * dsin(declination)
            val denominator = dcos(latitude) * dcos(declination)
            if (denominator == 0.0) return Double.NaN
            val span = darccos(numerator / denominator) / 15.0
            if (span.isNaN()) return Double.NaN
            return noonHours + if (ccw) -span else span
        }

        fun asrTime(factor: Double, time: Double): Double {
            val declination = sunPosition(jDate + time).declination
            val angle = -darccot(factor + dtan(abs(latitude - declination)))
            return sunAngleTime(angle, time, ccw = false)
        }

        val shift = timeZone - longitude / 15.0
        fun shifted(value: Double) = if (value.isNaN()) value else value + shift

        val fajr = shifted(sunAngleTime(method.fajrAngle, portion.getValue(Prayer.FAJR), ccw = true))
        val sunrise = shifted(sunAngleTime(SUNRISE_ANGLE, portion.getValue(Prayer.SUNRISE), ccw = true))
        val dhuhr = shifted(midDay(portion.getValue(Prayer.DHUHR)))
        val asr = shifted(asrTime(madhab.factor, portion.getValue(Prayer.ASR)))
        val sunset = shifted(sunAngleTime(SUNRISE_ANGLE, 18.0 / 24.0, ccw = false))
        val maghrib = if (method.maghribAngle != null) {
            shifted(sunAngleTime(method.maghribAngle, portion.getValue(Prayer.MAGHRIB), ccw = false))
        } else {
            sunset
        }
        val isha = if (method.ishaAngle != null) {
            shifted(sunAngleTime(method.ishaAngle, portion.getValue(Prayer.ISHA), ccw = false))
        } else {
            maghrib + method.ishaMinutes / 60.0
        }

        val night = timeDiff(sunset, sunrise)
        val fajrAdjusted = adjustHighLat(fajr, sunrise, method.fajrAngle, night, ccw = true, highLatitude)
        val maghribAdjusted = if (method.maghribAngle != null) {
            adjustHighLat(maghrib, sunset, method.maghribAngle, night, ccw = false, highLatitude)
        } else {
            maghrib
        }
        val ishaAdjusted = if (method.ishaAngle != null) {
            adjustHighLat(isha, sunset, method.ishaAngle, night, ccw = false, highLatitude)
        } else {
            isha
        }

        return mapOf(
            Prayer.FAJR to fajrAdjusted,
            Prayer.SUNRISE to sunrise,
            Prayer.DHUHR to dhuhr,
            Prayer.ASR to asr,
            Prayer.MAGHRIB to maghribAdjusted,
            Prayer.ISHA to ishaAdjusted,
        )
    }

    private fun adjustHighLat(
        time: Double,
        base: Double,
        angle: Double,
        night: Double,
        ccw: Boolean,
        rule: HighLatitudeRule,
    ): Double {
        if (rule == HighLatitudeRule.NONE || base.isNaN()) return time
        val portion = nightPortion(rule, angle) * night
        val diff = if (ccw) timeDiff(time, base) else timeDiff(base, time)
        if (time.isNaN() || diff > portion) {
            return if (ccw) base - portion else base + portion
        }
        return time
    }

    private fun nightPortion(rule: HighLatitudeRule, angle: Double): Double = when (rule) {
        HighLatitudeRule.NONE -> 0.0
        HighLatitudeRule.MIDDLE_OF_NIGHT -> 0.5
        HighLatitudeRule.ANGLE_BASED -> angle / 60.0
        HighLatitudeRule.ONE_SEVENTH -> 1.0 / 7.0
    }

    private fun hoursToDateTime(date: LocalDate, zone: ZoneId, hours: Double): ZonedDateTime {
        var shifted = hours + 0.5 / 60.0
        var day = date
        while (shifted < 0) {
            shifted += 24.0
            day = day.minusDays(1)
        }
        while (shifted >= 24.0) {
            shifted -= 24.0
            day = day.plusDays(1)
        }
        val hour = floor(shifted).toInt()
        val minute = floor((shifted - hour) * 60.0).toInt().coerceIn(0, 59)
        return day.atTime(hour.coerceIn(0, 23), minute).atZone(zone)
    }

    private data class SunPosition(val declination: Double, val equation: Double)

    private fun sunPosition(jd: Double): SunPosition {
        val day = jd - 2451545.0
        val anomaly = fix(357.529 + 0.98560028 * day, 360.0)
        val meanLongitude = fix(280.459 + 0.98564736 * day, 360.0)
        val ecliptic = fix(
            meanLongitude + 1.915 * dsin(anomaly) + 0.020 * dsin(2 * anomaly),
            360.0,
        )
        val obliquity = 23.439 - 0.00000036 * day
        val rightAscension = darctan2(dcos(obliquity) * dsin(ecliptic), dcos(ecliptic)) / 15.0
        val equation = meanLongitude / 15.0 - fix(rightAscension, 24.0)
        val declination = darcsin(dsin(obliquity) * dsin(ecliptic))
        return SunPosition(declination, equation)
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun timeDiff(from: Double, to: Double) = fix(to - from, 24.0)

    private fun fix(value: Double, bound: Double): Double {
        if (value.isNaN()) return Double.NaN
        var wrapped = value - bound * floor(value / bound)
        if (wrapped < 0) wrapped += bound
        return wrapped
    }

    private fun dsin(degrees: Double) = sin(Math.toRadians(degrees))
    private fun dcos(degrees: Double) = cos(Math.toRadians(degrees))
    private fun dtan(degrees: Double) = tan(Math.toRadians(degrees))
    private fun darcsin(value: Double) = Math.toDegrees(asin(value.coerceIn(-1.0, 1.0)))
    private fun darccos(value: Double): Double {
        if (value < -1.0 || value > 1.0) return Double.NaN
        return Math.toDegrees(acos(value))
    }

    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(value: Double) = Math.toDegrees(atan2(1.0, value))
}
