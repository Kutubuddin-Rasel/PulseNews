package com.example.newsapp.Navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.newsapp.Routes
import com.example.newsapp.Screen.AlgorithmSettingsScreen
import com.example.newsapp.Screen.ArticleDetailScreen
import com.example.newsapp.Screen.HomeScreen
import com.example.newsapp.Screen.NotificationPreferencesScreen
import com.example.newsapp.Screen.PulseProfileScreen
import com.example.newsapp.Screen.SavedArticle
import com.example.newsapp.Screen.SettingsScreen
import com.example.newsapp.Screen.WebScreen

@Composable
fun App() {
    val navController = rememberNavController()
    val navItems = listOf(
        NavigationItem(Routes.home, "Home", Icons.Outlined.Home),
        NavigationItem(Routes.saved, "Saved", Icons.Outlined.Bookmarks),
        NavigationItem(Routes.profile, "Profile", Icons.Outlined.Person),
        NavigationItem(Routes.settings, "Settings", Icons.Outlined.Settings)
    )

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(Routes.home, Routes.saved, Routes.profile, Routes.settings)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.screenName) },
                            label = { Text(item.screenName) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.home,
            modifier = Modifier.padding(bottom = if (showBottomBar) paddingValues.calculateBottomPadding() else 0.dp)
        ) {
            composable(Routes.home) { HomeScreen(navController) }
            composable(Routes.saved) { SavedArticle(navController) }
            composable(Routes.profile) { PulseProfileScreen() }
            composable(Routes.settings) {
                SettingsScreen(
                    onNavigateToNotifications = {
                        navController.navigate(Routes.notificationPreferences)
                    },
                    onNavigateToAlgorithm = {
                        navController.navigate(Routes.algorithmSettings)
                    }
                )
            }
            composable(Routes.notificationPreferences) {
                NotificationPreferencesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.algorithmPreferences) {
                com.example.newsapp.Screen.AlgorithmPreferencesScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Routes.algorithmSettings) {
                AlgorithmSettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.articleDetailPattern,
                arguments = listOf(navArgument(Routes.articleUrlArg) { type = NavType.StringType })
            ) {
                ArticleDetailScreen(navController)
            }
            composable(
                route = Routes.webPagePattern,
                arguments = listOf(navArgument(Routes.articleUrlArg) { type = NavType.StringType })
            ) {
                WebScreen(navController)
            }
        }
    }
}
