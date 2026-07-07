package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dtc_records")
data class DtcRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val carModel: String = "Kia Carens 2008 CRDi",
    val status: String // "Active", "Pending", "Cleared"
)
