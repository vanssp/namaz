package com.namaz.domain

import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val RU = Locale.forLanguageTag("ru")
private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val WEEKDAY: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", RU)
private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", RU)

fun formatClock(time: ZonedDateTime): String = time.format(CLOCK)

fun formatWeekday(date: LocalDate): String = date.format(WEEKDAY)

fun formatDayMonth(date: LocalDate): String = date.format(DAY_MONTH)

fun formatCountdown(from: ZonedDateTime, to: ZonedDateTime): String {
    return formatCountdown(Duration.between(from, to).seconds)
}

fun formatCountdown(seconds: Long): String {
    if (seconds < 0) return "время намаза"
    if (seconds <= 45) return "сейчас"
    val minutes = seconds / 60
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "через $hours ${hoursWord(hours)} $rest ${minutesWord(rest)}"
        hours > 0 -> "через $hours ${hoursWord(hours)}"
        else -> "через $minutes ${minutesWord(minutes)}"
    }
}

fun formatCoordinates(latitude: Double, longitude: Double): String {
    val latitudeLabel = if (latitude >= 0) "с.ш." else "ю.ш."
    val longitudeLabel = if (longitude >= 0) "в.д." else "з.д."
    return "%.2f° %s, %.2f° %s".format(RU, abs(latitude), latitudeLabel, abs(longitude), longitudeLabel)
}

fun formatDistanceKm(kilometers: Double): String {
    val rounded = kilometers.toInt()
    val grouped = "%,d".format(RU, rounded)
    return "$grouped км"
}

private fun hoursWord(value: Long) = plural(value, "час", "часа", "часов")

private fun minutesWord(value: Long) = plural(value, "минуту", "минуты", "минут")

private fun plural(value: Long, one: String, few: String, many: String): String {
    val hundred = value % 100
    val ten = value % 10
    return when {
        hundred in 11..14 -> many
        ten == 1L -> one
        ten in 2..4 -> few
        else -> many
    }
}
