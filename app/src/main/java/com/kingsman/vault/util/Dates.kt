package com.kingsman.vault.util

import com.kingsman.vault.data.Frequency
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object Dates {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfDay(d: LocalDate): Long = d.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toLocalDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun today(): LocalDate = LocalDate.now(zone)

    /** Chosen day with the current clock time, so entries made today sort naturally. */
    fun withNowTime(d: LocalDate): Long =
        d.atTime(LocalTime.now(zone)).atZone(zone).toInstant().toEpochMilli()

    fun monthRange(ym: YearMonth): Pair<Long, Long> =
        startOfDay(ym.atDay(1)) to startOfDay(ym.plusMonths(1).atDay(1))

    fun yearRange(year: Int): Pair<Long, Long> =
        startOfDay(LocalDate.of(year, 1, 1)) to startOfDay(LocalDate.of(year + 1, 1, 1))

    /** Inclusive day range -> [from, to) millis. */
    fun dayRange(from: LocalDate, to: LocalDate): Pair<Long, Long> =
        startOfDay(from) to startOfDay(to.plusDays(1))

    fun advance(ms: Long, frequency: String): Long {
        val d = toLocalDate(ms)
        val next = when (frequency) {
            Frequency.WEEKLY -> d.plusWeeks(1)
            Frequency.QUARTERLY -> d.plusMonths(3)
            Frequency.YEARLY -> d.plusYears(1)
            else -> d.plusMonths(1)
        }
        return startOfDay(next)
    }

    // Material DatePicker works in UTC midnight millis.
    fun toPickerMillis(ms: Long): Long =
        toLocalDate(ms).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(utc: Long): LocalDate =
        Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()

    private val dayFmt = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val shortFmt = DateTimeFormatter.ofPattern("d MMM")
    private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val monShort = DateTimeFormatter.ofPattern("MMM")
    private val stampFmt = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")

    fun fmt(ms: Long): String = toLocalDate(ms).format(dayFmt)
    fun fmtShort(ms: Long): String = toLocalDate(ms).format(shortFmt)
    fun fmtMonth(ym: YearMonth): String = ym.format(monthFmt)
    fun fmtMonShort(ym: YearMonth): String = ym.format(monShort)
    fun stamp(): String = java.time.LocalDateTime.now(zone).format(stampFmt)

    fun dayLabel(ms: Long): String {
        val d = toLocalDate(ms)
        val t = today()
        return when (d) {
            t -> "Today"
            t.minusDays(1) -> "Yesterday"
            else -> d.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
        }
    }
}
