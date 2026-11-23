package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weight_records",
    indices = [Index(value = ["date"])],
)
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val date: String, // ISO yyyy-MM-dd
    val weightKg: Double,
    val recordedAtMillis: Long,
    val note: String = "",
)
