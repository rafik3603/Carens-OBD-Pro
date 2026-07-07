package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historical_trend_points")
data class HistoricalTrendPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val rpm: Int,
    val speed: Int,
    val coolantTemp: Int,
    val fuelPressure: Int,
    val turboBoostPressure: Double,
    val batteryVoltage: Double,
    val engineLoad: Int,
    val dpfSootLevel: Int,
    val oilPressure: Double
)
