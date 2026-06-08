package com.prowllabs.prowl.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.util.BodyDecoder
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockEditorSheetContent(
    sourceLog: NetworkLog?,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlPattern by remember(sourceLog) {
        mutableStateOf(
            sourceLog?.url?.let { url ->
                runCatching {
                    val path = java.net.URI(url).path
                    path.ifEmpty { "/" }
                }.getOrElse { url }
            }.orEmpty(),
        )
    }
    var method by remember(sourceLog) { mutableStateOf(sourceLog?.method ?: "GET") }
    var statusCode by remember(sourceLog) {
        mutableStateOf((sourceLog?.statusCode ?: 200).toString())
    }
    var body by remember(sourceLog) {
        mutableStateOf(
            sourceLog?.responseBody?.let { responseBody ->
                val raw = BodyDecoder.toText(responseBody.data, responseBody.contentType)
                if (raw.isBlank()) {
                    ProwlLogFormatter.prettyBodyText(responseBody)
                } else {
                    runCatching { ProwlLogFormatter.prettyBodyText(responseBody) }.getOrDefault(raw)
                }
            }.orEmpty(),
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.prowl_mock_create_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.prowl_mock_create_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = urlPattern,
            onValueChange = { urlPattern = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prowl_mock_url_pattern)) },
            placeholder = { Text(stringResource(R.string.prowl_mock_url_placeholder)) },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = method,
                onValueChange = { method = it.uppercase() },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.prowl_mock_method)) },
                placeholder = { Text(stringResource(R.string.prowl_mock_method_placeholder)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = statusCode,
                onValueChange = { statusCode = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.prowl_mock_status)) },
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prowl_mock_response_body)) },
            minLines = 6,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
        )
        Button(
            onClick = {
                val rule = ProwlMockRule(
                    targetUrlPattern = urlPattern.trim(),
                    targetMethod = method.trim().ifBlank { "ANY" },
                    mockStatusCode = statusCode.toIntOrNull()?.coerceIn(100, 599) ?: 200,
                    mockBody = body.toByteArray(Charsets.UTF_8),
                    mockHeaders = mapOf("Content-Type" to "application/json; charset=utf-8"),
                )
                ProwlRuntime.mocker.addRule(rule)
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = urlPattern.isNotBlank(),
        ) {
            Text(stringResource(R.string.prowl_mock_save_rule))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockEditorScreen(
    sourceLogId: UUID?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val logs by ProwlRuntime.storage.logsFlow.collectAsState()
    val sourceLog = sourceLogId?.let { id -> logs.firstOrNull { it.id == id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prowl_mock_create_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        MockEditorSheetContent(
            sourceLog = sourceLog,
            onSaved = onSaved,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        )
    }
}
