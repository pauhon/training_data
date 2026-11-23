package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.export.ExportFormatter
import com.example.data.local.DailyWorkoutEntity
import com.example.data.local.WeightRecordEntity
import com.example.domain.DateKey
import com.example.domain.DateUtil
import com.example.ui.theme.LocalFitnessColors
import com.example.ui.theme.NumericStatStyle
import com.example.ui.viewmodel.CalendarUiState
import com.example.ui.viewmodel.DayMarker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHomeScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onDateClick: (DateKey) -> Unit,
    onQuickLogWeightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fitnessColors = LocalFitnessColors.current

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${uiState.today.toChineseFull()} · ${DateUtil.weekdayChinese(uiState.today)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                FilledTonalButton(
                    onClick = onQuickLogWeightClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = fitnessColors.weightSoft,
                        contentColor = fitnessColors.weight,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("quick_log_weight_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonitorWeight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_log_weight),
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
                    .widthIn(max = 640.dp),
            ) {
                // 1. Month Calendar Card
                item(key = "calendar_card") {
                    MonthCalendarCard(
                        uiState = uiState,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onJumpToCurrentMonth = onJumpToCurrentMonth,
                        onDateClick = onDateClick,
                    )
                }

                // 2. Persistent Today Quick Entry Card
                item(key = "today_quick_card") {
                    TodayQuickEntryCard(
                        today = uiState.today,
                        todayWorkout = uiState.todayWorkout,
                        todayWeights = uiState.todayWeights,
                        latestWeight = uiState.latestWeight,
                        onClick = { onDateClick(uiState.today) },
                    )
                }

                // 3. Compact Displayed-Month Summary Strip
                item(key = "month_strip") {
                    MonthQuickSummaryStrip(
                        monthLabel = uiState.displayedMonth.toChineseLabel(),
                        activeDays = uiState.monthActiveDays,
                        strengthSets = uiState.monthStrengthSets,
                        runKm = uiState.monthRunKm,
                    )
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarCard(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrentMonth: () -> Unit,
    onDateClick: (DateKey) -> Unit,
) {
    val fitnessColors = LocalFitnessColors.current
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)) {
            // Month selector bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.testTag("calendar_prev_month"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.cd_prev_month),
                        )
                    }
                    Text(
                        text = uiState.displayedMonth.toChineseLabel(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.testTag("calendar_next_month"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.cd_next_month),
                        )
                    }
                }

                if (!uiState.isCurrentMonth) {
                    Surface(
                        onClick = onJumpToCurrentMonth,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("back_to_current_month_button"),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_back_to_current_month),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Weekday header
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEachIndexed { idx, dayName ->
                    val isWeekend = idx >= 5
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isWeekend) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar week rows
            val rows = uiState.cells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                rows.forEach { week ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        week.forEach { cellDate ->
                            if (cellDate == null) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                )
                            } else {
                                val isToday = cellDate == uiState.today
                                val isFuture = cellDate > uiState.today
                                val marker = uiState.dayMarkers[cellDate.toIsoString()] ?: DayMarker()
                                CalendarDayCell(
                                    date = cellDate,
                                    isToday = isToday,
                                    isFuture = isFuture,
                                    marker = marker,
                                    onClick = { onDateClick(cellDate) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
            ) {
                LegendDotItem(color = fitnessColors.strength, label = stringResource(R.string.legend_strength))
                Spacer(modifier = Modifier.width(18.dp))
                LegendDotItem(color = fitnessColors.run, label = stringResource(R.string.legend_run))
                Spacer(modifier = Modifier.width(18.dp))
                LegendDotItem(color = fitnessColors.weight, label = stringResource(R.string.legend_weight))
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: DateKey,
    isToday: Boolean,
    isFuture: Boolean,
    marker: DayMarker,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fitnessColors = LocalFitnessColors.current
    val hasAnyRecord = marker.hasStrength || marker.hasRun || marker.hasWeight

    val containerColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        hasAnyRecord && !isFuture -> MaterialTheme.colorScheme.surfaceContainerLow
        else -> Color.Transparent
    }

    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .then(
                if (isToday) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(14.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(enabled = !isFuture, onClick = onClick)
            .testTag("calendar_day_${date.toIsoString()}"),
    ) {
        if (isToday) {
            Text(
                text = stringResource(R.string.today_badge),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 5.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.day.toString(),
                style = NumericStatStyle.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isToday || hasAnyRecord) FontWeight.Bold else FontWeight.Medium,
                ),
                color = textColor,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(6.dp),
            ) {
                if (marker.hasStrength) {
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .clip(CircleShape)
                            .background(if (isToday) Color.White else fitnessColors.strength),
                    )
                }
                if (marker.hasRun) {
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .clip(CircleShape)
                            .background(if (isToday) Color(0xFFFFD3C2) else fitnessColors.run),
                    )
                }
                if (marker.hasWeight) {
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .clip(CircleShape)
                            .background(if (isToday) Color(0xFFC7DAFF) else fitnessColors.weight),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDotItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodayQuickEntryCard(
    today: DateKey,
    todayWorkout: DailyWorkoutEntity?,
    todayWeights: List<WeightRecordEntity>,
    latestWeight: WeightRecordEntity?,
    onClick: () -> Unit,
) {
    val fitnessColors = LocalFitnessColors.current
    val hasWorkout = todayWorkout != null && !todayWorkout.isEmpty

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (hasWorkout) {
                fitnessColors.strength.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_quick_entry_card"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            fitnessColors.strengthSoft.copy(alpha = 0.45f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(fitnessColors.strength),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.today_card_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${today.toChineseMonthDay()} ${DateUtil.weekdayChinese(today)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Latest weight badge if available
                    val displayWeight = todayWeights.lastOrNull() ?: latestWeight
                    if (displayWeight != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = fitnessColors.weightSoft,
                        ) {
                            Text(
                                text = "${ExportFormatter.formatKg(displayWeight.weightKg)} kg",
                                style = NumericStatStyle.copy(fontSize = 13.sp),
                                color = fitnessColors.weight,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasWorkout) {
                    Text(
                        text = stringResource(R.string.today_card_unlogged_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.today_card_unlogged_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("today_start_workout_button"),
                    ) {
                        Text(
                            text = stringResource(R.string.today_card_cta_start),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    val w = todayWorkout!!
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Strength summary pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = fitnessColors.strengthSoft,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "力量训练",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = fitnessColors.strength,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${w.totalStrengthSets} 组",
                                    style = NumericStatStyle.copy(fontSize = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "共 ${w.totalStrengthReps} 个",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // Cardio summary pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = fitnessColors.runSoft,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.DirectionsRun,
                                        contentDescription = null,
                                        tint = fitnessColors.run,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "长跑有氧",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fitnessColors.run,
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${ExportFormatter.formatKm(w.runKm)} km",
                                    style = NumericStatStyle.copy(fontSize = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (w.hasRun) "已完成打卡" else "未记录里程",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Individual non-zero strength chips
                    val breakdown = buildList {
                        if (w.upperSets > 0) add("上肢 ${w.upperSets}组(${w.upperReps}个)")
                        if (w.lowerSets > 0) add("下肢 ${w.lowerSets}组(${w.lowerReps}个)")
                        if (w.crunchSets > 0) add("弯起 ${w.crunchSets}组(${w.crunchReps}个)")
                        if (w.situpSets > 0) add("躺起 ${w.situpSets}组(${w.situpReps}个)")
                    }
                    if (breakdown.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            breakdown.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                ) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.today_card_cta_edit),
                            style = MaterialTheme.typography.labelLarge,
                            color = fitnessColors.strength,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = fitnessColors.strength,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthQuickSummaryStrip(
    monthLabel: String,
    activeDays: Int,
    strengthSets: Int,
    runKm: Double,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Column {
                Text(
                    text = "${monthLabel}统计",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "有效打卡 $activeDays 天",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$strengthSets 组",
                        style = NumericStatStyle.copy(fontSize = 15.sp),
                        color = LocalFitnessColors.current.strength,
                    )
                    Text(
                        text = "${strengthSets * 10} 个",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${ExportFormatter.formatKm(runKm)} km",
                        style = NumericStatStyle.copy(fontSize = 15.sp),
                        color = LocalFitnessColors.current.run,
                    )
                    Text(
                        text = "长跑",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
