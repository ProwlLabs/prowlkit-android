package com.prowllabs.prowl.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.model.NetworkLog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prowllabs.prowl.ui.viewmodel.LogDetailViewModel
import com.prowllabs.prowl.ui.components.ProwlCapsuleTabBar
import com.prowllabs.prowl.ui.components.ProwlCopyToast
import com.prowllabs.prowl.ui.components.ProwlFooterCredit
import com.prowllabs.prowl.ui.components.ProwlLabeledValue
import com.prowllabs.prowl.ui.components.ProwlMethodCapsule
import com.prowllabs.prowl.ui.components.ProwlMethodStatusLine
import com.prowllabs.prowl.ui.components.ProwlSectionCard
import com.prowllabs.prowl.ui.components.ProwlSectionHeader
import com.prowllabs.prowl.ui.components.ProwlShareSheet
import com.prowllabs.prowl.ui.components.ProwlStatusCapsule
import com.prowllabs.prowl.ui.theme.ProwlColors
import com.prowllabs.prowl.ui.components.ProwlBodyViewer
import com.prowllabs.prowl.ui.components.ProwlMultipartViewer
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.util.ProwlWatchStore
import com.prowllabs.prowl.ui.util.formattedDurationSeconds
import com.prowllabs.prowl.ui.util.formattedResponseAt
import com.prowllabs.prowl.ui.util.formattedStartedAt
import com.prowllabs.prowl.ui.util.urlQueryItems
import java.util.UUID
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    logId: UUID,
    onBack: () -> Unit,
    viewModel: LogDetailViewModel = viewModel(),
) {
    val logs by viewModel.logs.collectAsState()
    val log = logs.firstOrNull { it.id == logId }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var shareVisible by remember { mutableStateOf(false) }
    var mockVisible by remember { mutableStateOf(false) }
    var rewriteVisible by remember { mutableStateOf(false) }
    var copyToast by remember { mutableStateOf<String?>(null) }
    var isWatched by remember(log?.id) {
        mutableStateOf(log?.let { ProwlWatchStore.isWatched(context, it) } ?: false)
    }

    LaunchedEffect(copyToast) {
        if (copyToast != null) {
            delay(2_000)
            copyToast = null
        }
    }

    ProwlShareSheet(
        visible = shareVisible && log != null,
        onDismiss = { shareVisible = false },
        onShareJson = { log?.let { ProwlLogFormatter.shareText(it) }.orEmpty() },
        onShareCurl = { log?.let { ProwlLogFormatter.curlCommand(it) }.orEmpty() },
        onCreateMock = { mockVisible = true },
        onCreateRequestRewrite = { rewriteVisible = true },
        onCopied = { copyToast = it },
    )

    if (log != null) {
        MockEditorBottomSheet(
            log = log,
            visible = mockVisible,
            onDismiss = { mockVisible = false },
            onSaved = {
                mockVisible = false
                copyToast = context.getString(R.string.prowl_mock_saved)
            },
        )
        RequestRewriteEditorBottomSheet(
            log = log,
            visible = rewriteVisible,
            onDismiss = { rewriteVisible = false },
            onSaved = {
                rewriteVisible = false
                copyToast = context.getString(R.string.prowl_rewrite_saved)
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.prowl_request_detail), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        log?.let {
                            ProwlMethodStatusLine(
                                method = it.method,
                                statusCode = it.statusCode,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.prowl_back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            log?.let {
                                isWatched = ProwlWatchStore.toggleWatch(context, it)
                            }
                        },
                        enabled = log != null,
                    ) {
                        Icon(
                            imageVector = if (isWatched) Icons.Filled.Star else Icons.Filled.FavoriteBorder,
                            contentDescription = stringResource(
                                if (isWatched) R.string.prowl_unwatch_endpoint else R.string.prowl_watch_endpoint,
                            ),
                        )
                    }
                    IconButton(onClick = { shareVisible = true }, enabled = log != null) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.prowl_share))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (log == null) {
                Text(text = stringResource(R.string.prowl_log_not_found), modifier = Modifier.padding(16.dp))
                return@Scaffold
            }

            Column(modifier = Modifier.fillMaxSize()) {
                ProwlCapsuleTabBar(
                    tabs = listOf(
                        stringResource(R.string.prowl_tab_info),
                        stringResource(R.string.prowl_tab_request),
                        stringResource(R.string.prowl_tab_response),
                    ),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (selectedTab) {
                        0 -> InfoTab(log, onCopy = { copyToast = it })
                        1 -> RequestTab(log, onCopy = { label, value ->
                            copyToClipboard(context, label, value)
                            copyToast = "$label copied"
                        })
                        2 -> ResponseTab(log, onCopy = { label, value ->
                            copyToClipboard(context, label, value)
                            copyToast = "$label copied"
                        })
                    }
                    ProwlFooterCredit()
                }
            }

            ProwlCopyToast(
                message = copyToast,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestRewriteEditorBottomSheet(
    log: NetworkLog,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        RequestRewriteEditorSheetContent(
            sourceLog = log,
            onSaved = onSaved,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MockEditorBottomSheet(
    log: NetworkLog,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        MockEditorSheetContent(
            sourceLog = log,
            onSaved = onSaved,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
        )
    }
}

@Composable
private fun InfoTab(log: NetworkLog, onCopy: (String) -> Unit) {
    val context = LocalContext.current

    ProwlSectionCard(title = stringResource(R.string.prowl_section_endpoint)) {
        if (log.responseMocked) {
            Text(
                text = stringResource(R.string.prowl_response_was_mocked),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ProwlColors.JsonLiteral.copy(alpha = 0.12f))
                    .padding(10.dp),
                fontSize = 12.sp,
                color = ProwlColors.JsonLiteral,
            )
        }
        if (log.requestRewritten) {
            Text(
                text = stringResource(R.string.prowl_request_was_rewritten),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ProwlColors.MethodPost.copy(alpha = 0.12f))
                    .padding(10.dp),
                fontSize = 12.sp,
                color = ProwlColors.MethodPost,
            )
        }
        if (log.endpointRateAlertTriggered) {
            Text(
                text = "⚡ ${stringResource(R.string.prowl_rate_alert_detail)}",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ProwlColors.Status4xx.copy(alpha = 0.12f))
                    .padding(10.dp),
                fontSize = 12.sp,
                color = ProwlColors.Status4xx,
            )
        }
        ProwlLabeledValue(
            label = stringResource(R.string.prowl_label_url),
            value = log.url.orEmpty().ifBlank { "-" },
            onCopy = {
                val label = context.getString(R.string.prowl_label_url)
                copyToClipboard(context, label, log.url.orEmpty())
                onCopy(label)
            },
        )
        log.hostIp?.let { ip ->
            ProwlLabeledValue(
                label = stringResource(R.string.prowl_host_ip),
                value = ip,
                onCopy = {
                    val label = context.getString(R.string.prowl_host_ip)
                    copyToClipboard(context, label, ip)
                    onCopy(label)
                },
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProwlMethodCapsule(method = log.method) {
                val label = context.getString(R.string.prowl_label_method)
                copyToClipboard(context, label, log.method)
                onCopy(label)
            }
            ProwlStatusCapsule(statusCode = log.statusCode) {
                val label = context.getString(R.string.prowl_label_status)
                copyToClipboard(context, label, log.statusCode?.toString() ?: context.getString(R.string.prowl_no_response))
                onCopy(label)
            }
        }
        log.errorDescription?.let { error ->
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
            ProwlLabeledValue(
                label = stringResource(R.string.prowl_label_error),
                value = error,
                valueColor = MaterialTheme.colorScheme.error,
                onCopy = {
                    val label = context.getString(R.string.prowl_label_error)
                    copyToClipboard(context, label, error)
                    onCopy(label)
                },
            )
        }
    }

    ProwlSectionCard(title = stringResource(R.string.prowl_timing)) {
        ProwlLabeledValue(
            label = stringResource(R.string.prowl_label_request_date),
            value = log.formattedStartedAt(),
            onCopy = {
                val label = context.getString(R.string.prowl_label_request_date)
                copyToClipboard(context, label, log.formattedStartedAt())
                onCopy(label)
            },
        )
        if (log.statusCode != null) {
            ProwlLabeledValue(
                label = stringResource(R.string.prowl_label_response_date),
                value = log.formattedResponseAt(),
                onCopy = {
                    val label = context.getString(R.string.prowl_label_response_date)
                    copyToClipboard(context, label, log.formattedResponseAt())
                    onCopy(label)
                },
            )
            ProwlLabeledValue(
                label = stringResource(R.string.prowl_label_time_interval),
                value = log.formattedDurationSeconds(),
                onCopy = {
                    val label = context.getString(R.string.prowl_label_time_interval)
                    copyToClipboard(context, label, log.formattedDurationSeconds())
                    onCopy(label)
                },
            )
        }
        ProwlLabeledValue(
            label = stringResource(R.string.prowl_label_timeout),
            value = log.timeoutMillis?.toString() ?: "-",
            onCopy = {
                val label = context.getString(R.string.prowl_label_timeout)
                copyToClipboard(context, label, log.timeoutMillis?.toString() ?: "-")
                onCopy(label)
            },
        )
        ProwlLabeledValue(
            label = stringResource(R.string.prowl_label_cache_policy),
            value = log.cachePolicy ?: "-",
            onCopy = {
                val label = context.getString(R.string.prowl_label_cache_policy)
                copyToClipboard(context, label, log.cachePolicy ?: "-")
                onCopy(label)
            },
        )
        log.timing?.let { timing ->
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
            timing.dnsMillis?.let {
                ProwlLabeledValue("DNS", "${it}ms", onCopy = {
                    copyToClipboard(context, "DNS", "${it}ms"); onCopy("DNS")
                })
            }
            timing.connectMillis?.let {
                ProwlLabeledValue("Connect", "${it}ms", onCopy = {
                    copyToClipboard(context, "Connect", "${it}ms"); onCopy("Connect")
                })
            }
            timing.secureConnectMillis?.let {
                ProwlLabeledValue("TLS", "${it}ms", onCopy = {
                    copyToClipboard(context, "TLS", "${it}ms"); onCopy("TLS")
                })
            }
            timing.requestBodyMillis?.let {
                ProwlLabeledValue("Request body", "${it}ms", onCopy = {
                    copyToClipboard(context, "Request body", "${it}ms"); onCopy("Request body")
                })
            }
            timing.responseBodyMillis?.let {
                ProwlLabeledValue("Response body", "${it}ms", onCopy = {
                    copyToClipboard(context, "Response body", "${it}ms"); onCopy("Response body")
                })
            }
        }
    }

    val queryItems = log.urlQueryItems()
    if (queryItems.isNotEmpty()) {
        ProwlSectionCard(title = "URL Query Strings") {
            queryItems.forEach { (key, value) ->
                ProwlLabeledValue(
                    label = key,
                    value = value.ifBlank { "(empty)" },
                    onCopy = {
                        copyToClipboard(context, key, value)
                        onCopy(key)
                    },
                )
            }
        }
    }
}

@Composable
private fun RequestTab(
    log: NetworkLog,
    onCopy: (String, String) -> Unit,
) {
    ProwlSectionHeader(title = "Request")
    PayloadSection(
        headers = log.requestHeaders,
        body = log.requestBody,
        multipartParts = log.requestMultipartParts,
        emptyHeadersText = "Request headers are empty",
        emptyBodyText = "Request body is empty",
        bodyToastLabel = "Request body",
        onCopy = onCopy,
    )
}

@Composable
private fun ResponseTab(
    log: NetworkLog,
    onCopy: (String, String) -> Unit,
) {
    ProwlSectionHeader(title = "Response")
    PayloadSection(
        headers = log.responseHeaders,
        body = log.responseBody,
        multipartParts = log.responseMultipartParts,
        emptyHeadersText = "Response headers are empty",
        emptyBodyText = "Response body is empty",
        bodyToastLabel = "Response body",
        onCopy = onCopy,
    )
}

@Composable
private fun PayloadSection(
    headers: Map<String, String>,
    body: NetworkLog.Body?,
    multipartParts: List<com.prowllabs.prowl.core.model.MultipartPart>,
    emptyHeadersText: String,
    emptyBodyText: String,
    bodyToastLabel: String,
    onCopy: (String, String) -> Unit,
) {
    ProwlSectionCard(title = "Headers") {
        if (headers.isEmpty()) {
            Text(emptyHeadersText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            headers.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (key, value) ->
                HeaderRow(key, value, onCopy)
            }
        }
    }

    if (multipartParts.isNotEmpty()) {
        ProwlSectionCard(title = "Multipart") {
            ProwlMultipartViewer(parts = multipartParts)
        }
    }

    ProwlSectionCard(title = "Body") {
        val bodyText = body?.let { ProwlLogFormatter.prettyBodyText(it) }.orEmpty()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.background)
                .clickable(enabled = body != null && bodyText.isNotBlank()) {
                    if (bodyText.isNotBlank()) onCopy(bodyToastLabel, bodyText)
                }
                .padding(12.dp),
        ) {
            ProwlBodyViewer(
                body = body,
                emptyText = emptyBodyText,
                onCopy = { onCopy(bodyToastLabel, bodyText) },
            )
        }
    }
}

@Composable
private fun HeaderRow(key: String, value: String, onCopy: (String, String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(key, "$key: $value") }
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = key,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = ProwlColors.JsonKey,
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}
