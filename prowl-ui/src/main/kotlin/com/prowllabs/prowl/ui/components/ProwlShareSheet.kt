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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.prowllabs.prowl.ui.R
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
                text = stringResource(R.string.prowl_share_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            ShareOption(
                icon = { Icon(Icons.Filled.Share, null, modifier = Modifier.size(22.dp)) },
                title = stringResource(R.string.prowl_share_json_title),
                subtitle = stringResource(R.string.prowl_share_json_sub),
                onClick = {
                    shareText(context, onShareJson())
                    onDismiss()
                },
            )
            ShareOption(
                icon = { Icon(Icons.Filled.List, null, modifier = Modifier.size(22.dp)) },
                title = stringResource(R.string.prowl_copy_json_title),
                subtitle = stringResource(R.string.prowl_copy_json_sub),
                onClick = {
                    copyText(context, onShareJson())
                    onCopied(context.getString(R.string.prowl_json_copied))
                    onDismiss()
                },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ShareOption(
                icon = { Icon(Icons.Filled.Build, null, modifier = Modifier.size(22.dp)) },
                title = stringResource(R.string.prowl_share_curl_title),
                subtitle = stringResource(R.string.prowl_share_curl_sub),
                onClick = {
                    shareText(context, onShareCurl())
                    onDismiss()
                },
            )
            ShareOption(
                icon = { Icon(Icons.Filled.Create, null, modifier = Modifier.size(22.dp)) },
                title = stringResource(R.string.prowl_copy_curl_title),
                subtitle = stringResource(R.string.prowl_copy_curl_sub),
                onClick = {
                    copyText(context, onShareCurl())
                    onCopied(context.getString(R.string.prowl_curl_copied))
                    onDismiss()
                },
            )
            if (onCreateMock != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ShareOption(
                    icon = { Icon(Icons.Filled.Edit, null, modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.prowl_share_create_mock_title),
                    subtitle = stringResource(R.string.prowl_share_create_mock_sub),
                    onClick = {
                        onDismiss()
                        onCreateMock()
                    },
                )
            }
            if (onCreateRequestRewrite != null) {
                ShareOption(
                    icon = { Icon(Icons.Filled.Edit, null, modifier = Modifier.size(22.dp)) },
                    title = stringResource(R.string.prowl_share_rewrite_title),
                    subtitle = stringResource(R.string.prowl_share_rewrite_sub),
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
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.prowl_export_via))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Prowl", text))
}
