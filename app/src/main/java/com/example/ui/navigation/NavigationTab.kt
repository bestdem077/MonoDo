package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TASKS("Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline),
    STATS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    TOOLS("Tools", Icons.Filled.Construction, Icons.Outlined.Construction),
    SETTINGS("Settings", Icons.Filled.Person, Icons.Outlined.PersonOutline)
}
