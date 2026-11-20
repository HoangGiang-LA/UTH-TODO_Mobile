package com.example.uth_todo.data.tasks

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object TaskDates {
    private fun isoFormat(utc: Boolean = true) = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        if (utc) timeZone = TimeZone.getTimeZone("UTC")
    }

    fun today(): String = isoFormat(utc = false).format(Date())

    fun isValidDate(value: String): Boolean {
        if (!value.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))) return false
        val position = ParsePosition(0)
        val parsed = isoFormat().parse(value, position) ?: return false
        return position.index == value.length && isoFormat().format(parsed) == value
    }

    fun isValidTime(value: String): Boolean = value.matches(Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))
    fun toUtcMillis(value: String): Long = requireNotNull(isoFormat().parse(value)).time
    fun fromUtcMillis(value: Long): String = isoFormat().format(Date(value))
    fun nextDay(value: String): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT)
        calendar.timeInMillis = toUtcMillis(value)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return fromUtcMillis(calendar.timeInMillis)
    }

    fun heading(value: String): String = SimpleDateFormat("EEEE, d 'tháng' M", Locale.forLanguageTag("vi-VN"))
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(toUtcMillis(value)))

    fun shortDate(value: String): String = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(toUtcMillis(value)))
}
