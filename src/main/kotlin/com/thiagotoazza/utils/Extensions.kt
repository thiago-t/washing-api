package com.thiagotoazza.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/*
 * Output example: 2000-12-28
 */
fun Long.toShortDate(): String {
    return Instant.ofEpochMilli(this).let { instant ->
        LocalDate.ofInstant(instant, ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE)
    }
}
