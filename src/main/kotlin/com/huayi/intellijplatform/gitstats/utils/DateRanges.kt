package com.huayi.intellijplatform.gitstats.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

object DateRanges {
    fun formatDate(date: Date): String = SimpleDateFormat(DATE_PATTERN).format(date)

    fun parseDateRange(value: String): Pair<Date, Date>? {
        val match = DATE_RANGE_PATTERN.matchEntire(value.trim()) ?: return null
        val start = parseDate(match.groupValues[1]) ?: return null
        val end = parseDate(match.groupValues[2]) ?: return null
        return orderedRange(startOfDay(start), endOfDay(end))
    }

    fun orderedRange(startDate: Date, endDate: Date): Pair<Date, Date> {
        return if (startDate.after(endDate)) {
            Pair(startOfDay(endDate), endOfDay(startDate))
        } else {
            Pair(startDate, endDate)
        }
    }

    fun startOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    fun endOfDay(date: Date): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    fun thisWeekDateTimeRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.time = today
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND))
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND))
        val startOfWeek = calendar.time
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMaximum(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND))
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND))
        val endOfWeek = calendar.time
        return Pair(startOfWeek, endOfWeek)
    }

    fun lastSevenDaysDateRange(): Pair<Date, Date> {
        val endCalendar = Calendar.getInstance()
        val startCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -6)
        }
        return Pair(startOfDay(startCalendar.time), endOfDay(endCalendar.time))
    }

    fun thisMonthDateRange(): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val startCalendar = calendar.clone() as Calendar
        startCalendar.set(Calendar.DAY_OF_MONTH, 1)
        val endCalendar = calendar.clone() as Calendar
        endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        return Pair(startOfDay(startCalendar.time), endOfDay(endCalendar.time))
    }

    private fun parseDate(value: String): Date? {
        return try {
            SimpleDateFormat(DATE_PATTERN).apply {
                isLenient = false
            }.parse(value)
        } catch (_: ParseException) {
            null
        }
    }

    private const val DATE_PATTERN = "yyyy-MM-dd"
    private val DATE_RANGE_PATTERN = Regex("""^(\d{4}-\d{2}-\d{2})\s*(?:-|~|to|至)\s*(\d{4}-\d{2}-\d{2})$""")
}
