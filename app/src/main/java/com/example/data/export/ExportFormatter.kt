package com.example.data.export

import com.example.data.local.DailyWorkoutEntity
import com.example.data.local.WeightRecordEntity
import com.example.domain.AnalyticsCalculator
import com.example.domain.DateKey
import com.example.domain.DateUtil
import java.util.Locale

enum class ExportScope {
    CURRENT_PERIOD,
    ALL_HISTORY,
}

enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    TXT("txt", "text/plain"),
}

object ExportFormatter {

    private const val UTF8_BOM = "\uFEFF"

    fun buildFileName(
        scopeLabelForFile: String,
        format: ExportFormat,
    ): String {
        val stamp = DateUtil.formatDateTimeForFile()
        val safeScope = scopeLabelForFile
            .replace(" ", "")
            .replace("/", "-")
            .replace(":", "-")
        return "每日训记_${safeScope}_${stamp}.${format.extension}"
    }

    /**
     * Generates a UTF-8 BOM CSV table so mobile & desktop Excel renders Chinese without mojibake.
     */
    fun generateCsv(
        workouts: List<DailyWorkoutEntity>,
        weights: List<WeightRecordEntity>,
    ): String {
        val workoutMap = workouts.associateBy { it.date }
        val weightMap = weights.groupBy { it.date }
        val allDates = (workoutMap.keys + weightMap.keys).sorted()

        val sb = StringBuilder()
        sb.append(UTF8_BOM)
        sb.appendLine(
            "日期,上肢(组),上肢(个),下肢(组),下肢(个),弯起(组),弯起(个),躺起(组),躺起(个),力量总计(组),力量总计(个),长跑(km),当日体重均值(kg),体重记录明细",
        )

        for (iso in allDates) {
            val w = workoutMap[iso]
            val dayWeights = weightMap[iso].orEmpty().sortedBy { it.recordedAtMillis }

            val upperSets = w?.upperSets ?: 0
            val lowerSets = w?.lowerSets ?: 0
            val crunchSets = w?.crunchSets ?: 0
            val situpSets = w?.situpSets ?: 0
            val totalSets = upperSets + lowerSets + crunchSets + situpSets
            val runStr = if (w != null && w.runKm > 0.0) formatKm(w.runKm) else "0"

            val avgWeightStr = if (dayWeights.isNotEmpty()) {
                formatKg(AnalyticsCalculator.round1(dayWeights.map { it.weightKg }.average()))
            } else {
                ""
            }

            val weightDetails = dayWeights.joinToString("；") { rec ->
                val time = DateUtil.formatTime(rec.recordedAtMillis)
                val notePart = if (rec.note.isNotBlank()) "(${rec.note})" else ""
                "$time ${formatKg(rec.weightKg)}kg$notePart"
            }

            val row = listOf(
                iso,
                upperSets.toString(),
                (upperSets * 10).toString(),
                lowerSets.toString(),
                (lowerSets * 10).toString(),
                crunchSets.toString(),
                (crunchSets * 10).toString(),
                situpSets.toString(),
                (situpSets * 10).toString(),
                totalSets.toString(),
                (totalSets * 10).toString(),
                runStr,
                avgWeightStr,
                weightDetails,
            ).joinToString(",") { escapeCsvField(it) }

            sb.appendLine(row)
        }
        return sb.toString()
    }

    /**
     * Generates a clean, readable Chinese training report (.txt) suitable for reading or sharing.
     */
    fun generateTxtReport(
        title: String,
        periodDescription: String,
        workouts: List<DailyWorkoutEntity>,
        weights: List<WeightRecordEntity>,
    ): String {
        val workoutMap = workouts.associateBy { it.date }
        val weightMap = weights.groupBy { it.date }
        val allDates = (workoutMap.keys + weightMap.keys).sorted()

        val activeDays = workouts.count { !it.isEmpty }
        val upperTotal = workouts.sumOf { it.upperSets }
        val lowerTotal = workouts.sumOf { it.lowerSets }
        val crunchTotal = workouts.sumOf { it.crunchSets }
        val situpTotal = workouts.sumOf { it.situpSets }
        val strengthTotal = upperTotal + lowerTotal + crunchTotal + situpTotal
        val runTotal = AnalyticsCalculator.round2(workouts.sumOf { it.runKm })
        val runDays = workouts.count { it.hasRun }

        val sortedWeights = weights.sortedWith(compareBy<WeightRecordEntity> { it.date }.thenBy { it.recordedAtMillis })
        val weightSummary = when {
            sortedWeights.isEmpty() -> "暂无体重记录"
            sortedWeights.size == 1 -> "记录 1 次：${formatKg(sortedWeights.first().weightKg)} kg"
            else -> {
                val first = sortedWeights.first().weightKg
                val last = sortedWeights.last().weightKg
                val diff = AnalyticsCalculator.round1(last - first)
                val diffSign = if (diff > 0) "+${formatKg(diff)}" else formatKg(diff)
                "${formatKg(first)} kg → ${formatKg(last)} kg（净变化 ${diffSign} kg，共 ${sortedWeights.size} 次）"
            }
        }

        val sb = StringBuilder()
        sb.appendLine("══════════════════════════════")
        sb.appendLine("  $title")
        sb.appendLine("  统计范围：$periodDescription")
        sb.appendLine("══════════════════════════════")
        sb.appendLine()
        sb.appendLine("【周期汇总】")
        sb.appendLine("• 有效训练天数：$activeDays 天")
        sb.appendLine("• 力量总训练量：$strengthTotal 组（共 ${strengthTotal * 10} 个）")
        sb.appendLine("  - 上肢：$upperTotal 组（${upperTotal * 10} 个）")
        sb.appendLine("  - 下肢：$lowerTotal 组（${lowerTotal * 10} 个）")
        sb.appendLine("  - 弯起：$crunchTotal 组（${crunchTotal * 10} 个）")
        sb.appendLine("  - 躺起：$situpTotal 组（${situpTotal * 10} 个）")
        sb.appendLine("• 长跑总跑量：${formatKm(runTotal)} km（共 $runDays 次）")
        sb.appendLine("• 体重指标：$weightSummary")
        sb.appendLine()
        sb.appendLine("──────────────────────────────")
        sb.appendLine("【逐日明细】")

        if (allDates.isEmpty()) {
            sb.appendLine("该范围内暂无训练或体重记录。")
        } else {
            for (iso in allDates) {
                val dateKey = DateKey.parse(iso)
                val weekday = DateUtil.weekdayChinese(dateKey)
                val w = workoutMap[iso]
                val dayWeights = weightMap[iso].orEmpty().sortedBy { it.recordedAtMillis }

                sb.appendLine()
                sb.appendLine("■ $iso（$weekday）")
                if (w != null && !w.isEmpty) {
                    if (w.hasStrength) {
                        val parts = buildList {
                            if (w.upperSets > 0) add("上肢 ${w.upperSets}组(${w.upperReps}个)")
                            if (w.lowerSets > 0) add("下肢 ${w.lowerSets}组(${w.lowerReps}个)")
                            if (w.crunchSets > 0) add("弯起 ${w.crunchSets}组(${w.crunchReps}个)")
                            if (w.situpSets > 0) add("躺起 ${w.situpSets}组(${w.situpReps}个)")
                        }.joinToString(" / ")
                        sb.appendLine("  力量：共 ${w.totalStrengthSets} 组(${w.totalStrengthReps} 个) — $parts")
                    }
                    if (w.hasRun) {
                        sb.appendLine("  长跑：${formatKm(w.runKm)} km")
                    }
                }
                if (dayWeights.isNotEmpty()) {
                    val wStr = dayWeights.joinToString("，") { rec ->
                        val t = DateUtil.formatTime(rec.recordedAtMillis)
                        val note = if (rec.note.isNotBlank()) " [${rec.note}]" else ""
                        "$t ${formatKg(rec.weightKg)}kg$note"
                    }
                    sb.appendLine("  体重：$wStr")
                }
            }
        }
        sb.appendLine()
        sb.appendLine("──────────────────────────────")
        sb.appendLine("由「每日训记」离线生成")
        return sb.toString()
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    fun formatKm(value: Double): String {
        val rounded = AnalyticsCalculator.round2(value)
        return if (rounded % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", rounded)
        } else if ((rounded * 10.0) % 1.0 == 0.0) {
            String.format(Locale.US, "%.1f", rounded)
        } else {
            String.format(Locale.US, "%.2f", rounded)
        }
    }

    fun formatKg(value: Double): String = String.format(Locale.US, "%.1f", AnalyticsCalculator.round1(value))
}
