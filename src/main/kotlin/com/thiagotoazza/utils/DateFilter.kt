package com.thiagotoazza.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.Year as JYear
import java.time.YearMonth as YMonth

sealed class DateFilter {
    abstract val startDate: Instant
    abstract val endDate: Instant

    class FullDate(year: Int, month: Int, day: Int) : DateFilter() {
        override val startDate: Instant = LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC).toInstant()
        override val endDate: Instant = startDate.plusSeconds(86399)
    }

    class YearMonth(year: Int, month: Int) : DateFilter() {
        override val startDate: Instant = YMonth.of(year, month).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        override val endDate: Instant =
            YMonth.of(year, month).atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
    }

    class Year(year: Int) : DateFilter() {
        override val startDate: Instant = LocalDate.of(year, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()
        override val endDate: Instant = LocalDate.of(year, 12, 31).atTime(23, 59, 59).toInstant(ZoneOffset.UTC)
    }

    companion object {
        fun parse(dateQueryParam: String): DateFilter {
            return when (dateQueryParam.count { it == '-' }) {
                2 -> {
                    val date = LocalDate.parse(dateQueryParam, DateTimeFormatter.ISO_LOCAL_DATE)
                    FullDate(date.year, date.monthValue, date.dayOfMonth)
                }

                1 -> {
                    val yearMonth = YMonth.parse(dateQueryParam, DateTimeFormatter.ofPattern("yyyy-MM"))
                    YearMonth(yearMonth.year, yearMonth.monthValue)
                }

                else -> {
                    val year = JYear.parse(dateQueryParam, DateTimeFormatter.ofPattern("yyyy"))
                    Year(year.value)
                }
            }
        }
    }

}