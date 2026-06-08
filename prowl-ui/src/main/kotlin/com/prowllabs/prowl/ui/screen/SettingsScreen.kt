package com.prowllabs.prowl.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prowllabs.prowl.core.formatting.ProwlExportFormat
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.components.ProwlFooterCredit
import com.prowllabs.prowl.ui.components.ProwlStatsSection
import com.prowllabs.prowl.core.mocking.ProwlMockExporter
import com.prowllabs.prowl.ui.util.ProwlFloatingBubble
import com.prowllabs.prowl.ui.util.ProwlEnvironment
import com.prowllabs.prowl.ui.util.ProwlThemeMode
import com.prowllabs.prowl.ui.util.ProwlUiPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMocks: () -> Unit,
    onShakePrefChanged: (Boolean) -> Unit = {},
    onThemeChanged: (ProwlThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    val env = remember { ProwlEnvironment.collect(context) }
    val logs by ProwlRuntime.storage.logsFlow.collectAsState()
    var loggingEnabled by remember { mutableStateOf(ProwlRuntime.isLoggingEnabled) }
    var maskingEnabled by remember { mutableStateOf(ProwlRuntime.isSensitiveDataMaskingEnabled) }
    var shakeToClear by remember {
        mutableStateOf(ProwlUiPreferences.isShakeToClearEnabled(context))
    }
    var persistSessions by remember {
        mutableStateOf(ProwlUiPreferences.isSessionPersistenceEnabled(context))
    }
    var floatingBubble by remember {
        mutableStateOf(ProwlUiPreferences.isFloatingBubbleEnabled(context))
    }
    var themeMode by remember { mutableStateOf(ProwlUiPreferences.themeMode(context)) }
    var showMockImport by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prowl_settings), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.prowl_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionTitle(stringResource(R.string.prowl_section_statistics))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
            ) {
                ProwlStatsSection(logs = logs)
            }

            SectionTitle(stringResource(R.string.prowl_section_capture))
            SettingToggle(
                title = stringResource(R.string.prowl_logging),
                subtitle = stringResource(R.string.prowl_logging_sub),
                checked = loggingEnabled,
                onCheckedChange = {
                    loggingEnabled = it
                    ProwlRuntime.isLoggingEnabled = it
                },
            )
            SettingToggle(
                title = stringResource(R.string.prowl_masking),
                subtitle = stringResource(R.string.prowl_masking_sub),
                checked = maskingEnabled,
                onCheckedChange = {
                    maskingEnabled = it
                    ProwlRuntime.isSensitiveDataMaskingEnabled = it
                },
            )
            SettingToggle(
                title = stringResource(R.string.prowl_persist_sessions),
                subtitle = stringResource(R.string.prowl_persist_sessions_sub),
                checked = persistSessions,
                onCheckedChange = { enabled ->
                    persistSessions = enabled
                    ProwlUiPreferences.setSessionPersistenceEnabled(context, enabled)
                },
            )
            SettingToggle(
                title = stringResource(R.string.prowl_floating_bubble),
                subtitle = stringResource(R.string.prowl_floating_bubble_sub),
                checked = floatingBubble,
                onCheckedChange = { enabled ->
                    floatingBubble = enabled
                    ProwlUiPreferences.setFloatingBubbleEnabled(context, enabled)
                    (context as? android.app.Activity)?.let { ProwlFloatingBubble.refresh(it) }
                },
            )
            SettingToggle(
                title = stringResource(R.string.prowl_shake_clear),
                subtitle = stringResource(R.string.prowl_shake_clear_sub),
                checked = shakeToClear,
                onCheckedChange = { enabled ->
                    shakeToClear = enabled
                    ProwlUiPreferences.setShakeToClearEnabled(context, enabled)
                    onShakePrefChanged(enabled)
                },
            )
            var shakeToOpen by remember {
                mutableStateOf(ProwlUiPreferences.isShakeToOpenEnabled(context))
            }
            SettingToggle(
                title = stringResource(R.string.prowl_shake_open),
                subtitle = stringResource(R.string.prowl_shake_open_sub),
                checked = shakeToOpen,
                onCheckedChange = { enabled ->
                    shakeToOpen = enabled
                    ProwlUiPreferences.setShakeToOpenEnabled(context, enabled)
                },
            )

            SectionTitle(stringResource(R.string.prowl_section_appearance))
            ThemePicker(
                selected = themeMode,
                onSelect = { mode ->
                    themeMode = mode
                    ProwlUiPreferences.setThemeMode(context, mode)
                    onThemeChanged(mode)
                },
            )

            SectionTitle(stringResource(R.string.prowl_section_mocks))
            ActionRow(
                title = stringResource(R.string.prowl_active_mocks),
                subtitle = stringResource(
                    R.string.prowl_active_mocks_sub,
                    ProwlRuntime.mocker.allRules().size,
                    ProwlRuntime.requestRewriter.allRules().size,
                ),
                onClick = onOpenMocks,
            )

            SectionTitle(stringResource(R.string.prowl_section_export))
            ActionRow(
                icon = { Icon(Icons.AutoMirrored.Outlined.Article, null) },
                title = stringResource(R.string.prowl_export_formatted),
                subtitle = stringResource(R.string.prowl_export_formatted_sub),
                onClick = {
                    shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.FORMATTED_TEXT))
                },
            )
            ActionRow(
                icon = { Icon(Icons.Outlined.Terminal, null) },
                title = stringResource(R.string.prowl_export_curl),
                subtitle = stringResource(R.string.prowl_export_curl_sub),
                onClick = {
                    shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.CURL_COMMANDS))
                },
            )
            ActionRow(
                title = stringResource(R.string.prowl_export_har),
                subtitle = stringResource(R.string.prowl_export_har_sub),
                onClick = {
                    shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.HAR), "prowl.har")
                },
            )
            ActionRow(
                title = stringResource(R.string.prowl_export_mocks),
                subtitle = stringResource(R.string.prowl_export_mocks_sub),
                onClick = {
                    shareExport(
                        context,
                        ProwlMockExporter.exportRules(ProwlRuntime.mocker.allRules()),
                        "prowl-mocks.json",
                    )
                },
            )
            ActionRow(
                title = stringResource(R.string.prowl_import_mocks),
                subtitle = stringResource(R.string.prowl_import_mocks_sub),
                onClick = { showMockImport = true },
            )

            SectionTitle(stringResource(R.string.prowl_section_environment))
            EnvInfoCard(
                rows = listOf(
                    stringResource(R.string.prowl_env_app_name) to env.appName,
                    stringResource(R.string.prowl_env_app_version) to env.appVersion,
                    stringResource(R.string.prowl_env_min_os) to env.minimumOs,
                    stringResource(R.string.prowl_env_os_version) to env.osVersion,
                    stringResource(R.string.prowl_env_screen) to env.screenSize,
                ),
            )

            ProwlFooterCredit()
        }
    }

    if (showMockImport) {
        MockImportDialog(
            onDismiss = { showMockImport = false },
            onImport = { json ->
                ProwlMockExporter.importRules(json).forEach { ProwlRuntime.mocker.addRule(it) }
                showMockImport = false
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EnvInfoCard(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEachIndexed { index, (label, value) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    modifier = Modifier.weight(0.35f),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(0.65f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (index < rows.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            }
        }
    }
}

private fun shareExport(context: android.content.Context, text: String, fileName: String = "prowl-export.txt") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.prowl_export_via)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun ThemePicker(selected: ProwlThemeMode, onSelect: (ProwlThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ProwlThemeMode.entries.forEach { mode ->
            val label = when (mode) {
                ProwlThemeMode.SYSTEM -> stringResource(R.string.prowl_theme_system)
                ProwlThemeMode.LIGHT -> stringResource(R.string.prowl_theme_light)
                ProwlThemeMode.DARK -> stringResource(R.string.prowl_theme_dark)
            }
            Text(
                text = label,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (mode == selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.background,
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = if (mode == selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (mode == selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MockImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prowl_import_mocks)) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                placeholder = { Text(stringResource(R.string.prowl_import_mocks_hint)) },
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onImport(text) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.prowl_import)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.prowl_cancel))
            }
        },
    )
}
