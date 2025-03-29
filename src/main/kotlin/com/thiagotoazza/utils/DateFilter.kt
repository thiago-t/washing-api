package com.thiagotoazza.utils

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth as YMonth
import java.time.ZoneOffset

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

}