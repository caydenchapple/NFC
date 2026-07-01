package com.zephyrcloud.nfckit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zephyrcloud.nfckit.ui.history.HistoryScreen
import com.zephyrcloud.nfckit.ui.more.MoreScreen
import com.zephyrcloud.nfckit.ui.profiles.ProfilesScreen
import com.zephyrcloud.nfckit.ui.read.ReadScreen
import com.zephyrcloud.nfckit.ui.tasks.TasksScreen
import com.zephyrcloud.nfckit.ui.write.WriteScreen

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Read : Tab("read", "Read", Icons.Filled.Nfc)
    data object Write : Tab("write", "Write", Icons.Filled.Save)
    data object Tasks : Tab("tasks", "Tasks", Icons.Filled.Task)
    data object More : Tab("more", "More", Icons.Filled.MoreHoriz)
}

private val bottomTabs = listOf(Tab.Read, Tab.Write, Tab.Tasks, Tab.More)

private object Routes {
    const val PROFILES = "profiles"
    const val HISTORY = "history"
}

@Composable
fun NfcKitNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar {
                bottomTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Read.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Read.route) { ReadScreen() }
            composable(Tab.Write.route) { WriteScreen() }
            composable(Tab.Tasks.route) { TasksScreen() }
            composable(Tab.More.route) {
                MoreScreen(
                    onOpenProfiles = { navController.navigate(Routes.PROFILES) },
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                )
            }
            composable(Routes.PROFILES) { ProfilesScreen() }
            composable(Routes.HISTORY) { HistoryScreen() }
        }
    }
}
