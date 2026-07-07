package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricalTrendPointDao {
    @Query("SELECT * FROM historical_trend_points ORDER BY timestamp DESC")
    fun getAllTrendPoints(): Flow<List<HistoricalTrendPoint>>

    @Query("SELECT * FROM historical_trend_points ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestTrendPoints(limit: Int): Flow<List<HistoricalTrendPoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrendPoint(point: HistoricalTrendPoint)

    @Query("DELETE FROM historical_trend_points")
    suspend fun deleteAllTrendPoints()

    @Query("SELECT COUNT(*) FROM historical_trend_points")
    suspend fun getCount(): Int

    @Query("DELETE FROM historical_trend_points WHERE id IN (SELECT id FROM historical_trend_points ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestTrendPoints(count: Int)
}
