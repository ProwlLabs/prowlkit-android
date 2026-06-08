package com.prowllabs.prowl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prowllabs.prowl.ui.theme.ProwlColors

fun methodColor(method: String): Color = when (method.uppercase()) {
    "GET" -> ProwlColors.MethodGet
    "POST" -> ProwlColors.MethodPost
    "PUT", "PATCH" -> ProwlColors.MethodPutPatch
    "DELETE" -> ProwlColors.MethodDelete
    else -> Color.Gray
}

fun statusColor(code: Int?): Color = when (code) {
    null -> Color(0xFFFF3B30)
    in 200..299 -> ProwlColors.Status2xx
    in 300..399 -> ProwlColors.Status3xx
    in 400..499 -> ProwlColors.Status4xx
    in 500..599 -> ProwlColors.Status5xx
    else -> Color.Gray
}

@Composable
fun ProwlMethodBadge(method: String, modifier: Modifier = Modifier) {
    val color = methodColor(method)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = method.uppercase(),
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}
