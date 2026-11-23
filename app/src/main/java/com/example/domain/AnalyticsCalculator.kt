package com.example.domain

import com.example.data.local.DailyWorkoutEntity
import com.example.data.local.WeightRecordEntity
import kotlin.math.roundToInt

enum class PeriodType {
    WEEK,
    MONTH,
}

enum class TrainingTrendMetric(val label: String, val unit: String, val isCardio: Boolean) {
    STRENGTH_TOTAL("力量总计", "组", false),
    UPPER("上肢", "组", false),
    LOWER("下肢", "组", false),
    CRUNCH("弯起", "组", false),
    SITUP("躺起", "组", false),
    RUN("长跑", "km", true),
}

data class CategoryVolumeItem(
    val metric: TrainingTrendMetric,
    val label: String,
    val sets: Int,
    val reps: Int,
    val shareFraction: Float,
)

data class TrainingTrendPoint(
    val dayIndex: Int,
    val date: DateKey,
    val value: Double,
    val repsValue: Int, // only meaningful for strength
)

data class WeightTrendPoint(
    val dayIndex: Int,
    val date: DateKey,
    val weightKg: Double,
    val recordCount: Int,
)

data class PeriodAnalytics(
    val range: DateRange,
    val totalDaysInPeriod: Int,
    val activeWorkoutDays: Int,
    val upperSets: Int,
    val lowerSets: Int,
    val crunchSets: Int,
    val situpSets: Int,
    val totalStrengthSets: Int,
    val totalStrengthReps: Int,
    val totalRunKm: Double,
    val runDays: Int,
    val firstWeightKg: Double?,
    val latestWeightKg: Double?,
    val weightNetChangeKg: Double?,
    val weightRecordCount: Int,
    val strengthCategories: List<CategoryVolumeItem>,
    val trainingTrends: Map<TrainingTrendMetric, List<TrainingTrendPoint>>,
    val weightTrendPoints: List<WeightTrendPoint>,
)

object AnalyticsCalculator {

    fun calculate(
        range: DateRange,
        workouts: List<DailyWorkoutEntity>,
        weights: List<WeightRecordEntity>,
    ): PeriodAnalytics {
        val dates = range.allDates()
        val workoutByDate = workouts.associateBy { it.date }
        val weightsByDate = weights.groupBy { it.date }

        var activeDays = 0
        var upper = 0
        var lower = 0
        var crunch = 0
        var situp = 0
        var totalRun = 0.0
        var runDays = 0

        val trendsMap = TrainingTrendMetric.entries.associateWith { mutableListOf<TrainingTrendPoint>() }
        val weightPoints = mutableListOf<WeightTrendPoint>()

        dates.forEachIndexed { index, dateKey ->
            val iso = dateKey.toIsoString()
            val w = workoutByDate[iso]
            if (w != null && !w.isEmpty) {
                activeDays++
                upper += w.upperSets
                lower += w.lowerSets
                crunch += w.crunchSets
                situp += w.situpSets
                if (w.runKm > 0.0) {
                    totalRun += w.runKm
                    runDays++
                }
            }

            val uSets = w?.upperSets ?: 0
            val lSets = w?.lowerSets ?: 0
            val cSets = w?.crunchSets ?: 0
            val sSets = w?.situpSets ?: 0
            val totalSets = uSets + lSets + cSets + sSets
            val runVal = w?.runKm ?: 0.0

            trendsMap[TrainingTrendMetric.STRENGTH_TOTAL]!!.add(
                TrainingTrendPoint(index, dateKey, totalSets.toDouble(), totalSets * 10),
            )
            trendsMap[TrainingTrendMetric.UPPER]!!.add(
                TrainingTrendPoint(index, dateKey, uSets.toDouble(), uSets * 10),
            )
            trendsMap[TrainingTrendMetric.LOWER]!!.add(
                TrainingTrendPoint(index, dateKey, lSets.toDouble(), lSets * 10),
            )
            trendsMap[TrainingTrendMetric.CRUNCH]!!.add(
                TrainingTrendPoint(index, dateKey, cSets.toDouble(), cSets * 10),
            )
            trendsMap[TrainingTrendMetric.SITUP]!!.add(
                TrainingTrendPoint(index, dateKey, sSets.toDouble(), sSets * 10),
            )
            trendsMap[TrainingTrendMetric.RUN]!!.add(
                TrainingTrendPoint(index, dateKey, round2(runVal), 0),
            )

            // Weight points: ONLY include dates that actually have weigh-in records (skip unweighed days)
            val dayWeights = weightsByDate[iso]
            if (!dayWeights.isNullOrEmpty()) {
                val avg = round1(dayWeights.map { it.weightKg }.average())
                weightPoints.add(
                    WeightTrendPoint(
                        dayIndex = index,
                        date = dateKey,
                        weightKg = avg,
                        recordCount = dayWeights.size,
                    ),
                )
            }
        }

        val totalStrengthSets = upper + lower + crunch + situp
        val totalStrengthReps = totalStrengthSets * 10
        val denom = totalStrengthSets.coerceAtLeast(1).toFloat()

        val categories = listOf(
            CategoryVolumeItem(
                metric = TrainingTrendMetric.UPPER,
                label = "上肢",
                sets = upper,
                reps = upper * 10,
                shareFraction = if (totalStrengthSets > 0) upper / denom else 0f,
            ),
            CategoryVolumeItem(
                metric = TrainingTrendMetric.LOWER,
                label = "下肢",
                sets = lower,
                reps = lower * 10,
                shareFraction = if (totalStrengthSets > 0) lower / denom else 0f,
            ),
            CategoryVolumeItem(
                metric = TrainingTrendMetric.CRUNCH,
                label = "弯起",
                sets = crunch,
                reps = crunch * 10,
                shareFraction = if (totalStrengthSets > 0) crunch / denom else 0f,
            ),
            CategoryVolumeItem(
                metric = TrainingTrendMetric.SITUP,
                label = "躺起",
                sets = situp,
                reps = situp * 10,
                shareFraction = if (totalStrengthSets > 0) situp / denom else 0f,
            ),
        )

        val sortedWeights = weights.sortedWith(compareBy<WeightRecordEntity> { it.date }.thenBy { it.recordedAtMillis })
        val firstWeight = sortedWeights.firstOrNull()?.weightKg
        val latestWeight = sortedWeights.lastOrNull()?.weightKg
        val netChange = if (sortedWeights.size >= 2 && firstWeight != null && latestWeight != null) {
            round1(latestWeight - firstWeight)
        } else {
            null
        }

        return PeriodAnalytics(
            range = range,
            totalDaysInPeriod = dates.size,
            activeWorkoutDays = activeDays,
            upperSets = upper,
            lowerSets = lower,
            crunchSets = crunch,
            situpSets = situp,
            totalStrengthSets = totalStrengthSets,
            totalStrengthReps = totalStrengthReps,
            totalRunKm = round2(totalRun),
            runDays = runDays,
            firstWeightKg = firstWeight,
            latestWeightKg = latestWeight,
            weightNetChangeKg = netChange,
            weightRecordCount = sortedWeights.size,
            strengthCategories = categories,
            trainingTrends = trendsMap,
            weightTrendPoints = weightPoints,
        )
    }

    fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0

    fun round2(value: Double): Double = (value * 100.0).roundToInt() / 100.0
}
