package com.vh.health.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(val route: String, val labelVi: String, val icon: ImageVector) {
    TODAY("today", "Hôm nay", Icons.Filled.WbTwilight),
    SCHEDULE("schedule", "Lịch", Icons.Filled.CalendarMonth),
    LIBRARY("library", "Động tác", Icons.Filled.FitnessCenter),
    PROGRESS("progress", "Tiến trình", Icons.Filled.ShowChart),
    SETTINGS("settings", "Cài đặt", Icons.Filled.Settings),
}
