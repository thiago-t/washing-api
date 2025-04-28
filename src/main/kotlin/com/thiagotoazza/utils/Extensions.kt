package com.thiagotoazza.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.bson.types.ObjectId

/*
 * Output example: 2000-12-28
 */
fun Long.toShortDate(): String {
    return Instant.ofEpochMilli(this).let { instant ->
        LocalDate.ofInstant(instant, ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE)
    }
}

fun Int.asDecimalString(): String {
    return String.format("%.2f", this / 100.0)
}

fun String?.isValidObjectId(): Boolean {
    return this != null && ObjectId.isValid(this)
}