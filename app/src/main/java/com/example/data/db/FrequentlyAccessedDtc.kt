package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "frequently_accessed_dtc")
data class FrequentlyAccessedDtc(
    @PrimaryKey val code: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val category: String,
    val accessCount: Int,
    val lastAccessTime: Long = System.currentTimeMillis()
)
