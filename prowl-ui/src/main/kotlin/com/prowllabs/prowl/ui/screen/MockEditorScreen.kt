package com.prowllabs.prowl.ui.screen

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
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockEditorScreen(
    sourceLogId: UUID?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val logs by ProwlRuntime.storage.logsFlow.collectAsState()
    val sourceLog = sourceLogId?.let { id -> logs.firstOrNull { it.id == id } }

    var urlPattern by remember(sourceLog) {
        mutableStateOf(sourceLog?.url?.substringAfter("://")?.substringAfter('/')?.let { "/$it" }
            ?: sourceLog?.url.orEmpty())
    }
    var method by remember(sourceLog) { mutableStateOf(sourceLog?.method ?: "GET") }
    var statusCode by remember(sourceLog) {
        mutableStateOf((sourceLog?.statusCode ?: 200).toString())
    }
    var body by remember(sourceLog) {
        mutableStateOf(
            sourceLog?.responseBody?.let { ProwlLogFormatter.prettyBodyText(it) }.orEmpty(),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock Editor") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = urlPattern,
                onValueChange = { urlPattern = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL pattern (substring match)") },
            )
            OutlinedTextField(
                value = method,
                onValueChange = { method = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Method (or ANY)") },
            )
            OutlinedTextField(
                value = statusCode,
                onValueChange = { statusCode = it.filter { ch -> ch.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Status code") },
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Response body") },
                minLines = 8,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
            )
            Button(
                onClick = {
                    val rule = ProwlMockRule(
                        targetUrlPattern = urlPattern.trim(),
                        targetMethod = method.trim().ifBlank { "ANY" },
                        mockStatusCode = statusCode.toIntOrNull() ?: 200,
                        mockBody = body.toByteArray(Charsets.UTF_8),
                    )
                    ProwlRuntime.mocker.addRule(rule)
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlPattern.isNotBlank(),
            ) {
                Text("Save Mock Rule")
            }
        }
    }
}
