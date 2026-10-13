package com.namaz.domain

import java.time.LocalDate
import kotlin.math.floor

data class HijriDate(val year: Int, val month: Int, val day: Int) {
    fun format(): String {
        val name = MONTHS.getOrElse(month - 1) { "" }
        return "$day $name $year"
    }

    companion object {
        private val MONTHS = listOf(
            "мухаррам",
            "сафар",
            "раби аль-авваль",
            "раби ас-сани",
            "джумада аль-уля",
            "джумада ас-сани",
            "раджаб",
            "шаабан",
            "рамадан",
            "шавваль",
            "зуль-када",
            "зуль-хиджжа",
        )
    }
}

object HijriCalendar {
    fun from(date: LocalDate): HijriDate = fromGregorian(date.year, date.monthValue, date.dayOfMonth)

    fun fromGregorian(year: Int, month: Int, day: Int): HijriDate {
        val jd = gregorianToJulian(year, month, day)
        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val hijriMonth = (24 * l) / 709
        val hijriDay = l - (709 * hijriMonth) / 24
        val hijriYear = 30 * n + j - 30
        return HijriDate(hijriYear, hijriMonth, hijriDay)
    }

    private fun gregorianToJulian(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + a / 4
        return floor(365.25 * (y + 4716)).toInt() + floor(30.6001 * (m + 1)).toInt() + day + b - 1524
    }
}
