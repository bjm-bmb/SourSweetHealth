package com.soursweethealth.data

import androidx.room.*

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val gender: String, // "男" or "女"
    val birthYear: Int,
    val birthMonth: Int,
    val avatarColor: Int = 0 // index into preset colors
)

@Entity(
    tableName = "records",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val type: String, // "blood_sugar" or "uric_acid"
    val value: Double,
    val date: String, // yyyy-MM-dd
    val measureTime: String, // 起床后, 餐后1小时, 餐后2小时, 其他时间
    val createdAt: Long = System.currentTimeMillis()
)
