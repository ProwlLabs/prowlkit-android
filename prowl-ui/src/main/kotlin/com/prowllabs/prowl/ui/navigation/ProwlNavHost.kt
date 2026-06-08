package com.prowllabs.prowl.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prowllabs.prowl.core.runtime.ProwlRuntime
import com.prowllabs.prowl.ui.screen.InspectorScreen
import com.prowllabs.prowl.ui.screen.LogDetailScreen
import com.prowllabs.prowl.ui.screen.MockEditorScreen
import com.prowllabs.prowl.ui.screen.MocksScreen
import com.prowllabs.prowl.ui.screen.RequestRewriteEditorScreen
import com.prowllabs.prowl.ui.screen.SettingsScreen
import com.prowllabs.prowl.ui.util.ProwlShakeDetector
import com.prowllabs.prowl.ui.util.ProwlThemeMode
import com.prowllabs.prowl.ui.util.ProwlUiPreferences
import java.util.UUID

object ProwlRoutes {
    const val INSPECTOR = "inspector"
    const val DETAIL = "detail/{logId}"
    const val SETTINGS = "settings"
    const val MOCKS = "mocks"
    const val MOCK_EDITOR = "mock_editor?logId={logId}&ruleId={ruleId}"
    const val REQUEST_REWRITE_EDITOR = "request_rewrite_editor?logId={logId}"

    fun detail(logId: UUID) = "detail/$logId"
    fun mockEditor(logId: UUID? = null, ruleId: UUID? = null): String {
        val logPart = logId?.toString().orEmpty()
        val rulePart = ruleId?.toString().orEmpty()
        return "mock_editor?logId=$logPart&ruleId=$rulePart"
    }
    fun requestRewriteEditor(logId: UUID?) =
        if (logId != null) "request_rewrite_editor?logId=$logId" else "request_rewrite_editor"
}

@Composable
fun ProwlNavHost(
    onClose: () -> Unit,
    onThemeChanged: (ProwlThemeMode) -> Unit = {},
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var shakeToClearEnabled by remember {
        mutableStateOf(ProwlUiPreferences.isShakeToClearEnabled(context))
    }

    ProwlShakeDetector(enabled = shakeToClearEnabled) {
        ProwlRuntime.storage.clearBlocking()
        ProwlRuntime.onLogsCleared()
    }

    NavHost(navController = navController, startDestination = ProwlRoutes.INSPECTOR) {
        composable(ProwlRoutes.INSPECTOR) {
            InspectorScreen(
                onClose = onClose,
                onOpenSettings = { navController.navigate(ProwlRoutes.SETTINGS) },
                onOpenLog = { logId -> navController.navigate(ProwlRoutes.detail(logId)) },
            )
        }
        composable(
            route = ProwlRoutes.DETAIL,
            arguments = listOf(navArgument("logId") { type = NavType.StringType }),
        ) { entry ->
            val logId = parseUuidOrNull(entry.arguments?.getString("logId"))
            if (logId == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                LogDetailScreen(
                    logId = logId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(ProwlRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenMocks = { navController.navigate(ProwlRoutes.MOCKS) },
                onShakePrefChanged = { enabled -> shakeToClearEnabled = enabled },
                onThemeChanged = onThemeChanged,
            )
        }
        composable(ProwlRoutes.MOCKS) {
            MocksScreen(
                onBack = { navController.popBackStack() },
                onCreateMock = { navController.navigate(ProwlRoutes.mockEditor()) },
                onEditMock = { ruleId -> navController.navigate(ProwlRoutes.mockEditor(ruleId = ruleId)) },
                onCreateRequestRewrite = { navController.navigate(ProwlRoutes.requestRewriteEditor(null)) },
            )
        }
        composable(
            route = ProwlRoutes.MOCK_EDITOR,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
                navArgument("ruleId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val logId = parseUuidOrNull(entry.arguments?.getString("logId")?.takeIf { it.isNotBlank() })
            val ruleId = parseUuidOrNull(entry.arguments?.getString("ruleId")?.takeIf { it.isNotBlank() })
            MockEditorScreen(
                sourceLogId = logId,
                ruleId = ruleId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = ProwlRoutes.REQUEST_REWRITE_EDITOR,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val logId = parseUuidOrNull(entry.arguments?.getString("logId"))
            RequestRewriteEditorScreen(
                sourceLogId = logId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}

private fun parseUuidOrNull(value: String?): UUID? =
    value?.let { runCatching { UUID.fromString(it) }.getOrNull() }
