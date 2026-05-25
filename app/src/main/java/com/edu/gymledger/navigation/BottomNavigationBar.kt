package com.edu.gymledger.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.edu.gymledger.R

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem.Dashboard,
        NavigationItem.Workouts,
        NavigationItem.Nutrition,
        NavigationItem.Body,
        NavigationItem.Settings
    )

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val isSelected = currentRoute == item.route.route

            NavigationBarItem(
                icon = {
                    Icon(
                        painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = {
                    if (currentRoute != item.route.route) {
                        navController.navigate(item.route.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}

sealed class NavigationItem(
    val title: String,
    val icon: Int,
    val route: NavigationRoute
) {
    object Dashboard : NavigationItem(
        title = "Dashboard",
        icon = R.drawable.ic_dashboard,
        route = NavigationRoute.Dashboard
    )

    object Workouts : NavigationItem(
        title = "Workouts",
        icon = R.drawable.ic_workout,
        route = NavigationRoute.Workouts
    )

    object Nutrition : NavigationItem(
        title = "Nutrition",
        icon = R.drawable.ic_nutrition,
        route = NavigationRoute.Nutrition
    )

    object Body : NavigationItem(
        title = "Body",
        icon = R.drawable.ic_body,
        route = NavigationRoute.Body
    )

    object Settings : NavigationItem(
        title = "Settings",
        icon = R.drawable.ic_settings,
        route = NavigationRoute.Settings
    )
}