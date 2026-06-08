package com.prowllabs.prowl.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProwlShareSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onShareJson: () -> String,
    onShareCurl: () -> String,
    onCreateMock: (() -> Unit)? = null,
    onCreateRequestRewrite: (() -> Unit)? = null,
    onCopied: (String) -> Unit,
) {
    if (!visible) return
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = "Share",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            ShareOption(
                icon = { Icon(Icons.Outlined.Share, null, modifier = Modifier.size(22.dp)) },
                title = "Share JSON",
                subtitle = "Export log as formatted JSON",
                onClick = {
                    shareText(context, onShareJson())
                    onDismiss()
                },
            )
            ShareOption(
                icon = { Icon(Icons.AutoMirrored.Outlined.Article, null, modifier = Modifier.size(22.dp)) },
                title = "Copy JSON",
                subtitle = "Copy to clipboard",
                onClick = {
                    copyText(context, onShareJson())
                    onCopied("JSON copied")
                    onDismiss()
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ShareOption(
                icon = { Icon(Icons.Outlined.Terminal, null, modifier = Modifier.size(22.dp)) },
                title = "Share cURL",
                subtitle = "Export as cURL command",
                onClick = {
                    shareText(context, onShareCurl())
                    onDismiss()
                },
            )
            ShareOption(
                icon = { Icon(Icons.Outlined.ContentCopy, null, modifier = Modifier.size(22.dp)) },
                title = "Copy cURL",
                subtitle = "Copy command to clipboard",
                onClick = {
                    copyText(context, onShareCurl())
                    onCopied("cURL copied")
                    onDismiss()
                },
            )
            if (onCreateMock != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ShareOption(
                    icon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(22.dp)) },
                    title = "Create Mock",
                    subtitle = "Mock this response for future requests",
                    onClick = {
                        onDismiss()
                        onCreateMock()
                    },
                )
            }
            if (onCreateRequestRewrite != null) {
                ShareOption(
                    icon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(22.dp)) },
                    title = "Rewrite Request",
                    subtitle = "Modify URL, headers, or body for matching requests",
                    onClick = {
                        onDismiss()
                        onCreateRequestRewrite()
                    },
                )
            }
        }
    }
}

@Composable
private fun ShareOption(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share via Prowl").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Prowl", text))
}
