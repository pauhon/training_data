package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.ExportFormatter
import com.example.domain.CategoryVolumeItem
import com.example.domain.DateKey
import com.example.domain.PeriodType
import com.example.domain.TrainingTrendMetric
import com.example.domain.TrainingTrendPoint
import com.example.domain.WeightTrendPoint
import com.example.ui.theme.LocalFitnessColors
import com.example.ui.theme.NumericStatStyle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun StrengthCategoryBarChart(
    categories: List<CategoryVolumeItem>,
    totalRunKm: Double,
    runDays: Int,
    modifier: Modifier = Modifier,
) {
    val fitnessColors = LocalFitnessColors.current
    val maxSets = remember(categories) { (categories.maxOfOrNull { it.sets } ?: 0).coerceAtLeast(1) }
    val barColors = listOf(
        fitnessColors.upperAccent,
        fitnessColors.lowerAccent,
        fitnessColors.crunchAccent,
        fitnessColors.situpAccent,
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Vertical bar chart
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp)
                .padding(horizontal = 4.dp),
        ) {
            categories.forEachIndexed { index, item ->
                val targetRatio = if (item.sets == 0) 0.04f else (item.sets.toFloat() / maxSets.toFloat()).coerceIn(0.08f, 1f)
                val animatedRatio by animateFloatAsState(
                    targetValue = targetRatio,
                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                    label = "bar_${item.label}",
                )
                val accent = barColors.getOrElse(index) { fitnessColors.strength }
                val pct = (item.shareFraction * 100f).roundToInt()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = if (item.sets > 0) "${pct}%" else "0%",
                        style = NumericStatStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = if (item.sets > 0) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .height(92.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // Subtle track background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(92.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                        )
                        // Filled bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((92.dp * animatedRatio).coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (item.sets > 0) {
                                        Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.72f)))
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.outlineVariant,
                                                MaterialTheme.colorScheme.outlineVariant,
                                            ),
                                        )
                                    },
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${item.sets}组/${item.reps}个",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Long run cardio horizontal summary strip
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = fitnessColors.runSoft,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(fitnessColors.run),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "有氧长跑累计",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "共 $runDays 次",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "${ExportFormatter.formatKm(totalRunKm)} km",
                    style = NumericStatStyle.copy(fontSize = 17.sp),
                    color = fitnessColors.run,
                )
            }
        }
    }
}

@Composable
fun TrainingTrendLineChart(
    points: List<TrainingTrendPoint>,
    metric: TrainingTrendMetric,
    periodType: PeriodType,
    modifier: Modifier = Modifier,
) {
    val fitnessColors = LocalFitnessColors.current
    val lineColor = when (metric) {
        TrainingTrendMetric.RUN -> fitnessColors.run
        TrainingTrendMetric.UPPER -> fitnessColors.upperAccent
        TrainingTrendMetric.LOWER -> fitnessColors.lowerAccent
        TrainingTrendMetric.CRUNCH -> fitnessColors.crunchAccent
        TrainingTrendMetric.SITUP -> fitnessColors.situpAccent
        TrainingTrendMetric.STRENGTH_TOTAL -> fitnessColors.strength
    }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    var selectedIndex by remember(points, metric) {
        val lastNonZero = points.indexOfLast { it.value > 0.0 }
        mutableIntStateOf(if (lastNonZero >= 0) lastNonZero else -1)
    }

    val maxRaw = remember(points) { points.maxOfOrNull { it.value } ?: 0.0 }
    val yMax = remember(maxRaw, metric) {
        if (maxRaw <= 0.0) {
            if (metric.isCardio) 5.0 else 10.0
        } else {
            val padded = maxRaw * 1.18
            if (metric.isCardio) ceil(padded * 2.0) / 2.0 else ceil(padded)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Interactive tooltip banner
        val inspected = points.getOrNull(selectedIndex)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            if (inspected != null) {
                Text(
                    text = inspected.date.toChineseMonthDay(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val valueStr = if (metric.isCardio) {
                    "${ExportFormatter.formatKm(inspected.value)} km"
                } else {
                    "${inspected.value.roundToInt()} 组（${inspected.repsValue} 个）"
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = lineColor.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = valueStr,
                        style = NumericStatStyle.copy(fontSize = 13.sp),
                        color = lineColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            } else {
                Text(
                    text = "点击图表可查看每日明细",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Y-axis labels column
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .width(34.dp)
                    .height(150.dp)
                    .padding(end = 6.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Text(
                    text = formatAxisVal(yMax, metric.isCardio),
                    style = NumericStatStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatAxisVal(yMax / 2.0, metric.isCardio),
                    style = NumericStatStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "0",
                    style = NumericStatStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Chart Canvas
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.size > 1) {
                                val padX = 12.dp.toPx()
                                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                                val frac = ((offset.x - padX) / usableW).coerceIn(0f, 1f)
                                selectedIndex = (frac * (points.size - 1)).roundToInt()
                                    .coerceIn(0, points.lastIndex)
                            }
                        }
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            if (points.size > 1) {
                                val padX = 12.dp.toPx()
                                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                                val frac = ((change.position.x - padX) / usableW).coerceIn(0f, 1f)
                                selectedIndex = (frac * (points.size - 1)).roundToInt()
                                    .coerceIn(0, points.lastIndex)
                            }
                        }
                    },
            ) {
                val padX = 12.dp.toPx()
                val padTop = 10.dp.toPx()
                val padBottom = 10.dp.toPx()
                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)
                val bottomY = padTop + usableH

                // Dashed horizontal grid lines
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 0..2) {
                    val y = padTop + usableH * (i / 2f)
                    drawLine(
                        color = gridColor,
                        start = Offset(padX, y),
                        end = Offset(size.width - padX, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i == 2) null else dash,
                    )
                }

                if (points.isEmpty()) return@Canvas

                val stepX = if (points.size > 1) usableW / (points.size - 1) else 0f
                val coords = points.mapIndexed { idx, pt ->
                    val x = padX + idx * stepX
                    val ratio = (pt.value / yMax).coerceIn(0.0, 1.0).toFloat()
                    val y = bottomY - ratio * usableH
                    Offset(x, y)
                }

                // Vertical highlight line for selected point
                if (selectedIndex in coords.indices) {
                    val sel = coords[selectedIndex]
                    drawLine(
                        color = lineColor.copy(alpha = 0.35f),
                        start = Offset(sel.x, padTop),
                        end = Offset(sel.x, bottomY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dash,
                    )
                }

                // Area gradient fill
                if (coords.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(coords.first().x, bottomY)
                        lineTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            lineTo(coords[i].x, coords[i].y)
                        }
                        lineTo(coords.last().x, bottomY)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.24f), lineColor.copy(alpha = 0.01f)),
                            startY = padTop,
                            endY = bottomY,
                        ),
                    )

                    // Line path
                    val strokePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            lineTo(coords[i].x, coords[i].y)
                        }
                    }
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.6.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                // Dots (all points in week view; non-zero or selected in month view)
                coords.forEachIndexed { idx, pt ->
                    val isSel = idx == selectedIndex
                    val showDot = isSel || periodType == PeriodType.WEEK || points[idx].value > 0.0
                    if (showDot) {
                        val r = if (isSel) 5.5.dp.toPx() else 3.2.dp.toPx()
                        drawCircle(color = surfaceColor, radius = r + 1.5.dp.toPx(), center = pt)
                        drawCircle(color = lineColor, radius = r, center = pt)
                    }
                }
            }
        }

        // X-axis date ticks
        Spacer(modifier = Modifier.height(6.dp))
        XAxisLabelsRow(
            dates = points.map { it.date },
            periodType = periodType,
            modifier = Modifier.padding(start = 34.dp),
        )
    }
}

@Composable
fun WeightTrendLineChart(
    points: List<WeightTrendPoint>,
    allDatesInPeriod: List<DateKey>,
    periodType: PeriodType,
    modifier: Modifier = Modifier,
) {
    val fitnessColors = LocalFitnessColors.current
    val lineColor = fitnessColors.weight
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    var selectedPointIdx by remember(points) {
        mutableIntStateOf(if (points.isNotEmpty()) points.lastIndex else -1)
    }

    val (yMin, yMax) = remember(points) {
        if (points.isEmpty()) {
            50.0 to 70.0
        } else {
            val minW = points.minOf { it.weightKg }
            val maxW = points.maxOf { it.weightKg }
            if (abs(maxW - minW) < 0.2) {
                (floor(minW - 1.5)) to (ceil(maxW + 1.5))
            } else {
                val pad = ((maxW - minW) * 0.25).coerceAtLeast(0.6)
                (floor((minW - pad) * 2.0) / 2.0) to (ceil((maxW + pad) * 2.0) / 2.0)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip bar
        val inspected = points.getOrNull(selectedPointIdx)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            if (inspected != null) {
                val multiHint = if (inspected.recordCount > 1) "（${inspected.recordCount}次均值）" else ""
                Text(
                    text = "${inspected.date.toChineseMonthDay()}$multiHint",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = fitnessColors.weightSoft,
                ) {
                    Text(
                        text = "${ExportFormatter.formatKg(inspected.weightKg)} kg",
                        style = NumericStatStyle.copy(fontSize = 13.sp),
                        color = lineColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Y-axis weight labels
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .width(40.dp)
                    .height(150.dp)
                    .padding(end = 6.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Text(
                    text = ExportFormatter.formatKg(yMax),
                    style = NumericStatStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = ExportFormatter.formatKg((yMax + yMin) / 2.0),
                    style = NumericStatStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = ExportFormatter.formatKg(yMin),
                    style = NumericStatStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Normal),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val totalDays = allDatesInPeriod.size.coerceAtLeast(2)

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(150.dp)
                    .pointerInput(points, totalDays) {
                        detectTapGestures { offset ->
                            if (points.isEmpty()) return@detectTapGestures
                            val padX = 14.dp.toPx()
                            val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                            var nearest = 0
                            var bestDist = Float.MAX_VALUE
                            points.forEachIndexed { idx, pt ->
                                val px = padX + (pt.dayIndex.toFloat() / (totalDays - 1)) * usableW
                                val dist = abs(offset.x - px)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    nearest = idx
                                }
                            }
                            selectedPointIdx = nearest
                        }
                    },
            ) {
                val padX = 14.dp.toPx()
                val padTop = 12.dp.toPx()
                val padBottom = 12.dp.toPx()
                val usableW = (size.width - padX * 2).coerceAtLeast(1f)
                val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)
                val bottomY = padTop + usableH
                val ySpan = (yMax - yMin).coerceAtLeast(0.5)

                // Grid lines
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (i in 0..2) {
                    val y = padTop + usableH * (i / 2f)
                    drawLine(
                        color = gridColor,
                        start = Offset(padX, y),
                        end = Offset(size.width - padX, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i == 2) null else dash,
                    )
                }

                if (points.isEmpty()) return@Canvas

                val coords = points.map { pt ->
                    val x = padX + (pt.dayIndex.toFloat() / (totalDays - 1)) * usableW
                    val ratio = ((pt.weightKg - yMin) / ySpan).coerceIn(0.0, 1.0).toFloat()
                    val y = bottomY - ratio * usableH
                    Offset(x, y)
                }

                if (selectedPointIdx in coords.indices) {
                    val sel = coords[selectedPointIdx]
                    drawLine(
                        color = lineColor.copy(alpha = 0.35f),
                        start = Offset(sel.x, padTop),
                        end = Offset(sel.x, bottomY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dash,
                    )
                }

                // Smooth cubic Bezier path connecting ONLY existing weigh-in points (skipping unweighed days)
                if (coords.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 0 until coords.lastIndex) {
                            val p0 = coords[i]
                            val p1 = coords[i + 1]
                            val midX = (p0.x + p1.x) / 2f
                            cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(coords.last().x, bottomY)
                        lineTo(coords.first().x, bottomY)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(lineColor.copy(alpha = 0.20f), lineColor.copy(alpha = 0.01f)),
                            startY = padTop,
                            endY = bottomY,
                        ),
                    )

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                // Point markers
                coords.forEachIndexed { idx, pt ->
                    val isSel = idx == selectedPointIdx
                    val outerR = if (isSel) 6.dp.toPx() else 4.2.dp.toPx()
                    val innerR = if (isSel) 2.8.dp.toPx() else 2.dp.toPx()
                    drawCircle(color = lineColor, radius = outerR, center = pt)
                    drawCircle(color = surfaceColor, radius = innerR, center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        XAxisLabelsRow(
            dates = allDatesInPeriod,
            periodType = periodType,
            modifier = Modifier.padding(start = 40.dp),
        )
    }
}

@Composable
private fun XAxisLabelsRow(
    dates: List<DateKey>,
    periodType: PeriodType,
    modifier: Modifier = Modifier,
) {
    if (dates.isEmpty()) return
    val labels = when (periodType) {
        PeriodType.WEEK -> dates.map { it.toShortMonthDay() }
        PeriodType.MONTH -> {
            val last = dates.lastIndex
            val indices = listOf(0, last / 4, last / 2, (last * 3) / 4, last).distinct()
            indices.map { "${dates[it].day}日" }
        }
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        labels.forEach { lbl ->
            Text(
                text = lbl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatAxisVal(value: Double, isCardio: Boolean): String {
    return if (isCardio) ExportFormatter.formatKm(value) else value.roundToInt().toString()
}
