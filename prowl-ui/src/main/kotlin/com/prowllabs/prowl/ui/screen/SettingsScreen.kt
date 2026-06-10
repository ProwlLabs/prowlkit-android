package com.prowllabs.prowl.ui.screen

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.core.formatting.ProwlExportFormat
import com.prowllabs.prowl.core.formatting.ProwlLogFormatter
import com.prowllabs.prowl.core.mocking.ProwlMockExporter
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.components.ProwlFooterCredit
import com.prowllabs.prowl.ui.components.ProwlStatsSection
import com.prowllabs.prowl.ui.components.computeRequestStats
import com.prowllabs.prowl.ui.util.ProwlEnvironment
import com.prowllabs.prowl.ui.util.ProwlFloatingBubble
import com.prowllabs.prowl.ui.util.ProwlThemeMode
import com.prowllabs.prowl.ui.util.ProwlUiPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenMocks: () -> Unit,
    onThemeChanged: (ProwlThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    val env = remember { ProwlEnvironment.collect(context) }
    val logs by ProwlRuntime.storage.logsFlow.collectAsState()
    val stats = remember(logs) { computeRequestStats(logs) }
    val overviewSummary = if (stats.total == 0) {
        stringResource(R.string.prowl_stats_no_traffic)
    } else {
        stringResource(
            R.string.prowl_stats_overview_summary,
            stats.total,
            stats.successRatePercent,
            stats.avgDurationMs,
        )
    }
    var loggingEnabled by remember { mutableStateOf(ProwlRuntime.isLoggingEnabled) }
    var maskingEnabled by remember { mutableStateOf(ProwlRuntime.isSensitiveDataMaskingEnabled) }
    var persistSessions by remember { mutableStateOf(ProwlUiPreferences.isSessionPersistenceEnabled(context)) }
    var floatingBubble by remember { mutableStateOf(ProwlUiPreferences.isFloatingBubbleEnabled(context)) }
    var shakeToClear by remember { mutableStateOf(ProwlUiPreferences.isShakeToClearEnabled(context)) }
    var shakeToOpen by remember { mutableStateOf(ProwlUiPreferences.isShakeToOpenEnabled(context)) }
    var themeMode by remember { mutableStateOf(ProwlUiPreferences.themeMode(context)) }
    var showMockImport by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.prowl_settings),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.prowl_back),
                        )
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CollapsibleSettingsGroup(
                title = stringResource(R.string.prowl_section_statistics),
                collapsedSummary = overviewSummary,
                defaultExpanded = false,
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    ProwlStatsSection(
                        logs = logs,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            SettingsGroup(
                title = stringResource(R.string.prowl_section_capture),
                footer = stringResource(R.string.prowl_capture_footer),
            ) {
                SettingsCard {
                    SettingsSwitchItem(
                        title = stringResource(R.string.prowl_request_logging),
                        subtitle = stringResource(R.string.prowl_logging_sub),
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        checked = loggingEnabled,
                        onCheckedChange = {
                            loggingEnabled = it
                            ProwlRuntime.isLoggingEnabled = it
                        },
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.prowl_persist_sessions),
                        subtitle = stringResource(R.string.prowl_persist_sessions_sub),
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        checked = persistSessions,
                        onCheckedChange = { enabled ->
                            persistSessions = enabled
                            ProwlUiPreferences.setSessionPersistenceEnabled(context, enabled)
                        },
                        showDivider = false,
                    )
                }
            }

            SettingsGroup(
                title = stringResource(R.string.prowl_section_privacy),
                footer = stringResource(R.string.prowl_masking_sub),
            ) {
                SettingsCard {
                    SettingsSwitchItem(
                        title = stringResource(R.string.prowl_masking),
                        subtitle = stringResource(R.string.prowl_masking_toggle_sub),
                        icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        checked = maskingEnabled,
                        onCheckedChange = {
                            maskingEnabled = it
                            ProwlRuntime.isSensitiveDataMaskingEnabled = it
                        },
                        showDivider = false,
                    )
                }
            }

            SettingsGroup(
                title = stringResource(R.string.prowl_section_inspector),
                footer = stringResource(R.string.prowl_inspector_footer),
            ) {
                SettingsCard {
                    SettingsSwitchItem(
                        title = stringResource(R.string.prowl_floating_bubble),
                        subtitle = stringResource(R.string.prowl_floating_bubble_sub),
                        icon = { Icon(Icons.Filled.AddCircle, contentDescription = null) },
                        checked = floatingBubble,
                        onCheckedChange = { enabled ->
                            floatingBubble = enabled
                            ProwlUiPreferences.setFloatingBubbleEnabled(context, enabled)
                            (context as? android.app.Activity)?.let { ProwlFloatingBubble.refresh(it) }
                        },
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.prowl_shake_open),
                        subtitle = stringResource(R.string.prowl_shake_open_sub),
                        icon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        checked = shakeToOpen,
                        onCheckedChange = { enabled ->
                            shakeToOpen = enabled
                            ProwlUiPreferences.setShakeToOpenEnabled(context, enabled)
                        },
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.prowl_shake_clear),
                        subtitle = stringResource(R.string.prowl_shake_clear_sub),
                        icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                        checked = shakeToClear,
                        onCheckedChange = { enabled ->
                            shakeToClear = enabled
                            ProwlUiPreferences.setShakeToClearEnabled(context, enabled)
                        },
                        showDivider = false,
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.prowl_section_appearance)) {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.prowl_theme)) },
                        leadingContent = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        supportingContent = {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            ) {
                                ProwlThemeMode.entries.forEachIndexed { index, mode ->
                                    val label = when (mode) {
                                        ProwlThemeMode.SYSTEM -> stringResource(R.string.prowl_theme_system)
                                        ProwlThemeMode.LIGHT -> stringResource(R.string.prowl_theme_light)
                                        ProwlThemeMode.DARK -> stringResource(R.string.prowl_theme_dark)
                                    }
                                    SegmentedButton(
                                        selected = themeMode == mode,
                                        onClick = {
                                            themeMode = mode
                                            ProwlUiPreferences.setThemeMode(context, mode)
                                            onThemeChanged(mode)
                                        },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ProwlThemeMode.entries.size,
                                        ),
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        },
                    )
                }
            }

            SettingsGroup(
                title = stringResource(R.string.prowl_section_mocks),
                footer = stringResource(R.string.prowl_mocks_footer),
            ) {
                SettingsCard {
                    SettingsNavigationItem(
                        title = stringResource(R.string.prowl_active_mocks),
                        subtitle = stringResource(
                            R.string.prowl_active_mocks_sub,
                            ProwlRuntime.mocker.allRules().size,
                            ProwlRuntime.requestRewriter.allRules().size,
                        ),
                        icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                        onClick = onOpenMocks,
                    )
                    SettingsNavigationItem(
                        title = stringResource(R.string.prowl_export_mocks),
                        subtitle = stringResource(R.string.prowl_export_mocks_sub),
                        icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        onClick = {
                            shareExport(
                                context,
                                ProwlMockExporter.exportRules(ProwlRuntime.mocker.allRules()),
                                "prowl-mocks.json",
                            )
                        },
                    )
                    SettingsNavigationItem(
                        title = stringResource(R.string.prowl_import_mocks),
                        subtitle = stringResource(R.string.prowl_import_mocks_sub),
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        onClick = { showMockImport = true },
                        showDivider = false,
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.prowl_section_export)) {
                SettingsCard {
                    SettingsNavigationItem(
                        title = stringResource(R.string.prowl_export_formatted),
                        subtitle = stringResource(R.string.prowl_export_formatted_sub),
                        icon = { Icon(Icons.Filled.List, contentDescription = null) },
                        onClick = {
                            shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.FORMATTED_TEXT))
                        },
                    )
                    SettingsNavigationItem(
                        title = stringResource(R.string.prowl_export_curl),
                        subtitle = stringResource(R.string.prowl_export_curl_sub),
                        icon = { Icon(Icons.Filled.Build, contentDescription = null) },
                        onClick = {
                            shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.CURL_COMMANDS))
                        },
                    )
                    SettingsNavigationItem(
                        title = stringResource(R.string.prowl_export_har),
                        subtitle = stringResource(R.string.prowl_export_har_sub),
                        icon = { Icon(Icons.Filled.Send, contentDescription = null) },
                        onClick = {
                            shareExport(context, ProwlLogFormatter.export(logs, ProwlExportFormat.HAR), "prowl.har")
                        },
                        showDivider = false,
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.prowl_section_environment)) {
                SettingsCard {
                    EnvRow(stringResource(R.string.prowl_env_app_name), env.appName)
                    EnvRow(stringResource(R.string.prowl_env_app_version), env.appVersion)
                    EnvRow(stringResource(R.string.prowl_env_min_os), env.minimumOs)
                    EnvRow(stringResource(R.string.prowl_env_os_version), env.osVersion)
                    EnvRow(
                        stringResource(R.string.prowl_env_screen),
                        env.screenSize,
                        showDivider = false,
                    )
                }
            }

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
private fun CollapsibleSettingsGroup(
    title: String,
    collapsedSummary: String,
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val toggleLabel = stringResource(
        if (expanded) R.string.prowl_stats_collapse else R.string.prowl_stats_expand,
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .semantics { contentDescription = toggleLabel }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!expanded) {
                    Text(
                        text = collapsedSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = toggleLabel,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        content()
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
    )
    if (showDivider) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = icon,
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
    if (showDivider) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

@Composable
private fun EnvRow(label: String, value: String, showDivider: Boolean = true) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
    if (showDivider) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
    }
}

private fun shareExport(context: android.content.Context, text: String, fileName: String = "prowl-export.txt") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.prowl_export_via))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Composable
private fun MockImportDialog(onDismiss: () -> Unit, onImport: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prowl_import_mocks)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                placeholder = { Text(stringResource(R.string.prowl_import_mocks_hint)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.prowl_import)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.prowl_cancel))
            }
        },
    )
}
