package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.DateKey
import com.example.domain.DateUtil
import com.example.ui.theme.LocalFitnessColors
import com.example.ui.theme.NumericStatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightEntrySheet(
    targetDate: DateKey?, // null means "now (today)"
    suggestedWeightKg: Double?,
    onDismiss: () -> Unit,
    onSave: (weightKg: Double, hour: Int, minute: Int, note: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fitnessColors = LocalFitnessColors.current
    val nowHourMinute = remember { DateUtil.currentHourMinute() }
    val isBackfillPast = targetDate != null && targetDate < DateUtil.today()

    var weightText by remember {
        mutableStateOf(
            suggestedWeightKg?.let { String.format(Locale.US, "%.1f", it) } ?: "",
        )
    }
    var noteText by remember { mutableStateOf("") }
    var selectedHour by remember { mutableIntStateOf(if (isBackfillPast) 8 else nowHourMinute.first) }
    var selectedMinute by remember { mutableIntStateOf(if (isBackfillPast) 0 else nowHourMinute.second) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val invalidError = stringResource(R.string.error_invalid_weight)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(fitnessColors.weightSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonitorWeight,
                        contentDescription = null,
                        tint = fitnessColors.weight,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isBackfillPast) {
                            stringResource(R.string.weight_sheet_title_date)
                        } else {
                            stringResource(R.string.weight_sheet_title_now)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val dateSubtitle = if (targetDate != null) {
                        "${targetDate.toChineseFull()} · ${DateUtil.weekdayChinese(targetDate)}"
                    } else {
                        val t = DateUtil.today()
                        "${t.toChineseFull()} · 当前时间"
                    }
                    Text(
                        text = dateSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = weightText,
                onValueChange = { raw ->
                    val filtered = sanitizeDecimalInput(raw, maxDecimals = 1)
                    weightText = filtered
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.weight_input_label)) },
                placeholder = { Text(stringResource(R.string.weight_input_placeholder)) },
                suffix = {
                    Text(
                        text = stringResource(R.string.unit_kg),
                        style = MaterialTheme.typography.titleMedium,
                        color = fitnessColors.weight,
                    )
                },
                supportingText = {
                    Text(
                        text = errorMessage ?: stringResource(R.string.weight_input_helper),
                        color = if (errorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                isError = errorMessage != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = NumericStatStyle.copy(fontSize = 22.sp),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weight_input_field"),
            )

            // Quick +/- 0.1kg fine tuning chips if a value is present
            val parsedPreview = weightText.toDoubleOrNull()
            if (parsedPreview != null && parsedPreview in 20.0..300.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(-0.5, -0.1, +0.1, +0.5).forEach { delta ->
                        Surface(
                            onClick = {
                                val next = (parsedPreview + delta).coerceIn(20.0, 300.0)
                                weightText = String.format(Locale.US, "%.1f", next)
                                errorMessage = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp),
                            ) {
                                Text(
                                    text = if (delta > 0) "+$delta" else "$delta",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time selector row (editable for past dates or custom time)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.weight_time_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(
                            onClick = {
                                val totalMin = (selectedHour * 60 + selectedMinute - 30 + 1440) % 1440
                                selectedHour = totalMin / 60
                                selectedMinute = totalMin % 60
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "提前30分钟",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute),
                            style = NumericStatStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                        FilledTonalIconButton(
                            onClick = {
                                val totalMin = (selectedHour * 60 + selectedMinute + 30) % 1440
                                selectedHour = totalMin / 60
                                selectedMinute = totalMin % 60
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "延后30分钟",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 30) noteText = it },
                label = { Text(stringResource(R.string.weight_note_label)) },
                placeholder = { Text(stringResource(R.string.weight_note_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weight_note_field"),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val value = weightText.toDoubleOrNull()
                    if (value == null || value < 20.0 || value > 300.0) {
                        errorMessage = invalidError
                    } else {
                        onSave(value, selectedHour, selectedMinute, noteText)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = fitnessColors.weight,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_weight_button"),
            ) {
                Text(
                    text = stringResource(R.string.action_save_weight),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

fun sanitizeDecimalInput(raw: String, maxDecimals: Int): String {
    val sb = StringBuilder()
    var hasDot = false
    var decimals = 0
    for (ch in raw) {
        when {
            ch.isDigit() -> {
                if (hasDot) {
                    if (decimals < maxDecimals) {
                        sb.append(ch)
                        decimals++
                    }
                } else {
                    if (sb.length < 4) sb.append(ch)
                }
            }
            ch == '.' && !hasDot -> {
                if (sb.isEmpty()) sb.append('0')
                sb.append('.')
                hasDot = true
            }
        }
    }
    return sb.toString()
}
