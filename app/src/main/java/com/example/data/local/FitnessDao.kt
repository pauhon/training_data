package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FitnessDao {

    // Daily workouts
    @Query("SELECT * FROM daily_workouts ORDER BY date ASC")
    fun observeAllWorkouts(): Flow<List<DailyWorkoutEntity>>

    @Query("SELECT * FROM daily_workouts WHERE date = :date LIMIT 1")
    fun observeWorkoutByDate(date: String): Flow<DailyWorkoutEntity?>

    @Query("SELECT * FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeWorkoutsInRange(startDate: String, endDate: String): Flow<List<DailyWorkoutEntity>>

    @Query("SELECT * FROM daily_workouts ORDER BY date ASC")
    suspend fun getAllWorkoutsOnce(): List<DailyWorkoutEntity>

    @Query("SELECT * FROM daily_workouts WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getWorkoutsInRangeOnce(startDate: String, endDate: String): List<DailyWorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkout(workout: DailyWorkoutEntity)

    @Query("DELETE FROM daily_workouts WHERE date = :date")
    suspend fun deleteWorkoutByDate(date: String)

    // Weight records
    @Query("SELECT * FROM weight_records ORDER BY date ASC, recordedAtMillis ASC")
    fun observeAllWeights(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE date = :date ORDER BY recordedAtMillis ASC")
    fun observeWeightsByDate(date: String): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, recordedAtMillis ASC")
    fun observeWeightsInRange(startDate: String, endDate: String): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY date DESC, recordedAtMillis DESC LIMIT 1")
    fun observeLatestWeight(): Flow<WeightRecordEntity?>

    @Query("SELECT * FROM weight_records ORDER BY date ASC, recordedAtMillis ASC")
    suspend fun getAllWeightsOnce(): List<WeightRecordEntity>

    @Query("SELECT * FROM weight_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, recordedAtMillis ASC")
    suspend fun getWeightsInRangeOnce(startDate: String, endDate: String): List<WeightRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightRecordEntity): Long

    @Query("DELETE FROM weight_records WHERE id = :id")
    suspend fun deleteWeightById(id: Long)
}
