package com.artsiom.footballpulse.ui.presentation

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class UiDates(private val zone: ZoneId = ZoneId.systemDefault()) {
    private val dateFormat = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH)
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    private fun parse(value: String) = try {
        Instant.parse(value).atZone(zone)
    } catch (_: DateTimeParseException) {
        null
    }

    fun localDate(value: String): LocalDate? = parse(value)?.toLocalDate()
    fun headerLabel(value: String): String? = parse(value)?.format(dateFormat)
    fun dateLabel(value: String): String = headerLabel(value) ?: value
    fun timeLabel(value: String): String? = parse(value)?.format(timeFormat)
}
