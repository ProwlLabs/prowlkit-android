package com.prowllabs.prowl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.prowllabs.prowl.ui.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.ui.theme.ProwlColors
import kotlin.math.roundToInt

data class ProwlRequestStats(
    val total: Int,
    val success2xx: Int,
    val redirect3xx: Int,
    val client4xx: Int,
    val server5xx: Int,
    val other: Int,
    val methodCounts: Map<String, Int>,
    val avgDurationMs: Long,
)

fun computeRequestStats(logs: List<NetworkLog>): ProwlRequestStats {
    var s2 = 0
    var s3 = 0
    var s4 = 0
    var s5 = 0
    var other = 0
    val methods = linkedMapOf<String, Int>()
    var durationSum = 0L

    logs.forEach { log ->
        durationSum += log.durationMillis
        when (log.statusCode) {
            in 200..299 -> s2++
            in 300..399 -> s3++
            in 400..499 -> s4++
            in 500..599 -> s5++
            else -> other++
        }
        val method = log.method.uppercase().ifBlank { "OTHER" }
        methods[method] = (methods[method] ?: 0) + 1
    }

    return ProwlRequestStats(
        total = logs.size,
        success2xx = s2,
        redirect3xx = s3,
        client4xx = s4,
        server5xx = s5,
        other = other,
        methodCounts = methods,
        avgDurationMs = if (logs.isEmpty()) 0L else durationSum / logs.size,
    )
}

@Composable
fun ProwlStatsSection(logs: List<NetworkLog>, modifier: Modifier = Modifier) {
    val stats = computeRequestStats(logs)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ProwlStatsSummaryRow(stats)
        ProwlStatusDonutChart(stats)
        ProwlMethodBarChart(stats)
    }
}

@Composable
private fun ProwlStatsSummaryRow(stats: ProwlRequestStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryPill("Total", stats.total.toString(), Modifier.weight(1f))
        SummaryPill(
            "Avg",
            if (stats.total == 0) "—" else "${stats.avgDurationMs}ms",
            Modifier.weight(1f),
        )
        SummaryPill(
            "2xx",
            if (stats.total == 0) "0%" else "${(stats.success2xx * 100f / stats.total).roundToInt()}%",
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProwlStatusDonutChart(stats: ProwlRequestStats) {
    val slices = listOf(
        "2xx" to stats.success2xx to ProwlColors.Status2xx,
        "3xx" to stats.redirect3xx to ProwlColors.Status3xx,
        "4xx" to stats.client4xx to ProwlColors.Status4xx,
        "5xx" to stats.server5xx to ProwlColors.Status5xx,
        "Other" to stats.other to Color(0xFF8E8E93),
    ).filter { it.first.second > 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.prowl_stats_status_distribution),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (stats.total == 0) {
            Text(stringResource(R.string.prowl_empty_logs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val stroke = 22f
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)
                var start = -90f
                slices.forEach { (labelCount, color) ->
                    val (_, count) = labelCount
                    val sweep = 360f * count / stats.total
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
            Column(
                modifier = Modifier.padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                slices.forEach { (labelCount, color) ->
                    val (label, count) = labelCount
                    LegendRow(label = label, count = count, color = color, total = stats.total)
                }
            }
        }
    }
}

@Composable
private fun ProwlMethodBarChart(stats: ProwlRequestStats) {
    val entries = stats.methodCounts.entries.sortedByDescending { it.value }.take(6)
    val max = entries.maxOfOrNull { it.value } ?: 0

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.prowl_stats_methods),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (entries.isEmpty()) {
            Text(stringResource(R.string.prowl_empty_logs), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        entries.forEach { (method, count) ->
            val fraction = if (max == 0) 0f else count.toFloat() / max
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = method,
                    modifier = Modifier.padding(end = 8.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = methodColor(method),
                )
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp),
                ) {
                    val barWidth = size.width * fraction
                    drawRoundRect(
                        color = methodColor(method).copy(alpha = 0.25f),
                        size = Size(size.width, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    )
                    if (barWidth > 0f) {
                        drawRoundRect(
                            color = methodColor(method),
                            size = Size(barWidth, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                        )
                    }
                }
                Text(
                    text = count.toString(),
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LegendRow(label: String, count: Int, color: Color, total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = stringResource(
                R.string.prowl_stats_row,
                label,
                count,
                if (total == 0) 0 else (count * 100 / total),
            ),
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp,
        )
    }
}
