package com.soursweethealth.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)
}

@Dao
interface HealthRecordDao {
    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY date DESC, createdAt DESC")
    fun getRecordsByUser(userId: Long): Flow<List<HealthRecord>>

    @Query("SELECT * FROM records WHERE userId = :userId AND type = :type ORDER BY date DESC, createdAt DESC")
    fun getRecordsByUserAndType(userId: Long, type: String): Flow<List<HealthRecord>>

    @Query("SELECT * FROM records WHERE userId = :userId AND type = :type AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getRecordsByDateRange(userId: Long, type: String, startDate: String, endDate: String): Flow<List<HealthRecord>>

    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY date DESC, createdAt DESC LIMIT 1")
    fun getLatestRecord(userId: Long): Flow<HealthRecord?>

    @Query("SELECT * FROM records WHERE userId = :userId AND type = :type ORDER BY date DESC, createdAt DESC LIMIT 1")
    fun getLatestRecordByType(userId: Long, type: String): Flow<HealthRecord?>

    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY date ASC")
    suspend fun getAllRecordsForUser(userId: Long): List<HealthRecord>

    @Query("SELECT * FROM records WHERE userId = :userId ORDER BY date DESC, createdAt DESC LIMIT :limit")
    suspend fun getLatestRecordsForUser(userId: Long, limit: Int): List<HealthRecord>

    @Insert
    suspend fun insert(record: HealthRecord)

    @Update
    suspend fun update(record: HealthRecord)

    @Delete
    suspend fun delete(record: HealthRecord)
}
