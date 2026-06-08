package com.prowllabs.prowl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.theme.ProwlColors
import com.prowllabs.prowl.ui.util.urlPath

private val BubbleShape = RoundedCornerShape(
    topStart = 6.dp,
    topEnd = 18.dp,
    bottomEnd = 18.dp,
    bottomStart = 18.dp,
)

@Composable
fun ProwlLogRow(
    log: NetworkLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWatched: Boolean = false,
) {
    val bubbleColor = bubbleBackground(log.statusCode)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 1.dp, shape = BubbleShape, clip = false)
                .clip(BubbleShape)
                .background(bubbleColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProwlMethodStatusLine(method = log.method, statusCode = log.statusCode)
                    if (log.endpointRateAlertTriggered) {
                        RateAlertBadge()
                    }
                    if (isWatched) {
                        WatchBadge()
                    }
                    if (log.requestRewritten) {
                        RewriteBadge()
                    }
                }
                Text(
                    text = log.urlPath(),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun bubbleBackground(statusCode: Int?): androidx.compose.ui.graphics.Color =
    statusColor(statusCode).copy(alpha = 0.14f)
        .compositeOver(MaterialTheme.colorScheme.surface)

@Composable
fun ProwlMethodStatusLine(
    method: String,
    statusCode: Int?,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = method.uppercase(),
            color = methodColor(method),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        Text(
            text = " - ",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
        Text(
            text = statusCode?.toString() ?: "ERR",
            color = statusColor(statusCode),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun ProwlStatusCapsule(
    statusCode: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val color = statusColor(statusCode)
    val label = statusCode?.let { "Status $it" } ?: "No response"
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
}

@Composable
fun ProwlMethodCapsule(
    method: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val color = methodColor(method)
    Text(
        text = method.uppercase(),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
    )
}

@Composable
private fun RewriteBadge() {
    Text(
        text = stringResource(R.string.prowl_rewrite_badge),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ProwlColors.MethodPost.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = ProwlColors.MethodPost,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun WatchBadge() {
    Text(
        text = stringResource(R.string.prowl_watch_badge),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ProwlColors.MethodGet.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = ProwlColors.MethodGet,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun RateAlertBadge() {
    Text(
        text = "⚡ ${stringResource(R.string.prowl_rate_badge)}",
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(ProwlColors.Status4xx.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = ProwlColors.Status4xx,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
    )
}
