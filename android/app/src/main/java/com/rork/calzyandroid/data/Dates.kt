package com.rork.calzyandroid.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Calendar helpers shared across the app. Timestamps persist as ISO-8601 instants. */
object Dates {

    fun nowIso(): String = Instant.now().toString()

    fun parse(iso: String): Instant = try {
        Instant.parse(iso)
    } catch (error: Exception) {
        Instant.now()
    }

    fun localDate(iso: String): LocalDate =
        parse(iso).atZone(ZoneId.systemDefault()).toLocalDate()

    fun isSameDay(iso: String, day: LocalDate): Boolean = localDate(iso) == day

    fun isToday(day: LocalDate): Boolean = day == LocalDate.now()

    /**
     * ISO timestamp for logging onto [selected] while keeping the current
     * clock time, so entries logged onto a past day still order correctly.
     */
    fun mergedTimestamp(selected: LocalDate): String {
        if (isToday(selected)) return nowIso()
        val now = LocalDateTime.now()
        return selected
            .atTime(now.hour, now.minute, now.second)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toString()
    }

    fun shortWeekday(day: LocalDate, locale: Locale = Locale.getDefault()): String =
        day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale)

    fun shortTime(iso: String): String =
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            .format(parse(iso).atZone(ZoneId.systemDefault()))

    fun monthDay(iso: String): String = monthDay(localDate(iso))

    fun monthDay(day: LocalDate): String =
        DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()).format(day)

    fun longMonthDay(day: LocalDate): String =
        DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()).format(day)

    fun monthYear(day: LocalDate): String =
        DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()).format(day)

    fun abbreviatedDate(day: LocalDate): String =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()).format(day)

    /** Localised single-letter weekday headers, ordered from Sunday. */
    fun weekdayInitials(locale: Locale = Locale.getDefault()): List<String> {
        val sunday = LocalDate.of(2024, 1, 7)
        return (0..6).map { offset ->
            sunday.plusDays(offset.toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
        }
    }
}
