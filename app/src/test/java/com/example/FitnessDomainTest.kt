package com.example

import com.example.data.export.ExportFormatter
import com.example.data.local.DailyWorkoutEntity
import com.example.data.local.WeightRecordEntity
import com.example.domain.AnalyticsCalculator
import com.example.domain.DateKey
import com.example.domain.DateRange
import com.example.domain.DateUtil
import com.example.domain.TrainingTrendMetric
import com.example.domain.YearMonthKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessDomainTest {

    @Test
    fun `strength sets convert at fixed 1 to 10 reps ratio`() {
        val workout = DailyWorkoutEntity(
            date = "2026-11-23",
            upperSets = 4,
            lowerSets = 3,
            crunchSets = 2,
            situpSets = 1,
            runKm = 5.25,
        )
        assertEquals(40, workout.upperReps)
        assertEquals(30, workout.lowerReps)
        assertEquals(20, workout.crunchReps)
        assertEquals(10, workout.situpReps)
        assertEquals(10, workout.totalStrengthSets)
        assertEquals(100, workout.totalStrengthReps)
        assertTrue(workout.hasStrength)
        assertTrue(workout.hasRun)
        assertFalse(workout.isEmpty)
    }

    @Test
    fun `analytics skips unweighed days and computes accurate net weight change`() {
        val range = DateRange(DateKey(2026, 11, 16), DateKey(2026, 11, 22))
        val workouts = listOf(
            DailyWorkoutEntity("2026-11-16", upperSets = 5, lowerSets = 3, runKm = 4.5),
            DailyWorkoutEntity("2026-11-19", crunchSets = 4, situpSets = 4, runKm = 0.0),
        )
        val weights = listOf(
            WeightRecordEntity(id = 1, date = "2026-11-16", weightKg = 70.0, recordedAtMillis = 1000L),
            WeightRecordEntity(id = 2, date = "2026-11-16", weightKg = 70.4, recordedAtMillis = 2000L),
            WeightRecordEntity(id = 3, date = "2026-11-21", weightKg = 69.2, recordedAtMillis = 3000L),
        )

        val analytics = AnalyticsCalculator.calculate(range, workouts, weights)

        assertEquals(2, analytics.activeWorkoutDays)
        assertEquals(16, analytics.totalStrengthSets)
        assertEquals(160, analytics.totalStrengthReps)
        assertEquals(4.5, analytics.totalRunKm, 0.001)
        assertEquals(1, analytics.runDays)
        assertEquals(-0.8, analytics.weightNetChangeKg!!, 0.001)

        // Only 2 days had weight logs; unweighed days are NOT padded with 0
        assertEquals(2, analytics.weightTrendPoints.size)
        assertEquals(0, analytics.weightTrendPoints[0].dayIndex)
        assertEquals(70.2, analytics.weightTrendPoints[0].weightKg, 0.001)
        assertEquals(5, analytics.weightTrendPoints[1].dayIndex)
        assertEquals(69.2, analytics.weightTrendPoints[1].weightKg, 0.001)

        // Training trend covers all 7 days
        assertEquals(7, analytics.trainingTrends[TrainingTrendMetric.STRENGTH_TOTAL]!!.size)
    }

    @Test
    fun `csv export starts with utf8 bom and includes converted reps and weight details`() {
        val workouts = listOf(
            DailyWorkoutEntity("2026-11-23", upperSets = 3, lowerSets = 2, runKm = 3.5),
        )
        val weights = listOf(
            WeightRecordEntity(id = 1, date = "2026-11-23", weightKg = 68.4, recordedAtMillis = 1700000000000L, note = "晨起,空腹"),
        )
        val csv = ExportFormatter.generateCsv(workouts, weights)
        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("上肢(组),上肢(个)"))
        assertTrue(csv.contains("2026-11-23,3,30,2,20,0,0,0,0,5,50,3.5,68.4,"))
        assertTrue(csv.contains("\"")) // escaped comma in note
    }

    @Test
    fun `month calendar grid aligns monday first and has multiple of 7 cells`() {
        val cells = DateUtil.buildMonthCalendarCells(YearMonthKey(2026, 11))
        assertEquals(0, cells.size % 7)
        val nonNullDays = cells.filterNotNull()
        assertEquals(30, nonNullDays.size)
        assertEquals(DateKey(2026, 11, 1), nonNullDays.first())
        assertEquals(DateKey(2026, 11, 30), nonNullDays.last())
    }
}
