package com.thiagotoazza.utils

sealed class DateFilter(val aggregationFormat: String, val regex: String) {
    class FullDate(
        year: String,
        month: String,
        day: String
    ) : DateFilter(aggregationFormat = "%Y-%m-%d", regex = "^$year-$month-$day")

    class MonthYear(
        year: String,
        month: String
    ) : DateFilter(aggregationFormat = "%Y-%m", regex = "^$year-$month")

    class Year(year: String) : DateFilter(aggregationFormat = "%Y", regex = "^$year")
}