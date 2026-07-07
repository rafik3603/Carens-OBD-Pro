package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DtcDao {
    @Query("SELECT * FROM dtc_records ORDER BY timestamp DESC")
    fun getAllDtcRecords(): Flow<List<DtcRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DtcRecord)

    @Query("UPDATE dtc_records SET status = 'Cleared' WHERE status != 'Cleared'")
    suspend fun clearActiveRecords()

    @Query("DELETE FROM dtc_records")
    suspend fun deleteAll()
}
