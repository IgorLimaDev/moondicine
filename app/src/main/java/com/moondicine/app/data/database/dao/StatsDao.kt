package com.moondicine.app.data.database.dao

import androidx.room.*
import com.moondicine.app.data.database.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity): Long

    @Update
    suspend fun update(stats: UserStatsEntity)

    @Query("SELECT * FROM user_stats WHERE specialty = :specialty LIMIT 1")
    suspend fun getBySpecialty(specialty: String): UserStatsEntity?

    @Query("SELECT * FROM user_stats ORDER BY specialty ASC")
    fun getAllFlow(): Flow<List<UserStatsEntity>>

    @Query("SELECT * FROM user_stats ORDER BY specialty ASC")
    suspend fun getAll(): List<UserStatsEntity>

    @Query("DELETE FROM user_stats")
    suspend fun deleteAll()
}
