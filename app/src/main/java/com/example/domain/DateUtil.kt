package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DateKey(
    val year: Int,
    val month: Int, // 1..12
    val day: Int,   // 1..31
) : Comparable<DateKey> {
    override fun compareTo(other: DateKey): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    fun toIsoString(): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    fun toShortMonthDay(): String = String.format(Locale.US, "%d/%d", month, day)

    fun toChineseFull(): String = "${year}年${month}月${day}日"

    fun toChineseMonthDay(): String = "${month}月${day}日"

    fun toYearMonth(): YearMonthKey = YearMonthKey(year, month)

    companion object {
        fun parse(iso: String): DateKey {
            val parts = iso.split("-")
            require(parts.size == 3) { "Invalid ISO date: $iso" }
            return DateKey(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }
    }
}

data class YearMonthKey(
    val year: Int,
    val month: Int, // 1..12
) : Comparable<YearMonthKey> {
    override fun compareTo(other: YearMonthKey): Int {
        if (year != other.year) return year.compareTo(other.year)
        return month.compareTo(other.month)
    }

    fun toChineseLabel(): String = "${year}年${month}月"

    fun plusMonths(delta: Int): YearMonthKey {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1)
            add(Calendar.MONTH, delta)
        }
        return YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
}

data class DateRange(
    val start: DateKey,
    val end: DateKey,
) {
    fun allDates(): List<DateKey> {
        val list = mutableListOf<DateKey>()
        var current = start
        while (current <= end) {
            list.add(current)
            current = DateUtil.plusDays(current, 1)
        }
        return list
    }
}

object DateUtil {
    fun today(): DateKey {
        val cal = Calendar.getInstance()
        return DateKey(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    fun currentYearMonth(): YearMonthKey = today().toYearMonth()

    fun currentHourMinute(): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }

    fun plusDays(date: DateKey, days: Int): DateKey {
        val cal = Calendar.getInstance().apply {
            clear()
            set(date.year, date.month - 1, date.day)
            add(Calendar.DAY_OF_MONTH, days)
        }
        return DateKey(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** Returns 1 for Monday .. 7 for Sunday */
    fun dayOfWeekMondayFirst(date: DateKey): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(date.year, date.month - 1, date.day)
        }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }

    fun weekdayChinese(date: DateKey): String = when (dayOfWeekMondayFirst(date)) {
        1 -> "周一"
        2 -> "周二"
        3 -> "周三"
        4 -> "周四"
        5 -> "周五"
        6 -> "周六"
        else -> "周日"
    }

    fun daysInMonth(ym: YearMonthKey): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(ym.year, ym.month - 1, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun monthRange(ym: YearMonthKey): DateRange {
        val lastDay = daysInMonth(ym)
        return DateRange(
            start = DateKey(ym.year, ym.month, 1),
            end = DateKey(ym.year, ym.month, lastDay),
        )
    }

    /** Monday-to-Sunday week range containing [anchor] */
    fun weekRange(anchor: DateKey): DateRange {
        val dow = dayOfWeekMondayFirst(anchor) // 1..7
        val monday = plusDays(anchor, -(dow - 1))
        val sunday = plusDays(monday, 6)
        return DateRange(monday, sunday)
    }

    /** Returns Monday-first calendar cells for a month (size is multiple of 7) */
    fun buildMonthCalendarCells(ym: YearMonthKey): List<DateKey?> {
        val firstDay = DateKey(ym.year, ym.month, 1)
        val leadingEmpty = dayOfWeekMondayFirst(firstDay) - 1
        val totalDays = daysInMonth(ym)
        val cells = mutableListOf<DateKey?>()
        repeat(leadingEmpty) { cells.add(null) }
        for (d in 1..totalDays) {
            cells.add(DateKey(ym.year, ym.month, d))
        }
        while (cells.size % 7 != 0) {
            cells.add(null)
        }
        return cells
    }

    fun combineDateAndTime(date: DateKey, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.month - 1)
            set(Calendar.DAY_OF_MONTH, date.day)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }

    fun formatDateTimeForFile(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return sdf.format(Date())
    }
}
