package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_workouts")
data class DailyWorkoutEntity(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd
    val upperSets: Int = 0,
    val lowerSets: Int = 0,
    val crunchSets: Int = 0,
    val situpSets: Int = 0,
    val runKm: Double = 0.0,
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    val upperReps: Int get() = upperSets * REPS_PER_SET
    val lowerReps: Int get() = lowerSets * REPS_PER_SET
    val crunchReps: Int get() = crunchSets * REPS_PER_SET
    val situpReps: Int get() = situpSets * REPS_PER_SET

    val totalStrengthSets: Int get() = upperSets + lowerSets + crunchSets + situpSets
    val totalStrengthReps: Int get() = totalStrengthSets * REPS_PER_SET

    val hasStrength: Boolean get() = totalStrengthSets > 0
    val hasRun: Boolean get() = runKm > 0.0001
    val isEmpty: Boolean get() = !hasStrength && !hasRun

    companion object {
        const val REPS_PER_SET = 10
    }
}
