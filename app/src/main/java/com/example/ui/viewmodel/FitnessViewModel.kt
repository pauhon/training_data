package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.export.ExportFormat
import com.example.data.export.ExportFormatter
import com.example.data.export.ExportScope
import com.example.data.local.DailyWorkoutEntity
import com.example.data.local.WeightRecordEntity
import com.example.data.repository.FitnessRepository
import com.example.domain.AnalyticsCalculator
import com.example.domain.DateKey
import com.example.domain.DateRange
import com.example.domain.DateUtil
import com.example.domain.PeriodAnalytics
import com.example.domain.PeriodType
import com.example.domain.TrainingTrendMetric
import com.example.domain.YearMonthKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DayMarker(
    val hasStrength: Boolean = false,
    val hasRun: Boolean = false,
    val hasWeight: Boolean = false,
)

data class CalendarUiState(
    val today: DateKey = DateUtil.today(),
    val displayedMonth: YearMonthKey = DateUtil.currentYearMonth(),
    val isCurrentMonth: Boolean = true,
    val cells: List<DateKey?> = DateUtil.buildMonthCalendarCells(DateUtil.currentYearMonth()),
    val dayMarkers: Map<String, DayMarker> = emptyMap(),
    val todayWorkout: DailyWorkoutEntity? = null,
    val todayWeights: List<WeightRecordEntity> = emptyList(),
    val latestWeight: WeightRecordEntity? = null,
    val monthActiveDays: Int = 0,
    val monthStrengthSets: Int = 0,
    val monthRunKm: Double = 0.0,
)

data class DayDetailData(
    val workout: DailyWorkoutEntity? = null,
    val weights: List<WeightRecordEntity> = emptyList(),
)

data class DashboardUiState(
    val periodType: PeriodType = PeriodType.WEEK,
    val periodOffset: Int = 0,
    val periodLabel: String = "",
    val periodSubLabel: String = "",
    val periodFileTag: String = "",
    val canGoNext: Boolean = false,
    val selectedMetric: TrainingTrendMetric = TrainingTrendMetric.STRENGTH_TOTAL,
    val analytics: PeriodAnalytics = AnalyticsCalculator.calculate(
        range = DateUtil.weekRange(DateUtil.today()),
        workouts = emptyList(),
        weights = emptyList(),
    ),
)

data class ExportPayload(
    val fileName: String,
    val mimeType: String,
    val content: String,
    val recordCount: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModel(
    private val repository: FitnessRepository,
) : ViewModel() {

    private val displayedMonthFlow = MutableStateFlow(DateUtil.currentYearMonth())
    private val periodTypeFlow = MutableStateFlow(PeriodType.WEEK)
    private val periodOffsetFlow = MutableStateFlow(0)
    private val selectedTrendMetricFlow = MutableStateFlow(TrainingTrendMetric.STRENGTH_TOTAL)

    val calendarUiState: StateFlow<CalendarUiState> = combine(
        displayedMonthFlow,
        repository.allWorkouts,
        repository.allWeights,
        repository.latestWeight,
    ) { displayedMonth, allWorkouts, allWeights, latestWeight ->
        val today = DateUtil.today()
        val todayIso = today.toIsoString()
        val monthRange = DateUtil.monthRange(displayedMonth)
        val startIso = monthRange.start.toIsoString()
        val endIso = monthRange.end.toIsoString()

        val markerMap = mutableMapOf<String, DayMarker>()
        var monthActiveDays = 0
        var monthStrengthSets = 0
        var monthRunKm = 0.0

        for (w in allWorkouts) {
            if (!w.isEmpty) {
                val existing = markerMap[w.date] ?: DayMarker()
                markerMap[w.date] = existing.copy(
                    hasStrength = w.hasStrength,
                    hasRun = w.hasRun,
                )
                if (w.date in startIso..endIso) {
                    monthActiveDays++
                    monthStrengthSets += w.totalStrengthSets
                    monthRunKm += w.runKm
                }
            }
        }

        for (rec in allWeights) {
            val existing = markerMap[rec.date] ?: DayMarker()
            markerMap[rec.date] = existing.copy(hasWeight = true)
        }

        val todayWorkout = allWorkouts.firstOrNull { it.date == todayIso }
        val todayWeights = allWeights.filter { it.date == todayIso }

        CalendarUiState(
            today = today,
            displayedMonth = displayedMonth,
            isCurrentMonth = displayedMonth == today.toYearMonth(),
            cells = DateUtil.buildMonthCalendarCells(displayedMonth),
            dayMarkers = markerMap,
            todayWorkout = todayWorkout,
            todayWeights = todayWeights,
            latestWeight = latestWeight,
            monthActiveDays = monthActiveDays,
            monthStrengthSets = monthStrengthSets,
            monthRunKm = AnalyticsCalculator.round2(monthRunKm),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(),
    )

    private val currentRangeFlow: Flow<Triple<PeriodType, Int, DateRange>> = combine(
        periodTypeFlow,
        periodOffsetFlow,
    ) { type, offset ->
        val today = DateUtil.today()
        val range = when (type) {
            PeriodType.WEEK -> {
                val anchor = DateUtil.plusDays(today, offset * 7)
                DateUtil.weekRange(anchor)
            }
            PeriodType.MONTH -> {
                val ym = today.toYearMonth().plusMonths(offset)
                DateUtil.monthRange(ym)
            }
        }
        Triple(type, offset, range)
    }

    val dashboardUiState: StateFlow<DashboardUiState> = currentRangeFlow
        .flatMapLatest { (type, offset, range) ->
            combine(
                repository.observeWorkoutsInRange(range),
                repository.observeWeightsInRange(range),
                selectedTrendMetricFlow,
            ) { workouts, weights, metric ->
                val analytics = AnalyticsCalculator.calculate(range, workouts, weights)
                val (label, subLabel, fileTag) = buildPeriodLabels(type, offset, range)
                DashboardUiState(
                    periodType = type,
                    periodOffset = offset,
                    periodLabel = label,
                    periodSubLabel = subLabel,
                    periodFileTag = fileTag,
                    canGoNext = offset < 0,
                    selectedMetric = metric,
                    analytics = analytics,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(),
        )

    fun observeDayDetail(dateIso: String): Flow<DayDetailData> = combine(
        repository.observeWorkoutByDate(dateIso),
        repository.observeWeightsByDate(dateIso),
    ) { workout, weights ->
        DayDetailData(workout = workout, weights = weights)
    }

    // Calendar navigation
    fun previousMonth() {
        displayedMonthFlow.value = displayedMonthFlow.value.plusMonths(-1)
    }

    fun nextMonth() {
        displayedMonthFlow.value = displayedMonthFlow.value.plusMonths(1)
    }

    fun jumpToCurrentMonth() {
        displayedMonthFlow.value = DateUtil.currentYearMonth()
    }

    // Dashboard controls
    fun setPeriodType(type: PeriodType) {
        if (periodTypeFlow.value != type) {
            periodTypeFlow.value = type
            periodOffsetFlow.value = 0
        }
    }

    fun previousPeriod() {
        periodOffsetFlow.value = periodOffsetFlow.value - 1
    }

    fun nextPeriod() {
        if (periodOffsetFlow.value < 0) {
            periodOffsetFlow.value = periodOffsetFlow.value + 1
        }
    }

    fun jumpToCurrentPeriod() {
        periodOffsetFlow.value = 0
    }

    fun selectTrendMetric(metric: TrainingTrendMetric) {
        selectedTrendMetricFlow.value = metric
    }

    // Data mutations
    fun saveDailyWorkout(
        dateIso: String,
        upperSets: Int,
        lowerSets: Int,
        crunchSets: Int,
        situpSets: Int,
        runKm: Double,
    ) {
        viewModelScope.launch {
            repository.saveDailyWorkout(
                dateIso = dateIso,
                upperSets = upperSets,
                lowerSets = lowerSets,
                crunchSets = crunchSets,
                situpSets = situpSets,
                runKm = runKm,
            )
        }
    }

    fun clearDailyWorkout(dateIso: String) {
        viewModelScope.launch {
            repository.clearDailyWorkout(dateIso)
        }
    }

    fun addWeightNow(weightKg: Double, note: String) {
        viewModelScope.launch {
            val today = DateUtil.today()
            repository.addWeightRecord(
                dateIso = today.toIsoString(),
                weightKg = weightKg,
                recordedAtMillis = System.currentTimeMillis(),
                note = note,
            )
        }
    }

    fun addWeightForDate(
        date: DateKey,
        weightKg: Double,
        hour: Int,
        minute: Int,
        note: String,
    ) {
        viewModelScope.launch {
            val timestamp = DateUtil.combineDateAndTime(date, hour, minute)
            repository.addWeightRecord(
                dateIso = date.toIsoString(),
                weightKg = weightKg,
                recordedAtMillis = timestamp,
                note = note,
            )
        }
    }

    fun deleteWeightRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteWeightRecord(id)
        }
    }

    suspend fun buildExportPayload(
        scope: ExportScope,
        format: ExportFormat,
    ): ExportPayload {
        val dash = dashboardUiState.value
        val range: DateRange? = when (scope) {
            ExportScope.CURRENT_PERIOD -> dash.analytics.range
            ExportScope.ALL_HISTORY -> null
        }

        val workouts = repository.getWorkoutsForExport(range)
        val weights = repository.getWeightsForExport(range)
        val recordCount = workouts.count { !it.isEmpty } + weights.size

        val scopeFileTag = when (scope) {
            ExportScope.CURRENT_PERIOD -> dash.periodFileTag
            ExportScope.ALL_HISTORY -> "全部历史"
        }

        val periodDesc = when (scope) {
            ExportScope.CURRENT_PERIOD -> "${dash.analytics.range.start.toIsoString()} 至 ${dash.analytics.range.end.toIsoString()}"
            ExportScope.ALL_HISTORY -> {
                val allDates = (workouts.map { it.date } + weights.map { it.date }).sorted()
                if (allDates.isEmpty()) "全部历史记录" else "${allDates.first()} 至 ${allDates.last()}（全部历史）"
            }
        }

        val content = when (format) {
            ExportFormat.CSV -> ExportFormatter.generateCsv(workouts, weights)
            ExportFormat.TXT -> ExportFormatter.generateTxtReport(
                title = "每日训记 · 健身与体重战报",
                periodDescription = periodDesc,
                workouts = workouts,
                weights = weights,
            )
        }

        val fileName = ExportFormatter.buildFileName(scopeFileTag, format)
        return ExportPayload(
            fileName = fileName,
            mimeType = format.mimeType,
            content = content,
            recordCount = recordCount,
        )
    }

    private fun buildPeriodLabels(
        type: PeriodType,
        offset: Int,
        range: DateRange,
    ): Triple<String, String, String> {
        return when (type) {
            PeriodType.WEEK -> {
                val main = "${range.start.toChineseMonthDay()} - ${range.end.toChineseMonthDay()}"
                val sub = when (offset) {
                    0 -> "本周 · ${range.start.year}年"
                    -1 -> "上周 · ${range.start.year}年"
                    else -> "${range.start.year}年"
                }
                val fileTag = "周报_${range.start.toIsoString()}_${range.end.toIsoString()}"
                Triple(main, sub, fileTag)
            }
            PeriodType.MONTH -> {
                val ym = range.start.toYearMonth()
                val main = ym.toChineseLabel()
                val sub = when (offset) {
                    0 -> "本月统计"
                    -1 -> "上月统计"
                    else -> "历史月统计"
                }
                val fileTag = "月报_${ym.year}年${ym.month}月"
                Triple(main, sub, fileTag)
            }
        }
    }

    class Factory(private val repository: FitnessRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FitnessViewModel(repository) as T
        }
    }
}
