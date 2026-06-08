package com.prowllabs.prowl.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.core.formatting.ProwlExportFormat
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.runtime.ProwlRuntime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMocks: () -> Unit,
) {
    val context = LocalContext.current
    val logs by ProwlRuntime.storage.logsFlow.collectAsState()
    var loggingEnabled by remember { mutableStateOf(ProwlRuntime.isLoggingEnabled) }
    var maskingEnabled by remember { mutableStateOf(ProwlRuntime.isSensitiveDataMaskingEnabled) }

    val total = logs.size
    val successCount = logs.count { (it.statusCode ?: 0) in 200..299 }
    val errorCount = logs.count { (it.statusCode ?: 0) >= 400 || it.errorDescription != null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Statistics")
            Text("Total requests: $total")
            Text("2xx rate: ${if (total == 0) 0 else successCount * 100 / total}%")
            Text("Errors: $errorCount")

            RowSwitch(
                label = "Logging enabled",
                checked = loggingEnabled,
                onCheckedChange = {
                    loggingEnabled = it
                    ProwlRuntime.isLoggingEnabled = it
                },
            )
            RowSwitch(
                label = "Sensitive data masking",
                checked = maskingEnabled,
                onCheckedChange = {
                    maskingEnabled = it
                    ProwlRuntime.isSensitiveDataMaskingEnabled = it
                },
            )

            Button(onClick = onOpenMocks, modifier = Modifier.fillMaxWidth()) {
                Text("Active Mocks (${ProwlRuntime.mocker.allRules().size})")
            }

            Button(
                onClick = {
                    shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.FORMATTED_TEXT))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export formatted text")
            }
            Button(
                onClick = {
                    shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.CURL_COMMANDS))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export cURL commands")
            }
        }
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun shareExport(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
