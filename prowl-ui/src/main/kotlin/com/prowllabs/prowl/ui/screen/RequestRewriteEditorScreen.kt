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
import androidx.compose.material3.FilterChip
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
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.mocking.ProwlRequestRewriteRule
import com.prowllabs.prowl.core.model.NetworkLog
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.ui.R
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRewriteEditorSheetContent(
    sourceLog: NetworkLog?,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var urlPattern by remember(sourceLog) {
        mutableStateOf(sourceLog?.url?.let(::urlPatternFromLog).orEmpty())
    }
    var method by remember(sourceLog) { mutableStateOf(sourceLog?.method ?: "ANY") }
    var replacementUrl by remember(sourceLog) { mutableStateOf("") }
    var headerOverrides by remember(sourceLog) {
        mutableStateOf(sourceLog?.requestHeaders?.let(::formatHeaderLines).orEmpty())
    }
    var headersToRemove by remember(sourceLog) { mutableStateOf("") }
    var body by remember(sourceLog) {
        mutableStateOf(
            sourceLog?.requestBody?.let { ProwlLogFormatter.prettyBodyText(it) }.orEmpty(),
        )
    }
    var replaceBody by remember(sourceLog) { mutableStateOf(sourceLog?.requestBody != null) }
    var contentType by remember(sourceLog) {
        mutableStateOf(sourceLog?.requestBody?.contentType ?: "application/json")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.prowl_create_request_rewrite),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.prowl_create_request_rewrite_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = urlPattern,
            onValueChange = { urlPattern = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prowl_rewrite_match_pattern)) },
            placeholder = { Text("/api/users") },
            singleLine = true,
        )
        OutlinedTextField(
            value = method,
            onValueChange = { method = it.uppercase() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prowl_rewrite_match_method)) },
            placeholder = { Text("POST or ANY") },
            singleLine = true,
        )
        OutlinedTextField(
            value = replacementUrl,
            onValueChange = { replacementUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prowl_rewrite_new_url)) },
            placeholder = { Text("https://staging.api.com/v2/users or /v2/users") },
            singleLine = true,
        )
        OutlinedTextField(
            value = headerOverrides,
            onValueChange = { headerOverrides = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prowl_rewrite_headers)) },
            placeholder = { Text("Authorization: Bearer token\nX-Debug: true") },
            minLines = 3,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
        )
        OutlinedTextField(
            value = headersToRemove,
            onValueChange = { headersToRemove = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.prowl_rewrite_remove_headers)) },
            placeholder = { Text("Cookie, X-Old-Header") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = replaceBody,
                onClick = { replaceBody = !replaceBody },
                label = { Text(stringResource(R.string.prowl_rewrite_replace_body)) },
            )
        }
        if (replaceBody) {
            OutlinedTextField(
                value = contentType,
                onValueChange = { contentType = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.prowl_rewrite_content_type)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.prowl_rewrite_body)) },
                minLines = 6,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
            )
        }
        Button(
            onClick = {
                val rule = ProwlRequestRewriteRule(
                    targetUrlPattern = urlPattern.trim(),
                    targetMethod = method.trim().ifBlank { "ANY" },
                    replacementUrl = replacementUrl.trim(),
                    headerOverrides = parseHeaderLines(headerOverrides),
                    headersToRemove = headersToRemove.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .toSet(),
                    replacementBody = if (replaceBody) body.toByteArray(Charsets.UTF_8) else null,
                    replacementContentType = if (replaceBody) contentType.trim().ifBlank { null } else null,
                )
                ProwlRuntime.requestRewriter.addRule(rule)
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = urlPattern.isNotBlank(),
        ) {
            Text(stringResource(R.string.prowl_save_rewrite_rule))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRewriteEditorScreen(
    sourceLogId: UUID?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val logs by ProwlRuntime.storage.logsFlow.collectAsState()
    val sourceLog = sourceLogId?.let { id -> logs.firstOrNull { it.id == id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prowl_request_rewrite_editor)) },
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
        RequestRewriteEditorSheetContent(
            sourceLog = sourceLog,
            onSaved = onSaved,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        )
    }
}

internal fun urlPatternFromLog(url: String): String = runCatching {
    val path = java.net.URI(url).path
    path.ifEmpty { "/" }
}.getOrElse { url }

internal fun formatHeaderLines(headers: Map<String, String>): String =
    headers.entries.joinToString("\n") { (key, value) -> "$key: $value" }

internal fun parseHeaderLines(text: String): Map<String, String> = buildMap {
    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .forEach { line ->
            val index = line.indexOf(':')
            if (index > 0) {
                put(line.substring(0, index).trim(), line.substring(index + 1).trim())
            }
        }
}
