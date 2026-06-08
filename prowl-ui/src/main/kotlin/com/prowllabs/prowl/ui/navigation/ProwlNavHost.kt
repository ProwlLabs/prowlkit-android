package com.prowllabs.prowl.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prowllabs.prowl.ui.screen.InspectorScreen
import com.prowllabs.prowl.ui.screen.LogDetailScreen
import com.prowllabs.prowl.ui.screen.MockEditorScreen
import com.prowllabs.prowl.ui.screen.MocksScreen
import com.prowllabs.prowl.ui.screen.SettingsScreen
import java.util.UUID

object ProwlRoutes {
    const val INSPECTOR = "inspector"
    const val DETAIL = "detail/{logId}"
    const val SETTINGS = "settings"
    const val MOCKS = "mocks"
    const val MOCK_EDITOR = "mock_editor?logId={logId}"

    fun detail(logId: UUID) = "detail/$logId"
    fun mockEditor(logId: UUID?) =
        if (logId != null) "mock_editor?logId=$logId" else "mock_editor"
}

@Composable
fun ProwlNavHost(onClose: () -> Unit) {
    val navController = rememberNavController()

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
            val logId = UUID.fromString(entry.arguments?.getString("logId"))
            LogDetailScreen(
                logId = logId,
                onBack = { navController.popBackStack() },
                onCreateMock = { navController.navigate(ProwlRoutes.mockEditor(logId)) },
            )
        }
        composable(ProwlRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenMocks = { navController.navigate(ProwlRoutes.MOCKS) },
            )
        }
        composable(ProwlRoutes.MOCKS) {
            MocksScreen(
                onBack = { navController.popBackStack() },
                onCreateMock = { navController.navigate(ProwlRoutes.mockEditor(null)) },
            )
        }
        composable(
            route = ProwlRoutes.MOCK_EDITOR,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val logId = entry.arguments?.getString("logId")?.let(UUID::fromString)
            MockEditorScreen(
                sourceLogId = logId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
