package com.soursweethealth.data

object HealthUtils {
    // Blood sugar thresholds (mmol/L)
    // Based on ADA (American Diabetes Association) standards:
    // Fasting: Normal <5.6, Prediabetes 5.6-6.9, Diabetes ≥7.0
    // Postprandial 2h: Normal <7.8, Prediabetes 7.8-11.0, Diabetes ≥11.1
    fun bloodSugarLevel(value: Double): HealthLevel {
        return when {
            value < 6.1 -> HealthLevel.NORMAL
            value < 7.0 -> HealthLevel.HIGH
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

    fun getLevel(type: String, value: Double, gender: String = "男"): HealthLevel {
        return if (type == "blood_sugar") bloodSugarLevel(value) else uricAcidLevel(value, gender)
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

    /** Returns (normalMax, highMax) thresholds for drawing colored zones */
    fun getThresholds(type: String, gender: String = "男"): Pair<Double, Double> {
        return if (type == "blood_sugar") {
            6.1 to 7.0
        } else {
            if (gender == "男") 420.0 to 480.0 else 360.0 to 420.0
        }
    }

    fun getWarningText(type: String, value: Double, gender: String = "男"): String? {
        val level = getLevel(type, value, gender)
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
