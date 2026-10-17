package com.soursweethealth.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthUtilsTest {
    @Test
    fun bloodSugar_detectsLowAndVeryLowValues() {
        assertEquals(HealthLevel.VERY_LOW, HealthUtils.bloodSugarLevel(2.9))
        assertEquals(HealthLevel.LOW, HealthUtils.bloodSugarLevel(3.0))
        assertEquals(HealthLevel.LOW, HealthUtils.bloodSugarLevel(3.8))
        assertEquals(HealthLevel.NORMAL, HealthUtils.bloodSugarLevel(3.9))
    }

    @Test
    fun bloodSugar_usesMeasurementContextThresholds() {
        assertEquals(HealthLevel.VERY_HIGH, HealthUtils.bloodSugarLevel(7.0, "空腹（起床后）"))
        assertEquals(HealthLevel.NORMAL, HealthUtils.bloodSugarLevel(8.0, "餐后1小时"))
        assertEquals(HealthLevel.HIGH, HealthUtils.bloodSugarLevel(8.0, "餐后2小时"))
    }

    @Test
    fun uricAcid_usesGenderThresholds() {
        assertEquals(HealthLevel.NORMAL, HealthUtils.uricAcidLevel(420.0, "男"))
        assertEquals(HealthLevel.HIGH, HealthUtils.uricAcidLevel(420.0, "女"))
    }
}
