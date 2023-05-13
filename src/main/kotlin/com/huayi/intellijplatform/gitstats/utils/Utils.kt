package com.huayi.intellijplatform.gitstats.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.*

object Utils {
    fun getThisWeekDateRange(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        val startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return Pair(startOfWeek, endOfWeek)
    }

    fun getThisWeekDateTimeRange(): Pair<Date, Date> {
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
}