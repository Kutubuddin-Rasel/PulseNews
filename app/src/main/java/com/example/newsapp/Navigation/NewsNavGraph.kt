package com.example.newsapp.Navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.newsapp.Routes
import com.example.newsapp.Screen.AlgorithmSettingsScreen
import com.example.newsapp.Screen.ArticleDetailScreen
import com.example.newsapp.Screen.HomeScreen
import com.example.newsapp.Screen.NotificationPreferencesScreen
import com.example.newsapp.Screen.PulseProfileScreen

import com.example.newsapp.Screen.SearchScreen
import com.example.newsapp.Screen.SettingsScreen
import com.example.newsapp.Screen.WebScreen

@Composable
fun NewsNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.home,
        modifier = modifier
    ) {
        composable(Routes.home) { com.example.newsapp.Screen.HomeRoute(navController) }
        composable(Routes.search) { com.example.newsapp.Screen.SearchRoute(navController) }
        composable(Routes.saved) { com.example.newsapp.Screen.SavedRoute(navController) }
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

        composable(Routes.algorithmSettings) {
            com.example.newsapp.Screen.AlgorithmSettingsRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.articleDetailPattern,
            arguments = listOf(navArgument(Routes.articleUrlArg) { type = NavType.StringType })
        ) {
            com.example.newsapp.Screen.ArticleDetailRoute(navController)
        }
        composable(
            route = Routes.webPagePattern,
            arguments = listOf(navArgument(Routes.articleUrlArg) { type = NavType.StringType })
        ) {
            WebScreen(navController)
        }
    }
}
