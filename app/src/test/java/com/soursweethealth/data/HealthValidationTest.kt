package com.soursweethealth.data

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HealthValidationTest {
    private val now = LocalDateTime.of(2026, 7, 12, 12, 0)

    @Test
    fun record_rejectsInvalidAndFutureInput() {
        assertNotNull(HealthValidation.validateRecord("blood_sugar", 0.0, "2026-07-12", "10:00", now))
        assertNotNull(HealthValidation.validateRecord("uric_acid", 420.0, "2026-02-30", "10:00", now))
        assertNotNull(HealthValidation.validateRecord("uric_acid", 420.0, "2026-07-12", "12:01", now))
    }

    @Test
    fun record_acceptsValidInput() {
        assertNull(HealthValidation.validateRecord("blood_sugar", 5.6, "2026-07-12", "11:59", now))
    }

    @Test
    fun user_rejectsImpossibleBirthMonth() {
        assertNotNull(HealthValidation.validateUser("张三", 1980, 13, LocalDate.of(2026, 7, 12)))
        assertNull(HealthValidation.validateUser("张三", 1980, 12, LocalDate.of(2026, 7, 12)))
    }

    @Test
    fun age_accountsForBirthMonth() {
        val today = LocalDate.of(2026, 7, 12)
        assertEquals(26, HealthValidation.age(2000, 6, today))
        assertEquals(25, HealthValidation.age(2000, 8, today))
    }
}
