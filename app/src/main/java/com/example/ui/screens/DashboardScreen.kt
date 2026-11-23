package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.export.ExportFormatter
import com.example.domain.PeriodType
import com.example.domain.TrainingTrendMetric
import com.example.ui.components.StrengthCategoryBarChart
import com.example.ui.components.TrainingTrendLineChart
import com.example.ui.components.WeightTrendLineChart
import com.example.ui.theme.LocalFitnessColors
import com.example.ui.theme.NumericStatStyle
import com.example.ui.viewmodel.DashboardUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onPeriodTypeChange: (PeriodType) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onJumpToCurrentPeriod: () -> Unit,
    onSelectTrendMetric: (TrainingTrendMetric) -> Unit,
    onOpenExportSheet: () -> Unit,
    onQuickLogWeightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fitnessColors = LocalFitnessColors.current
    val analytics = uiState.analytics
    val allDatesInPeriod = remember(analytics.range) { analytics.range.allDates() }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.dashboard_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            actions = {
                FilledTonalButton(
                    onClick = onOpenExportSheet,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("open_export_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_export),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
            ) {
                // 1. Period Switcher + Pager
                item(key = "period_controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = uiState.periodType == PeriodType.WEEK,
                                onClick = { onPeriodTypeChange(PeriodType.WEEK) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                modifier = Modifier.testTag("period_week_tab"),
                            ) {
                                Text(stringResource(R.string.period_week))
                            }
                            SegmentedButton(
                                selected = uiState.periodType == PeriodType.MONTH,
                                onClick = { onPeriodTypeChange(PeriodType.MONTH) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                modifier = Modifier.testTag("period_month_tab"),
                            ) {
                                Text(stringResource(R.string.period_month))
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                            ) {
                                IconButton(
                                    onClick = onPreviousPeriod,
                                    modifier = Modifier.testTag("dashboard_prev_period"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.cd_prev_period),
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = uiState.periodLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = uiState.periodSubLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (uiState.canGoNext) {
                                            Surface(
                                                onClick = onJumpToCurrentPeriod,
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.testTag("dashboard_back_current"),
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.action_back_to_current_period),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = onNextPeriod,
                                    enabled = uiState.canGoNext,
                                    modifier = Modifier.testTag("dashboard_next_period"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.cd_next_period),
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Core KPI Grid (2 x 2)
                item(key = "kpi_grid") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            KpiCard(
                                icon = Icons.Rounded.CalendarMonth,
                                title = stringResource(R.string.kpi_active_days),
                                value = "${analytics.activeWorkoutDays} 天",
                                subtitle = "周期共 ${analytics.totalDaysInPeriod} 天",
                                accent = fitnessColors.strength,
                                softBg = fitnessColors.strengthSoft,
                                modifier = Modifier.weight(1f),
                            )
                            KpiCard(
                                icon = Icons.Rounded.FitnessCenter,
                                title = stringResource(R.string.kpi_total_strength),
                                value = "${analytics.totalStrengthSets} 组",
                                subtitle = "换算共 ${analytics.totalStrengthReps} 个",
                                accent = fitnessColors.upperAccent,
                                softBg = fitnessColors.strengthSoft,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            KpiCard(
                                icon = Icons.Rounded.DirectionsRun,
                                title = stringResource(R.string.kpi_total_run),
                                value = "${ExportFormatter.formatKm(analytics.totalRunKm)} km",
                                subtitle = "长跑 ${analytics.runDays} 次",
                                accent = fitnessColors.run,
                                softBg = fitnessColors.runSoft,
                                modifier = Modifier.weight(1f),
                            )

                            val weightChangeValue = when {
                                analytics.weightNetChangeKg != null -> {
                                    val diff = analytics.weightNetChangeKg
                                    val prefix = if (diff > 0) "+" else ""
                                    "$prefix${ExportFormatter.formatKg(diff)} kg"
                                }
                                analytics.latestWeightKg != null -> {
                                    "${ExportFormatter.formatKg(analytics.latestWeightKg)} kg"
                                }
                                else -> "暂无"
                            }
                            val weightChangeSub = when {
                                analytics.weightNetChangeKg != null && analytics.firstWeightKg != null && analytics.latestWeightKg != null -> {
                                    "${ExportFormatter.formatKg(analytics.firstWeightKg)} → ${ExportFormatter.formatKg(analytics.latestWeightKg)} kg"
                                }
                                analytics.weightRecordCount == 1 -> "周期内记录 1 次"
                                else -> "未称重"
                            }

                            KpiCard(
                                icon = Icons.Rounded.MonitorWeight,
                                title = stringResource(R.string.kpi_weight_change),
                                value = weightChangeValue,
                                subtitle = weightChangeSub,
                                accent = fitnessColors.weight,
                                softBg = fitnessColors.weightSoft,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // 3. Category Volume Comparison Bar Chart
                item(key = "category_bar_chart") {
                    DashboardSectionCard(
                        title = stringResource(R.string.chart_category_volume),
                        badge = "4 项力量 + 长跑",
                    ) {
                        StrengthCategoryBarChart(
                            categories = analytics.strengthCategories,
                            totalRunKm = analytics.totalRunKm,
                            runDays = analytics.runDays,
                        )
                    }
                }

                // 4. Multi-Dimensional Training Trend Line Chart
                item(key = "training_trend_chart") {
                    DashboardSectionCard(
                        title = stringResource(R.string.chart_training_trend),
                        badge = uiState.selectedMetric.label,
                    ) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                TrainingTrendMetric.entries.forEach { metric ->
                                    val selected = metric == uiState.selectedMetric
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onSelectTrendMetric(metric) },
                                        label = { Text(metric.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (metric.isCardio) {
                                                fitnessColors.runSoft
                                            } else {
                                                fitnessColors.strengthSoft
                                            },
                                            selectedLabelColor = if (metric.isCardio) {
                                                fitnessColors.run
                                            } else {
                                                fitnessColors.strength
                                            },
                                        ),
                                        modifier = Modifier.testTag("trend_chip_${metric.name.lowercase()}"),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val points = analytics.trainingTrends[uiState.selectedMetric].orEmpty()
                            if (points.all { it.value <= 0.0 }) {
                                EmptyChartBox(text = stringResource(R.string.chart_empty_training))
                            } else {
                                TrainingTrendLineChart(
                                    points = points,
                                    metric = uiState.selectedMetric,
                                    periodType = uiState.periodType,
                                )
                            }
                        }
                    }
                }

                // 5. Weight Trend Line Chart (skipping unweighed days smoothly)
                item(key = "weight_trend_chart") {
                    DashboardSectionCard(
                        title = stringResource(R.string.chart_weight_trend),
                        badge = stringResource(R.string.chart_weight_skip_hint),
                    ) {
                        if (analytics.weightTrendPoints.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.chart_empty_weight),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                FilledTonalButton(
                                    onClick = onQuickLogWeightClick,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = fitnessColors.weightSoft,
                                        contentColor = fitnessColors.weight,
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(stringResource(R.string.action_log_weight))
                                }
                            }
                        } else {
                            WeightTrendLineChart(
                                points = analytics.weightTrendPoints,
                                allDatesInPeriod = allDatesInPeriod,
                                periodType = uiState.periodType,
                            )
                        }
                    }
                }

                item(key = "dashboard_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    softBg: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = softBg,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = NumericStatStyle.copy(fontSize = 21.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DashboardSectionCard(
    title: String,
    badge: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun EmptyChartBox(text: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
