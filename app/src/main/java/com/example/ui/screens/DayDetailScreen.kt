package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.export.ExportFormatter
import com.example.data.local.DailyWorkoutEntity
import com.example.data.local.WeightRecordEntity
import com.example.domain.DateKey
import com.example.domain.DateUtil
import com.example.ui.components.sanitizeDecimalInput
import com.example.ui.theme.LocalFitnessColors
import com.example.ui.theme.NumericStatStyle
import com.example.ui.viewmodel.DayDetailData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailScreen(
    date: DateKey,
    dayData: DayDetailData,
    onNavigateBack: () -> Unit,
    onSaveWorkout: (upper: Int, lower: Int, crunch: Int, situp: Int, runKm: Double) -> Unit,
    onClearWorkout: () -> Unit,
    onAddWeightClick: () -> Unit,
    onDeleteWeight: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { DateUtil.today() }
    val isToday = date == today
    val fitnessColors = LocalFitnessColors.current

    val workout = dayData.workout
    val hasWorkout = workout != null && !workout.isEmpty

    // For past dates: default read-only if record exists, or empty backfill prompt if no record
    var isEditingPast by remember(date) { mutableStateOf(false) }

    // Editable draft fields
    var upperSets by remember(date) { mutableIntStateOf(workout?.upperSets ?: 0) }
    var lowerSets by remember(date) { mutableIntStateOf(workout?.lowerSets ?: 0) }
    var crunchSets by remember(date) { mutableIntStateOf(workout?.crunchSets ?: 0) }
    var situpSets by remember(date) { mutableIntStateOf(workout?.situpSets ?: 0) }
    var runKmText by remember(date) {
        mutableStateOf(
            if (workout != null && workout.runKm > 0.0) ExportFormatter.formatKm(workout.runKm) else "",
        )
    }
    var initializedFromDb by remember(date) { mutableStateOf(false) }

    // Sync local draft when database loads initially or when not actively editing past
    LaunchedEffect(workout, isEditingPast) {
        if (!initializedFromDb || (!isToday && !isEditingPast)) {
            upperSets = workout?.upperSets ?: 0
            lowerSets = workout?.lowerSets ?: 0
            crunchSets = workout?.crunchSets ?: 0
            situpSets = workout?.situpSets ?: 0
            runKmText = if (workout != null && workout.runKm > 0.0) ExportFormatter.formatKm(workout.runKm) else ""
            if (workout != null) initializedFromDb = true
        }
    }

    fun persistDraftNow(
        u: Int = upperSets,
        l: Int = lowerSets,
        c: Int = crunchSets,
        s: Int = situpSets,
        runStr: String = runKmText,
    ) {
        val km = runStr.toDoubleOrNull() ?: 0.0
        onSaveWorkout(u, l, c, s, km)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("detail_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "${date.toChineseFull()} · ${DateUtil.weekdayChinese(date)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val statusLabel = when {
                            isToday -> stringResource(R.string.detail_auto_saved)
                            isEditingPast -> stringResource(R.string.detail_editing_badge)
                            hasWorkout -> stringResource(R.string.detail_readonly_badge)
                            else -> stringResource(R.string.detail_past_title)
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                isToday -> fitnessColors.strength
                                isEditingPast -> fitnessColors.run
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
                actions = {
                    if (!isToday && hasWorkout && !isEditingPast) {
                        FilledTonalButton(
                            onClick = {
                                upperSets = workout?.upperSets ?: 0
                                lowerSets = workout?.lowerSets ?: 0
                                crunchSets = workout?.crunchSets ?: 0
                                situpSets = workout?.situpSets ?: 0
                                runKmText = if (workout != null && workout.runKm > 0.0) {
                                    ExportFormatter.formatKm(workout.runKm)
                                } else {
                                    ""
                                }
                                isEditingPast = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .testTag("unlock_edit_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_unlock_edit),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    } else if (!isToday && isEditingPast) {
                        TextButton(
                            onClick = { isEditingPast = false },
                            modifier = Modifier.testTag("cancel_edit_button"),
                        ) {
                            Text(stringResource(R.string.action_cancel_edit))
                        }
                        Button(
                            onClick = {
                                persistDraftNow()
                                isEditingPast = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .testTag("save_past_workout_button"),
                        ) {
                            Text(stringResource(R.string.action_save_changes))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .navigationBarsPadding(),
            ) {
                // Workout section — 3 clear states per spec
                item(key = "workout_section") {
                    when {
                        // State A: Today (instant auto-save editable) OR Past unlocked for editing/backfill
                        isToday || isEditingPast -> {
                            EditableWorkoutSection(
                                isToday = isToday,
                                upperSets = upperSets,
                                lowerSets = lowerSets,
                                crunchSets = crunchSets,
                                situpSets = situpSets,
                                runKmText = runKmText,
                                onUpperChange = { next ->
                                    upperSets = next
                                    if (isToday) persistDraftNow(u = next)
                                },
                                onLowerChange = { next ->
                                    lowerSets = next
                                    if (isToday) persistDraftNow(l = next)
                                },
                                onCrunchChange = { next ->
                                    crunchSets = next
                                    if (isToday) persistDraftNow(c = next)
                                },
                                onSitupChange = { next ->
                                    situpSets = next
                                    if (isToday) persistDraftNow(s = next)
                                },
                                onRunTextChange = { next ->
                                    runKmText = next
                                    if (isToday) persistDraftNow(runStr = next)
                                },
                                onSavePastClick = {
                                    persistDraftNow()
                                    isEditingPast = false
                                },
                                onClearClick = {
                                    upperSets = 0
                                    lowerSets = 0
                                    crunchSets = 0
                                    situpSets = 0
                                    runKmText = ""
                                    onClearWorkout()
                                    if (!isToday) isEditingPast = false
                                },
                            )
                        }

                        // State B: Past Date with NO workout record -> Clear backfill prompt
                        !hasWorkout -> {
                            EmptyPastWorkoutCard(
                                onStartBackfill = {
                                    upperSets = 0
                                    lowerSets = 0
                                    crunchSets = 0
                                    situpSets = 0
                                    runKmText = ""
                                    isEditingPast = true
                                },
                            )
                        }

                        // State C: Past Date with EXISTING workout -> Read-only safe display
                        else -> {
                            ReadOnlyPastWorkoutCard(
                                workout = workout!!,
                                onUnlockEdit = { isEditingPast = true },
                            )
                        }
                    }
                }

                // Daily Weight Management Section (for both Today and Past Dates)
                item(key = "weight_section") {
                    DailyWeightSection(
                        isToday = isToday,
                        weights = dayData.weights,
                        onAddWeightClick = onAddWeightClick,
                        onDeleteWeight = onDeleteWeight,
                    )
                }

                item(key = "detail_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun EditableWorkoutSection(
    isToday: Boolean,
    upperSets: Int,
    lowerSets: Int,
    crunchSets: Int,
    situpSets: Int,
    runKmText: String,
    onUpperChange: (Int) -> Unit,
    onLowerChange: (Int) -> Unit,
    onCrunchChange: (Int) -> Unit,
    onSitupChange: (Int) -> Unit,
    onRunTextChange: (String) -> Unit,
    onSavePastClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    val fitnessColors = LocalFitnessColors.current
    val totalSets = upperSets + lowerSets + crunchSets + situpSets
    val totalReps = totalSets * DailyWorkoutEntity.REPS_PER_SET
    val currentRunKm = runKmText.toDoubleOrNull() ?: 0.0
    val hasAnyValue = totalSets > 0 || currentRunKm > 0.0

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Strength Training Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(fitnessColors.strengthSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = fitnessColors.strength,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.section_strength_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.conversion_rule_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = fitnessColors.strengthSoft,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = "$totalSets 组",
                                style = NumericStatStyle.copy(fontSize = 15.sp),
                                color = fitnessColors.strength,
                            )
                            Text(
                                text = "共 $totalReps 个",
                                style = MaterialTheme.typography.labelSmall,
                                color = fitnessColors.strength.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                StrengthStepperRow(
                    name = stringResource(R.string.exercise_upper),
                    sets = upperSets,
                    accent = fitnessColors.upperAccent,
                    onSetsChange = onUpperChange,
                    testTagPrefix = "upper",
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                StrengthStepperRow(
                    name = stringResource(R.string.exercise_lower),
                    sets = lowerSets,
                    accent = fitnessColors.lowerAccent,
                    onSetsChange = onLowerChange,
                    testTagPrefix = "lower",
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                StrengthStepperRow(
                    name = stringResource(R.string.exercise_crunch),
                    sets = crunchSets,
                    accent = fitnessColors.crunchAccent,
                    onSetsChange = onCrunchChange,
                    testTagPrefix = "crunch",
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                StrengthStepperRow(
                    name = stringResource(R.string.exercise_situp),
                    sets = situpSets,
                    accent = fitnessColors.situpAccent,
                    onSetsChange = onSitupChange,
                    testTagPrefix = "situp",
                )
            }
        }

        // Cardio (Long Run) Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(fitnessColors.runSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = fitnessColors.run,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${stringResource(R.string.section_cardio_title)} · ${stringResource(R.string.exercise_run)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.run_input_helper),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = runKmText,
                    onValueChange = { raw ->
                        val sanitized = sanitizeDecimalInput(raw, maxDecimals = 2)
                        onRunTextChange(sanitized)
                    },
                    label = { Text(stringResource(R.string.run_input_label)) },
                    placeholder = { Text(stringResource(R.string.run_input_placeholder)) },
                    suffix = {
                        Text(
                            text = stringResource(R.string.unit_km),
                            style = MaterialTheme.typography.titleMedium,
                            color = fitnessColors.run,
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = NumericStatStyle.copy(fontSize = 20.sp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_km_input"),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(1.0, 3.0, 5.0).forEach { addKm ->
                        Surface(
                            onClick = {
                                val next = (currentRunKm + addKm).coerceAtMost(999.99)
                                onRunTextChange(ExportFormatter.formatKm(next))
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = fitnessColors.runSoft,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = "+${addKm.toInt()} km",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = fitnessColors.run,
                                )
                            }
                        }
                    }
                    Surface(
                        onClick = { onRunTextChange("") },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.weight(0.85f),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 10.dp),
                        ) {
                            Text(
                                text = "清零",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Action strip
        if (!isToday) {
            Button(
                onClick = onSavePastClick,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_save_backfill_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "保存并归档至该日期",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        } else if (hasAnyValue) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = fitnessColors.strength,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "每次修改已自动实时保存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onClearClick,
                    modifier = Modifier.testTag("clear_today_workout_button"),
                ) {
                    Text(
                        text = stringResource(R.string.action_clear_workout),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun StrengthStepperRow(
    name: String,
    sets: Int,
    accent: Color,
    onSetsChange: (Int) -> Unit,
    testTagPrefix: String,
) {
    val reps = sets * DailyWorkoutEntity.REPS_PER_SET
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "= $reps 个",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (sets > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilledTonalIconButton(
                onClick = { if (sets > 0) onSetsChange(sets - 1) },
                enabled = sets > 0,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${testTagPrefix}_dec_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "${stringResource(R.string.cd_decrease_sets)} $name",
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp),
            ) {
                Text(
                    text = sets.toString(),
                    style = NumericStatStyle.copy(fontSize = 20.sp),
                    color = if (sets > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.unit_set),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledTonalIconButton(
                onClick = { onSetsChange((sets + 1).coerceAtMost(999)) },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = accent.copy(alpha = 0.16f),
                    contentColor = accent,
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${testTagPrefix}_inc_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "${stringResource(R.string.cd_increase_sets)} $name",
                    modifier = Modifier.size(20.dp),
                )
            }

            Surface(
                onClick = { onSetsChange((sets + 5).coerceAtMost(999)) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("${testTagPrefix}_inc5_button"),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 10.dp),
                ) {
                    Text(
                        text = "+5",
                        style = NumericStatStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPastWorkoutCard(
    onStartBackfill: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_workout),
                contentDescription = stringResource(R.string.empty_past_workout_title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(148.dp)
                    .clip(RoundedCornerShape(18.dp)),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_past_workout_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.empty_past_workout_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onStartBackfill,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("backfill_workout_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.action_backfill_workout),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyPastWorkoutCard(
    workout: DailyWorkoutEntity,
    onUnlockEdit: () -> Unit,
) {
    val fitnessColors = LocalFitnessColors.current

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "当日训练明细（只读）",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = fitnessColors.strengthSoft,
                ) {
                    Text(
                        text = "力量 ${workout.totalStrengthSets}组 / ${workout.totalStrengthReps}个",
                        style = MaterialTheme.typography.labelMedium,
                        color = fitnessColors.strength,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2x2 grid for 4 strength items
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ReadOnlyMetricTile(
                    label = stringResource(R.string.exercise_upper),
                    sets = workout.upperSets,
                    reps = workout.upperReps,
                    accent = fitnessColors.upperAccent,
                    modifier = Modifier.weight(1f),
                )
                ReadOnlyMetricTile(
                    label = stringResource(R.string.exercise_lower),
                    sets = workout.lowerSets,
                    reps = workout.lowerReps,
                    accent = fitnessColors.lowerAccent,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ReadOnlyMetricTile(
                    label = stringResource(R.string.exercise_crunch),
                    sets = workout.crunchSets,
                    reps = workout.crunchReps,
                    accent = fitnessColors.crunchAccent,
                    modifier = Modifier.weight(1f),
                )
                ReadOnlyMetricTile(
                    label = stringResource(R.string.exercise_situp),
                    sets = workout.situpSets,
                    reps = workout.situpReps,
                    accent = fitnessColors.situpAccent,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Long run row
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = fitnessColors.runSoft,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = fitnessColors.run,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "有氧长跑",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = if (workout.hasRun) "${ExportFormatter.formatKm(workout.runKm)} km" else "0 km",
                        style = NumericStatStyle.copy(fontSize = 19.sp),
                        color = if (workout.hasRun) fitnessColors.run else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onUnlockEdit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("card_unlock_edit_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("解锁补充或修正当日数据")
            }
        }
    }
}

@Composable
private fun ReadOnlyMetricTile(
    label: String,
    sets: Int,
    reps: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (sets > 0) accent else MaterialTheme.colorScheme.outline),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$sets 组",
                style = NumericStatStyle.copy(fontSize = 20.sp),
                color = if (sets > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "换算 $reps 个",
                style = MaterialTheme.typography.bodySmall,
                color = if (sets > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DailyWeightSection(
    isToday: Boolean,
    weights: List<WeightRecordEntity>,
    onAddWeightClick: () -> Unit,
    onDeleteWeight: (Long) -> Unit,
) {
    val fitnessColors = LocalFitnessColors.current

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(fitnessColors.weightSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MonitorWeight,
                            contentDescription = null,
                            tint = fitnessColors.weight,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.section_weight_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                FilledTonalButton(
                    onClick = onAddWeightClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = fitnessColors.weightSoft,
                        contentColor = fitnessColors.weight,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("day_add_weight_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isToday) {
                            stringResource(R.string.action_add_weight_today)
                        } else {
                            stringResource(R.string.action_backfill_weight)
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (weights.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.empty_day_weight_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    weights.forEach { record ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = fitnessColors.weightSoft,
                                    ) {
                                        Text(
                                            text = DateUtil.formatTime(record.recordedAtMillis),
                                            style = NumericStatStyle.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                            ),
                                            color = fitnessColors.weight,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${ExportFormatter.formatKg(record.weightKg)} kg",
                                            style = NumericStatStyle.copy(fontSize = 18.sp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (record.note.isNotBlank()) {
                                            Text(
                                                text = record.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteWeight(record.id) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("delete_weight_${record.id}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = stringResource(R.string.cd_delete_weight),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
