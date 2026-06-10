package com.prowllabs.prowl.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.prowllabs.prowl.ui.R
import com.prowllabs.prowl.ui.components.ProwlBrandIconView
import com.prowllabs.prowl.ui.components.ProwlFooterCredit
import com.prowllabs.prowl.ui.components.ProwlLogRow
import com.prowllabs.prowl.ui.util.ProwlWatchStore
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
    val context = LocalContext.current
    val filtered = viewModel.filterLogs(context, logs)
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.prowl_clear_dialog_title)) },
            text = { Text(stringResource(R.string.prowl_clear_dialog_message, logs.size)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearLogs()
                    showClearDialog = false
                }) {
                    Text(stringResource(R.string.prowl_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.prowl_cancel))
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    ProwlBrandIconView(height = 16.dp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.prowl_close))
                    }
                },
                actions = {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.prowl_clear_logs))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.prowl_settings))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SearchField(
                value = viewModel.searchText,
                onValueChange = viewModel::updateSearchText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (logs.isEmpty()) {
                            stringResource(R.string.prowl_empty_logs)
                        } else {
                            stringResource(R.string.prowl_no_matches)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    items(filtered, key = { it.id }) { log ->
                        ProwlLogRow(
                            log = log,
                            isWatched = ProwlWatchStore.isWatched(context, log),
                            onClick = { onOpenLog(log.id) },
                        )
                    }
                    item { ProwlFooterCredit(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        placeholder = { Text(stringResource(R.string.prowl_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}
