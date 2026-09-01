package com.vh.health.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vh.health.AppContainer
import com.vh.health.ui.library.LibraryScreen
import com.vh.health.ui.nav.Destination
import com.vh.health.ui.placeholder.PlaceholderScreen
import com.vh.health.ui.player.WorkoutPlayerScreen
import com.vh.health.ui.schedule.ScheduleScreen
import com.vh.health.ui.settings.SettingsScreen
import com.vh.health.ui.today.TodayScreen

private const val PLAYER_ROUTE = "player"

@Composable
fun VhHealthRoot(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val inPlayer = currentRoute?.startsWith(PLAYER_ROUTE) == true

    Scaffold(
        bottomBar = {
            // The workout player hides the bottom nav: nothing should pull the user
            // out of a running session by accident.
            if (!inPlayer) {
                NavigationBar {
                    Destination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(Destination.TODAY.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.labelVi) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.TODAY.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.TODAY.route) {
                TodayScreen(container, onStartWorkout = { workoutId -> navController.navigate("$PLAYER_ROUTE/$workoutId") })
            }
            composable(
                route = "$PLAYER_ROUTE/{workoutId}",
                arguments = listOf(navArgument("workoutId") { type = NavType.StringType }),
            ) { entry ->
                val workoutId = entry.arguments?.getString("workoutId")
                if (workoutId != null) {
                    WorkoutPlayerScreen(container, workoutId, onFinish = { navController.popBackStack() })
                }
            }
            composable(Destination.SCHEDULE.route) { ScheduleScreen(container) }
            composable(Destination.LIBRARY.route) { LibraryScreen(container) }
            composable(Destination.PROGRESS.route) {
                PlaceholderScreen(
                    title = "Tiến trình",
                    milestone = "M6",
                    body = "Nhật ký từng buổi, biểu đồ cân nặng và vòng eo, lịch chuỗi ngày, " +
                        "và đường tín hiệu gối để phát hiện sớm khi khối lượng vượt sức chịu tải.",
                )
            }
            composable(Destination.SETTINGS.route) { SettingsScreen(container) }
        }
    }
}
