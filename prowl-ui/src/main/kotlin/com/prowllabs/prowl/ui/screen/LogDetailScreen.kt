package com.prowllabs.prowl.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    logId: UUID,
    onBack: () -> Unit,
    onCreateMock: () -> Unit,
) {
    val logs by ProwlRuntime.storage.logsFlow.collectAsState()
    val log = logs.firstOrNull { it.id == logId }
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var shareMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateMock) {
                        Icon(Icons.Default.Edit, contentDescription = "Create mock")
                    }
                    IconButton(onClick = { shareMenuExpanded = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    DropdownMenu(
                        expanded = shareMenuExpanded,
                        onDismissRequest = { shareMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share JSON") },
                            onClick = {
                                shareMenuExpanded = false
                                log?.let { shareText(context, ProwlLogFormatter.shareText(it)) }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share cURL") },
                            onClick = {
                                shareMenuExpanded = false
                                log?.let { shareText(context, ProwlLogFormatter.curlCommand(it)) }
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (log == null) {
            Text(
                text = "Log not found",
                modifier = Modifier.padding(padding).padding(16.dp),
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Info") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Request") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Response") })
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                when (selectedTab) {
                    0 -> InfoTab(log)
                    1 -> PayloadTab(
                        headers = log.requestHeaders,
                        body = log.requestBody,
                    )
                    2 -> PayloadTab(
                        headers = log.responseHeaders,
                        body = log.responseBody,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoTab(log: NetworkLog) {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        DetailLine("URL", log.url.orEmpty())
        DetailLine("Method", log.method)
        DetailLine("Status", log.statusCode?.toString() ?: "N/A")
        DetailLine("Duration", "${log.durationMillis} ms")
        if (log.endpointRateAlertTriggered) {
            Text(
                text = "Endpoint rate alert triggered",
                color = MaterialTheme.colorScheme.error,
            )
        }
        log.errorDescription?.let { DetailLine("Error", it) }
    }
}

@Composable
private fun PayloadTab(headers: Map<String, String>, body: NetworkLog.Body?) {
    Text("Headers", style = MaterialTheme.typography.titleSmall)
    headers.toSortedMap(String.CASE_INSENSITIVE_ORDER).forEach { (key, value) ->
        Text("$key: $value", fontFamily = FontFamily.Monospace)
    }
    Text("Body", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
    Text(
        text = body?.let { ProwlLogFormatter.prettyBodyText(it) } ?: "(empty)",
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DetailLine(label: String, value: String) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Text(value, fontFamily = FontFamily.Monospace)
}

private fun shareText(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
