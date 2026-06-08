package com.prowllabs.prowl.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.mocking.ProwlRequestRewriteRule
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.components.ProwlMethodBadge
import com.prowllabs.prowl.ui.viewmodel.MocksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MocksScreen(
    onBack: () -> Unit,
    onCreateMock: () -> Unit,
    onCreateRequestRewrite: () -> Unit,
    viewModel: MocksViewModel = viewModel(),
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val mockRules by viewModel.mockRules.collectAsState()
    val rewriteRules by viewModel.rewriteRules.collectAsState()

    val isResponseTab = selectedTab == 0
    val rulesEmpty = if (isResponseTab) mockRules.isEmpty() else rewriteRules.isEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prowl_mocks_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!rulesEmpty) {
                        TextButton(onClick = {
                            if (isResponseTab) {
                                viewModel.deleteAllMocks()
                            } else {
                                viewModel.deleteAllRewrites()
                            }
                        }) {
                            Text(stringResource(R.string.prowl_delete_all))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isResponseTab) onCreateMock() else onCreateRequestRewrite()
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SegmentedButton(
                    selected = isResponseTab,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(stringResource(R.string.prowl_tab_response_mocks))
                }
                SegmentedButton(
                    selected = !isResponseTab,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(stringResource(R.string.prowl_tab_request_rewrites))
                }
            }

            if (rulesEmpty) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (isResponseTab) {
                            stringResource(R.string.prowl_no_response_mocks)
                        } else {
                            stringResource(R.string.prowl_no_request_rewrites)
                        },
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (isResponseTab) {
                            stringResource(R.string.prowl_no_response_mocks_hint)
                        } else {
                            stringResource(R.string.prowl_no_request_rewrites_hint)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else if (isResponseTab) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(mockRules, key = { it.id }) { rule ->
                        MockRuleRow(
                            rule = rule,
                            onToggle = { enabled -> viewModel.setMockEnabled(rule, enabled) },
                            onDelete = { viewModel.deleteMock(rule.id) },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(rewriteRules, key = { it.id }) { rule ->
                        RequestRewriteRuleRow(
                            rule = rule,
                            onToggle = { enabled -> viewModel.setRewriteEnabled(rule, enabled) },
                            onDelete = { viewModel.deleteRewrite(rule.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MockRuleRow(
    rule: ProwlMockRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProwlMethodBadge(method = rule.targetMethod)
            Text(
                text = rule.mockStatusCode.toString(),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = rule.targetUrlPattern,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
        RuleToggleRow(
            enabled = rule.isEnabled,
            onToggle = onToggle,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun RequestRewriteRuleRow(
    rule: ProwlRequestRewriteRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProwlMethodBadge(method = rule.targetMethod)
            Text(
                text = stringResource(R.string.prowl_rewrite_badge),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = rule.targetUrlPattern,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
        if (rule.replacementUrl.isNotBlank()) {
            Text(
                text = stringResource(R.string.prowl_rewrite_arrow, rule.replacementUrl),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (rule.headerOverrides.isNotEmpty()) {
            Text(
                text = stringResource(R.string.prowl_rewrite_headers_count, rule.headerOverrides.size),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RuleToggleRow(
            enabled = rule.isEnabled,
            onToggle = onToggle,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun RuleToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = enabled, onCheckedChange = onToggle)
            Text(
                text = if (enabled) {
                    stringResource(R.string.prowl_enabled)
                } else {
                    stringResource(R.string.prowl_disabled)
                },
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.prowl_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
