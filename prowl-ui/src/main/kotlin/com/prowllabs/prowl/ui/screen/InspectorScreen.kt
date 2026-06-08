package com.prowllabs.prowl.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.ui.viewmodel.InspectorViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLog: (UUID) -> Unit,
    viewModel: InspectorViewModel = viewModel(),
) {
    val logs by viewModel.logs.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = remember(logs, query) {
        if (query.isBlank()) logs else logs.filter { log -> log.matchesQuery(query) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prowl Inspector") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear logs")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { viewModel.setLoggingEnabled(!viewModel.isLoggingEnabled) },
                    label = {
                        Text(if (viewModel.isLoggingEnabled) "Logging On" else "Logging Off")
                    },
                )
                AssistChip(
                    onClick = {
                        viewModel.setSensitiveDataMaskingEnabled(!viewModel.isSensitiveDataMaskingEnabled)
                    },
                    label = {
                        Text(
                            if (viewModel.isSensitiveDataMaskingEnabled) "Masked" else "Raw",
                        )
                    },
                )
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search URL, method, status, body…") },
                singleLine = true,
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { log ->
                    LogRow(log = log, onClick = { onOpenLog(log.id) })
                }
            }
        }
    }
}

@Composable
private fun LogRow(log: NetworkLog, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = log.method,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = log.statusCode?.toString() ?: "—",
                color = statusColor(log.statusCode),
                fontWeight = FontWeight.SemiBold,
            )
            if (log.endpointRateAlertTriggered) {
                Text("⚠️", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            text = log.url.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
        )
        Text(
            text = "${log.durationMillis}ms",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun statusColor(code: Int?) = when {
    code == null -> MaterialTheme.colorScheme.onSurfaceVariant
    code in 200..299 -> MaterialTheme.colorScheme.primary
    code in 400..499 -> MaterialTheme.colorScheme.tertiary
    code >= 500 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

private fun NetworkLog.matchesQuery(query: String): Boolean {
    val q = query.lowercase()
    return listOfNotNull(
        url?.lowercase(),
        method.lowercase(),
        statusCode?.toString(),
        requestBody?.data?.toString(Charsets.UTF_8)?.lowercase(),
        responseBody?.data?.toString(Charsets.UTF_8)?.lowercase(),
    ).any { it.contains(q) }
}
