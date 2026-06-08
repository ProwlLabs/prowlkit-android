package com.prowllabs.prowl.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import com.prowllabs.prowl.core.runtime.ProwlRuntime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MocksScreen(
    onBack: () -> Unit,
    onCreateMock: () -> Unit,
) {
    var rules by remember { mutableStateOf(ProwlRuntime.mocker.allRules()) }

    fun refresh() {
        rules = ProwlRuntime.mocker.allRules()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Mocks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        ProwlRuntime.mocker.removeAllRules()
                        refresh()
                    }) {
                        Text("Delete All")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMock) {
                Icon(Icons.Default.Add, contentDescription = "Add mock")
            }
        },
    ) { padding ->
        if (rules.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No mock rules yet.")
                Text("Create one from a captured request or tap +.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rules, key = { it.id }) { rule ->
                    MockRuleRow(
                        rule = rule,
                        onToggle = { enabled ->
                            ProwlRuntime.mocker.updateRule(rule.copy(isEnabled = enabled))
                            refresh()
                        },
                        onDelete = {
                            ProwlRuntime.mocker.removeRule(rule.id)
                            refresh()
                        },
                    )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("${rule.targetMethod} · ${rule.mockStatusCode}")
        Text(rule.targetUrlPattern)
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
            TextButton(onClick = onDelete) { Text("Delete") }
        }
    }
}
