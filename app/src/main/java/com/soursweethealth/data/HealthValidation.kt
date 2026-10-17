package com.soursweethealth.data

import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object HealthValidation {
    private val dateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
        .withResolverStyle(ResolverStyle.STRICT)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withResolverStyle(ResolverStyle.STRICT)

    fun validateRecord(
        type: String,
        value: Double?,
        dateText: String,
        timeText: String,
        now: LocalDateTime = LocalDateTime.now()
    ): String? {
        if (type !in setOf("blood_sugar", "uric_acid")) return "不支持的记录类别"
        if (value == null || !value.isFinite()) return "请输入有效数值"
        val range = if (type == "blood_sugar") 1.0..50.0 else 50.0..1500.0
        if (value !in range) {
            return if (type == "blood_sugar") "血糖应在 1.0–50.0 mmol/L 之间" else "尿酸应在 50–1500 μmol/L 之间"
        }

        val measured = parseDateTime(dateText, timeText) ?: return "请输入有效的日期和时间"
        if (measured.isAfter(now)) return "测量时间不能晚于当前时间"
        return null
    }

    fun parseDateTime(dateText: String, timeText: String): LocalDateTime? = try {
        LocalDateTime.of(LocalDate.parse(dateText, dateFormatter), LocalTime.parse(timeText, timeFormatter))
    } catch (_: DateTimeException) {
        null
    }

    fun toEpochMillis(dateText: String, timeText: String, zoneId: ZoneId = ZoneId.systemDefault()): Long? =
        parseDateTime(dateText, timeText)?.atZone(zoneId)?.toInstant()?.toEpochMilli()

    fun validateUser(name: String, birthYear: Int?, birthMonth: Int?, today: LocalDate = LocalDate.now()): String? {
        if (name.trim().isEmpty()) return "请输入姓名"
        if (name.trim().length > 30) return "姓名不能超过 30 个字符"
        if (birthYear == null || birthYear !in 1900..today.year) return "请输入 1900–${today.year} 之间的出生年"
        if (birthMonth == null || birthMonth !in 1..12) return "出生月份应为 1–12"
        if (YearMonth.of(birthYear, birthMonth).isAfter(YearMonth.from(today))) return "出生年月不能晚于当前月份"
        return null
    }

    fun age(birthYear: Int, birthMonth: Int, today: LocalDate = LocalDate.now()): Int {
        val beforeBirthdayMonth = today.monthValue < birthMonth
        return (today.year - birthYear - if (beforeBirthdayMonth) 1 else 0).coerceAtLeast(0)
    }
}
