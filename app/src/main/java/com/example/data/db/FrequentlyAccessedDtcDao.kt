package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FrequentlyAccessedDtcDao {
    @Query("SELECT * FROM frequently_accessed_dtc ORDER BY accessCount DESC, lastAccessTime DESC LIMIT :limit")
    fun getFrequentlyAccessedDtcs(limit: Int): Flow<List<FrequentlyAccessedDtc>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrequentlyAccessedDtc(dtc: FrequentlyAccessedDtc)

    @Query("SELECT * FROM frequently_accessed_dtc WHERE code = :code")
    suspend fun getDtcByCode(code: String): FrequentlyAccessedDtc?

    @Query("DELETE FROM frequently_accessed_dtc")
    suspend fun deleteAllFrequentlyAccessed()
}
