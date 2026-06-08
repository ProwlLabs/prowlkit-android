package com.prowllabs.prowl.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.model.MultipartPart
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.util.BodyDecoder
import com.prowllabs.prowl.ui.util.ProwlJsonHighlighter

@Composable
fun ProwlBodyViewer(
    body: NetworkLog.Body?,
    emptyText: String,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (body == null || body.data.isEmpty()) {
        Text(emptyText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val contentType = body.contentType
    val text = remember(body) {
        if (BodyDecoder.isBinaryContentType(contentType) &&
            !BodyDecoder.looksLikeText(body.data, contentType)
        ) {
            BodyDecoder.hexPreview(body.data)
        } else {
            ProwlLogFormatter.prettyBodyText(body)
        }
    }

    Column(modifier = modifier) {
        if (BodyDecoder.isImageContentType(contentType)) {
            val bitmap = remember(body.data) {
                BitmapFactory.decodeByteArray(body.data, 0, body.data.size)
            }
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Response image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .padding(bottom = 8.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        if (ProwlJsonHighlighter.looksLikeJson(text, contentType)) {
            ProwlJsonTreeView(json = text, modifier = Modifier.fillMaxWidth())
        } else {
            val highlighted = ProwlJsonHighlighter.highlight(text, contentType)
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = "Tap to copy body",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun ProwlMultipartViewer(parts: List<MultipartPart>) {
    if (parts.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        parts.forEachIndexed { index, part ->
            Text(
                text = "Part ${index + 1}",
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            part.name?.let {
                Text("name: $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            part.fileName?.let {
                Text("filename: $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            part.contentType?.let {
                Text("type: $it", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
            Text("size: ${part.sizeBytes} bytes", fontSize = 12.sp)
            part.textPreview?.let {
                Text(
                    text = it,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
