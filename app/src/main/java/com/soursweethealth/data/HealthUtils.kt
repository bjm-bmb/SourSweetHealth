package com.soursweethealth.data

object HealthUtils {
    // Blood sugar thresholds (mmol/L) vary by measurement time:
    // 空腹: Normal <6.1, Prediabetes 6.1-7.0, Diabetes ≥7.0
    // 餐后1h: Normal <9.4, Borderline 9.4-11.1, High ≥11.1
    // 餐后2h: Normal <7.8, Prediabetes 7.8-11.1, Diabetes ≥11.1
    // 其他时间: same as 空腹 (conservative)

    private fun bloodSugarThresholds(measureTime: String): Pair<Double, Double> {
        return when {
            measureTime.contains("餐后1") -> 9.4 to 11.1
            measureTime.contains("餐后2") -> 7.8 to 11.1
            else -> 6.1 to 7.0  // 空腹、其他时间
        }
    }

    fun bloodSugarLevel(value: Double, measureTime: String = "空腹（起床后）"): HealthLevel {
        val (normal, veryHigh) = bloodSugarThresholds(measureTime)
        return when {
            value < normal -> HealthLevel.NORMAL
            value < veryHigh -> HealthLevel.HIGH
            else -> HealthLevel.VERY_HIGH
        }
    }

    // Uric acid thresholds (μmol/L)
    // Based on clinical standards:
    // Male: Normal ≤420, Elevated 420-480, High >480
    // Female: Normal ≤360, Elevated 360-420, High >420
    fun uricAcidLevel(value: Double, gender: String = "男"): HealthLevel {
        val (normal, high) = if (gender == "男") 420.0 to 480.0 else 360.0 to 420.0
        return when {
            value <= normal -> HealthLevel.NORMAL
            value <= high -> HealthLevel.HIGH
            else -> HealthLevel.VERY_HIGH
        }
    }

    fun getLevel(type: String, value: Double, gender: String = "男", measureTime: String = "空腹（起床后）"): HealthLevel {
        return if (type == "blood_sugar") bloodSugarLevel(value, measureTime) else uricAcidLevel(value, gender)
    }

    fun getUnit(type: String): String {
        return if (type == "blood_sugar") "mmol/L" else "μmol/L"
    }

    fun getTypeName(type: String): String {
        return if (type == "blood_sugar") "血糖" else "尿酸"
    }

    fun getMeasureTimeOptions(type: String): List<String> {
        return if (type == "blood_sugar") {
            listOf("空腹（起床后）", "餐后1小时", "餐后2小时", "其他时间")
        } else {
            listOf("空腹（起床后）", "餐后1小时", "餐后2小时", "其他时间")
        }
    }

    /** Returns (normalMax, highMax) thresholds for drawing colored zones.
     *  For blood_sugar, thresholds depend on measureTime. */
    fun getThresholds(type: String, gender: String = "男", measureTime: String = "空腹（起床后）"): Pair<Double, Double> {
        return if (type == "blood_sugar") {
            bloodSugarThresholds(measureTime)
        } else {
            if (gender == "男") 420.0 to 480.0 else 360.0 to 420.0
        }
    }

    fun getWarningText(type: String, value: Double, gender: String = "男", measureTime: String = "空腹（起床后）"): String? {
        val level = getLevel(type, value, gender, measureTime)
        if (level == HealthLevel.NORMAL) return null
        val typeName = getTypeName(type)
        val unit = getUnit(type)
        return when (level) {
            HealthLevel.HIGH -> "${typeName}${String.format("%.1f", value)}${unit}，略高于正常范围"
            HealthLevel.VERY_HIGH -> "${typeName}${String.format("%.1f", value)}${unit}，明显偏高，请注意"
            else -> null
        }
    }
}

enum class HealthLevel {
    NORMAL, HIGH, VERY_HIGH
}
