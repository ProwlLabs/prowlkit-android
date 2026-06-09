package com.prowllabs.prowl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.theme.ProwlColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
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
    val p95DurationMs: Long,
) {
    val successRatePercent: Int
        get() = if (total == 0) 0 else (success2xx * 100f / total).roundToInt()
}

data class TrafficBucket(
    val label: String,
    val count: Int,
    val avgDurationMs: Long,
)

data class LatencyBucket(
    val label: String,
    val count: Int,
    val color: Color,
)

fun computeRequestStats(logs: List<NetworkLog>): ProwlRequestStats {
    var s2 = 0
    var s3 = 0
    var s4 = 0
    var s5 = 0
    var other = 0
    val methods = linkedMapOf<String, Int>()
    var durationSum = 0L
    val durations = ArrayList<Long>(logs.size)

    logs.forEach { log ->
        durations += log.durationMillis
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

    val p95 = if (durations.isEmpty()) {
        0L
    } else {
        durations.sorted().let { sorted ->
            val index = ((sorted.size - 1) * 0.95).roundToInt().coerceIn(0, sorted.lastIndex)
            sorted[index]
        }
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
        p95DurationMs = p95,
    )
}

fun computeTrafficBuckets(logs: List<NetworkLog>, maxBuckets: Int = 10): List<TrafficBucket> {
    if (logs.isEmpty()) return emptyList()
    val sorted = logs.sortedBy { it.startedAtMillis }
    val minT = sorted.first().startedAtMillis
    val maxT = sorted.last().startedAtMillis
    val span = (maxT - minT).coerceAtLeast(1L)
    val bucketSize = max(span / maxBuckets, 1_000L)
    val bucketCount = ceil(span.toDouble() / bucketSize).toInt().coerceIn(1, maxBuckets)
    val formatter = SimpleDateFormat(
        if (span < 60_000) "HH:mm:ss" else "HH:mm",
        Locale.getDefault(),
    )

    return List(bucketCount) { index ->
        val start = minT + index * bucketSize
        val end = if (index == bucketCount - 1) maxT + 1 else start + bucketSize
        val inBucket = sorted.filter { it.startedAtMillis in start until end }
        val avg = if (inBucket.isEmpty()) 0L else inBucket.sumOf { it.durationMillis } / inBucket.size
        TrafficBucket(
            label = formatter.format(Date(start)),
            count = inBucket.size,
            avgDurationMs = avg,
        )
    }
}

fun computeLatencyHistogram(logs: List<NetworkLog>): List<LatencyBucket> {
    val ranges = listOf(
        Triple("<100", 0L..99L, Color(0xFF34C759)),
        Triple("100–300", 100L..299L, Color(0xFF30D158)),
        Triple("300–1s", 300L..999L, Color(0xFFFF9500)),
        Triple("1–3s", 1_000L..2_999L, Color(0xFFFF6B00)),
        Triple("3s+", 3_000L..Long.MAX_VALUE, Color(0xFFFF3B30)),
    )
    return ranges.map { (label, range, color) ->
        LatencyBucket(label, logs.count { it.durationMillis in range }, color)
    }.filter { it.count > 0 }
}

@Composable
fun ProwlStatsSection(logs: List<NetworkLog>, modifier: Modifier = Modifier) {
    val stats = remember(logs) { computeRequestStats(logs) }
    val trafficBuckets = remember(logs) { computeTrafficBuckets(logs) }
    val latencyHistogram = remember(logs) { computeLatencyHistogram(logs) }

    if (stats.total == 0) {
        StatsEmptyState(modifier = modifier)
        return
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        MetricsRow(stats)
        if (trafficBuckets.isNotEmpty()) {
            ChartSection(title = stringResource(R.string.prowl_stats_traffic_over_time)) {
                TrafficAreaChart(buckets = trafficBuckets)
            }
            ChartSection(title = stringResource(R.string.prowl_stats_latency_trend)) {
                LatencyLineChart(buckets = trafficBuckets)
            }
        }
        ChartSection(title = stringResource(R.string.prowl_stats_status_distribution)) {
            StatusBarChart(stats = stats)
        }
        if (latencyHistogram.isNotEmpty()) {
            ChartSection(title = stringResource(R.string.prowl_stats_latency_distribution)) {
                LatencyHistogramChart(buckets = latencyHistogram, total = stats.total)
            }
        }
        ChartSection(title = stringResource(R.string.prowl_stats_methods)) {
            MethodBarChart(stats = stats)
        }
    }
}

@Composable
private fun ChartSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun StatsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.prowl_stats_no_traffic),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.prowl_stats_no_traffic_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MetricsRow(stats: ProwlRequestStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricTile(
            value = stats.total.toString(),
            label = stringResource(R.string.prowl_stats_total),
            tint = Color(0xFF007AFF),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            value = "${stats.avgDurationMs}",
            label = stringResource(R.string.prowl_stats_avg_ms),
            tint = Color(0xFFFF9500),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            value = "${stats.p95DurationMs}",
            label = stringResource(R.string.prowl_stats_p95_ms),
            tint = Color(0xFFFF6B00),
            modifier = Modifier.weight(1f),
        )
        MetricTile(
            value = "${stats.successRatePercent}%",
            label = stringResource(R.string.prowl_stats_success),
            tint = Color(0xFF34C759),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricTile(
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.sp,
        )
    }
}

@Composable
private fun TrafficAreaChart(buckets: List<TrafficBucket>) {
    val maxCount = buckets.maxOf { it.count }.coerceAtLeast(1)
    val lineColor = Color(0xFF007AFF)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            YAxisLabels(maxValue = maxCount, modifier = Modifier.width(28.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                val points = buckets.mapIndexed { index, bucket ->
                    val x = if (buckets.size == 1) {
                        constraints.maxWidth / 2f
                    } else {
                        index * constraints.maxWidth.toFloat() / (buckets.size - 1)
                    }
                    val y = constraints.maxHeight - (bucket.count.toFloat() / maxCount * constraints.maxHeight)
                    Offset(x, y)
                }

                Canvas(modifier = Modifier.matchParentSize()) {
                    val gridLines = 4
                    repeat(gridLines + 1) { line ->
                        val y = size.height * line / gridLines
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }

                    if (points.size >= 2) {
                        val fillPath = Path().apply {
                            moveTo(points.first().x, size.height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, size.height)
                            close()
                        }
                        drawPath(
                            fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f)),
                                startY = 0f,
                                endY = size.height,
                            ),
                        )

                        for (i in 0 until points.lastIndex) {
                            drawLine(
                                color = lineColor,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 3f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    points.forEach { point ->
                        drawCircle(lineColor, radius = 5f, center = point)
                        drawCircle(Color.White, radius = 2.5f, center = point)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            buckets.forEachIndexed { index, bucket ->
                if (index == 0 || index == buckets.lastIndex || buckets.size <= 4) {
                    Text(
                        text = bucket.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            buckets.forEach { bucket ->
                Text(
                    text = bucket.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = lineColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun LatencyLineChart(buckets: List<TrafficBucket>) {
    val maxLatency = buckets.maxOf { it.avgDurationMs }.coerceAtLeast(1L)
    val lineColor = Color(0xFFFF9500)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            YAxisLabels(
                maxValue = maxLatency.toInt(),
                suffix = "ms",
                modifier = Modifier.width(36.dp),
            )
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                val points = buckets.mapIndexed { index, bucket ->
                    val x = if (buckets.size == 1) {
                        constraints.maxWidth / 2f
                    } else {
                        index * constraints.maxWidth.toFloat() / (buckets.size - 1)
                    }
                    val y = constraints.maxHeight - (bucket.avgDurationMs.toFloat() / maxLatency * constraints.maxHeight)
                    Offset(x, y)
                }

                Canvas(modifier = Modifier.matchParentSize()) {
                    repeat(5) { line ->
                        val y = size.height * line / 4f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }

                    if (points.size >= 2) {
                        for (i in 0 until points.lastIndex) {
                            drawLine(
                                color = lineColor,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 2.5f,
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    points.forEach { point ->
                        drawCircle(lineColor, radius = 4.5f, center = point)
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.prowl_stats_latency_trend_sub),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 44.dp),
        )
    }
}

@Composable
private fun YAxisLabels(
    maxValue: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
) {
    val labels = listOf(maxValue, maxValue / 2, 0)
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEach { value ->
            Text(
                text = if (suffix.isEmpty()) value.toString() else "$value$suffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun StatusBarChart(stats: ProwlRequestStats) {
    val entries = listOf(
        Triple("2xx", stats.success2xx, ProwlColors.Status2xx),
        Triple("3xx", stats.redirect3xx, ProwlColors.Status3xx),
        Triple("4xx", stats.client4xx, ProwlColors.Status4xx),
        Triple("5xx", stats.server5xx, ProwlColors.Status5xx),
        Triple("—", stats.other, Color(0xFF8E8E93)),
    ).filter { it.second > 0 }

    if (entries.isEmpty()) return

    val max = entries.maxOf { it.second }.coerceAtLeast(1)
    VerticalBarChart(
        entries = entries.map { (label, count, color) ->
            BarChartEntry(label, count, color, stats.total)
        },
        maxValue = max,
        chartHeight = 130.dp,
    )
}

@Composable
private fun MethodBarChart(stats: ProwlRequestStats) {
    val entries = stats.methodCounts.entries
        .sortedByDescending { it.value }
        .take(6)
        .map { (method, count) ->
            BarChartEntry(method, count, methodColor(method), stats.total)
        }

    if (entries.isEmpty()) return

    VerticalBarChart(
        entries = entries,
        maxValue = entries.maxOf { it.value }.coerceAtLeast(1),
        chartHeight = 130.dp,
    )
}

private data class BarChartEntry(
    val label: String,
    val value: Int,
    val color: Color,
    val total: Int,
)

@Composable
private fun VerticalBarChart(
    entries: List<BarChartEntry>,
    maxValue: Int,
    chartHeight: Dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        entries.forEach { entry ->
            val fraction = entry.value.toFloat() / maxValue
            val percent = if (entry.total == 0) 0 else (entry.value * 100f / entry.total).roundToInt()

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.value.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = entry.color,
                    fontSize = 11.sp,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .fillMaxHeight(fraction.coerceIn(0.04f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        entry.color.copy(alpha = 0.95f),
                                        entry.color.copy(alpha = 0.55f),
                                    ),
                                ),
                            ),
                    )
                }
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LatencyHistogramChart(buckets: List<LatencyBucket>, total: Int) {
    val max = buckets.maxOf { it.count }.coerceAtLeast(1)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEach { bucket ->
            val fraction = bucket.count.toFloat() / max
            val percent = if (total == 0) 0 else (bucket.count * 100f / total).roundToInt()

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = bucket.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = bucket.color,
                    fontSize = 11.sp,
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .fillMaxHeight(fraction.coerceIn(0.06f, 1f))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(bucket.color.copy(alpha = 0.85f)),
                    )
                }
                Text(
                    text = bucket.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 11.sp,
                )
            }
        }
    }
}
