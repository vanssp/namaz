package com.namaz

import com.namaz.domain.AsrMadhab
import com.namaz.domain.CalculationMethod
import com.namaz.domain.HighLatitudeRule
import com.namaz.domain.HijriCalendar
import com.namaz.domain.Prayer
import com.namaz.domain.PrayerCalculator
import com.namaz.domain.Qibla
import com.namaz.domain.formatCountdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PrayerCalculatorTest {
    private val clock = DateTimeFormatter.ofPattern("HH:mm")

    @Test
    fun moscowMatchesEstablishedLeagueTimes() {
        val times = PrayerCalculator.calculate(
            date = LocalDate.of(2026, 10, 13),
            latitude = 55.7558,
            longitude = 37.6173,
            zone = ZoneId.of("Europe/Moscow"),
            method = CalculationMethod.MWL,
            madhab = AsrMadhab.HANAFI,
            highLatitude = HighLatitudeRule.ANGLE_BASED,
        ).associate { it.prayer to it.time }
        assertEquals("04:53", times.getValue(Prayer.FAJR)!!.format(clock))
        assertEquals("06:56", times.getValue(Prayer.SUNRISE)!!.format(clock))
        assertEquals("12:16", times.getValue(Prayer.DHUHR)!!.format(clock))
        assertEquals("15:38", times.getValue(Prayer.ASR)!!.format(clock))
        assertEquals("17:35", times.getValue(Prayer.MAGHRIB)!!.format(clock))
        assertEquals("19:30", times.getValue(Prayer.ISHA)!!.format(clock))
    }

    @Test
    fun meccaUmmAlQuraUsesNinetyMinutesAfterMaghrib() {
        val times = PrayerCalculator.calculate(
            date = LocalDate.of(2026, 10, 13),
            latitude = 21.4225,
            longitude = 39.8262,
            zone = ZoneId.of("Asia/Riyadh"),
            method = CalculationMethod.UMM_AL_QURA,
            madhab = AsrMadhab.SHAFI,
            highLatitude = HighLatitudeRule.NONE,
        ).associate { it.prayer to it.time }
        assertEquals("04:59", times.getValue(Prayer.FAJR)!!.format(clock))
        assertEquals("06:16", times.getValue(Prayer.SUNRISE)!!.format(clock))
        assertEquals("12:07", times.getValue(Prayer.DHUHR)!!.format(clock))
        assertEquals("15:28", times.getValue(Prayer.ASR)!!.format(clock))
        assertEquals("17:58", times.getValue(Prayer.MAGHRIB)!!.format(clock))
        assertEquals("19:28", times.getValue(Prayer.ISHA)!!.format(clock))
    }

    @Test
    fun offsetMovesFajrByTwoMinutes() {
        val times = PrayerCalculator.calculate(
            date = LocalDate.of(2026, 10, 13),
            latitude = 55.7558,
            longitude = 37.6173,
            zone = ZoneId.of("Europe/Moscow"),
            method = CalculationMethod.MWL,
            madhab = AsrMadhab.HANAFI,
            highLatitude = HighLatitudeRule.ANGLE_BASED,
            offsets = mapOf(Prayer.FAJR to 2),
        )
        assertEquals("04:55", times.first { it.prayer == Prayer.FAJR }.time!!.format(clock))
    }

    @Test
    fun shortNorthernNightStillProducesOrderedTimes() {
        val moments = PrayerCalculator.calculate(
            date = LocalDate.of(2026, 6, 21),
            latitude = 59.93,
            longitude = 30.34,
            zone = ZoneId.of("Europe/Moscow"),
            method = CalculationMethod.MWL,
            madhab = AsrMadhab.HANAFI,
            highLatitude = HighLatitudeRule.ANGLE_BASED,
        )
        assertTrue(moments.all { it.time != null })
        val epochs = moments.map { it.time!!.toInstant().toEpochMilli() }
        assertEquals(epochs, epochs.sorted())
        assertTrue(epochs.zipWithNext().all { (left, right) -> left < right })
    }

    @Test
    fun qiblaPointsTowardKaabaFromCardinalOffsets() {
        val north = Qibla.bearing(22.4225, 39.8262)
        val south = Qibla.bearing(20.4225, 39.8262)
        val west = Qibla.bearing(21.4225, 38.8262)
        assertTrue(north in 170.0..190.0)
        assertTrue(south <= 10.0 || south >= 350.0)
        assertTrue(west in 80.0..100.0)
        assertTrue(Qibla.distanceKm(21.4225, 39.8262) < 1.0)
        assertTrue(Qibla.distanceKm(55.7558, 37.6173) in 3_000.0..5_000.0)
    }

    @Test
    fun hijriDateForOctober2026() {
        val hijri = HijriCalendar.fromGregorian(2026, 10, 13)
        assertEquals(1448, hijri.year)
        assertEquals(5, hijri.month)
        assertEquals(1, hijri.day)
        assertEquals("1 джумада аль-уля 1448", hijri.format())
    }

    @Test
    fun countdownUsesRussianPlurals() {
        assertEquals("сейчас", formatCountdown(20))
        assertEquals("через 1 минуту", formatCountdown(60))
        assertEquals("через 2 минуты", formatCountdown(120))
        assertEquals("через 5 минут", formatCountdown(300))
        assertEquals("через 11 минут", formatCountdown(11 * 60))
        assertEquals("через 21 минуту", formatCountdown(21 * 60))
        assertEquals("через 1 час", formatCountdown(60 * 60))
        assertEquals("через 2 часа", formatCountdown(2 * 60 * 60))
        assertEquals("через 5 часов", formatCountdown(5 * 60 * 60))
        assertEquals("через 1 час 1 минуту", formatCountdown(61 * 60))
    }
}
