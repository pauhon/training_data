package com.example.data.repository

import com.example.data.local.DailyWorkoutEntity
import com.example.data.local.FitnessDao
import com.example.data.local.WeightRecordEntity
import com.example.domain.DateRange
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

class FitnessRepository(private val dao: FitnessDao) {

    val allWorkouts: Flow<List<DailyWorkoutEntity>> = dao.observeAllWorkouts()
    val allWeights: Flow<List<WeightRecordEntity>> = dao.observeAllWeights()
    val latestWeight: Flow<WeightRecordEntity?> = dao.observeLatestWeight()

    fun observeWorkoutByDate(dateIso: String): Flow<DailyWorkoutEntity?> =
        dao.observeWorkoutByDate(dateIso)

    fun observeWeightsByDate(dateIso: String): Flow<List<WeightRecordEntity>> =
        dao.observeWeightsByDate(dateIso)

    fun observeWorkoutsInRange(range: DateRange): Flow<List<DailyWorkoutEntity>> =
        dao.observeWorkoutsInRange(range.start.toIsoString(), range.end.toIsoString())

    fun observeWeightsInRange(range: DateRange): Flow<List<WeightRecordEntity>> =
        dao.observeWeightsInRange(range.start.toIsoString(), range.end.toIsoString())

    /**
     * Saves or overwrites the daily workout summary.
     * Enforces non-negative integer sets and 2-decimal precision for runKm.
     * Per specification: when all fields are zero, clears/deletes the day's record.
     */
    suspend fun saveDailyWorkout(
        dateIso: String,
        upperSets: Int,
        lowerSets: Int,
        crunchSets: Int,
        situpSets: Int,
        runKm: Double,
    ) {
        val safeUpper = upperSets.coerceAtLeast(0)
        val safeLower = lowerSets.coerceAtLeast(0)
        val safeCrunch = crunchSets.coerceAtLeast(0)
        val safeSitup = situpSets.coerceAtLeast(0)
        val safeRun = (runKm.coerceAtLeast(0.0) * 100.0).roundToInt() / 100.0

        val totalSets = safeUpper + safeLower + safeCrunch + safeSitup
        if (totalSets == 0 && safeRun <= 0.0) {
            dao.deleteWorkoutByDate(dateIso)
        } else {
            dao.upsertWorkout(
                DailyWorkoutEntity(
                    date = dateIso,
                    upperSets = safeUpper,
                    lowerSets = safeLower,
                    crunchSets = safeCrunch,
                    situpSets = safeSitup,
                    runKm = safeRun,
                    updatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun clearDailyWorkout(dateIso: String) {
        dao.deleteWorkoutByDate(dateIso)
    }

    suspend fun addWeightRecord(
        dateIso: String,
        weightKg: Double,
        recordedAtMillis: Long,
        note: String,
    ) {
        val safeWeight = (weightKg.coerceIn(20.0, 300.0) * 10.0).roundToInt() / 10.0
        dao.insertWeight(
            WeightRecordEntity(
                date = dateIso,
                weightKg = safeWeight,
                recordedAtMillis = recordedAtMillis,
                note = note.trim(),
            ),
        )
    }

    suspend fun deleteWeightRecord(id: Long) {
        dao.deleteWeightById(id)
    }

    suspend fun getWorkoutsForExport(range: DateRange?): List<DailyWorkoutEntity> =
        if (range == null) {
            dao.getAllWorkoutsOnce()
        } else {
            dao.getWorkoutsInRangeOnce(range.start.toIsoString(), range.end.toIsoString())
        }

    suspend fun getWeightsForExport(range: DateRange?): List<WeightRecordEntity> =
        if (range == null) {
            dao.getAllWeightsOnce()
        } else {
            dao.getWeightsInRangeOnce(range.start.toIsoString(), range.end.toIsoString())
        }
}
