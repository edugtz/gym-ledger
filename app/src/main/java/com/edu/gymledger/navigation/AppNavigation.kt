package com.edu.gymledger.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edu.gymledger.feature.body.BodyScreen
import com.edu.gymledger.feature.dashboard.DashboardScreen
import com.edu.gymledger.feature.exercises.ExercisesScreen
import com.edu.gymledger.feature.nutrition.FoodsScreen
import com.edu.gymledger.feature.nutrition.MealDetailScreen
import com.edu.gymledger.feature.nutrition.NutritionScreen
import com.edu.gymledger.feature.routines.RoutineDetailScreen
import com.edu.gymledger.feature.routines.RoutinesScreen
import com.edu.gymledger.feature.settings.SettingsScreen
import com.edu.gymledger.feature.workouts.WorkoutDetailScreen
import com.edu.gymledger.feature.workouts.WorkoutsScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoute.Dashboard.route
    ) {
        // Dashboard
        composable(NavigationRoute.Dashboard.route) {
            DashboardScreen()
        }

        // Workouts
        composable(NavigationRoute.Workouts.route) {
            WorkoutsScreen()
        }
        composable("${NavigationRoute.WorkoutDetail.route}/{workoutId}") { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: ""
            WorkoutDetailScreen()
        }

        // Exercises
        composable(NavigationRoute.Exercises.route) {
            ExercisesScreen()
        }

        // Routines
        composable(NavigationRoute.Routines.route) {
            RoutinesScreen()
        }
        composable("${NavigationRoute.RoutineDetail.route}/{routineId}") { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId") ?: ""
            RoutineDetailScreen()
        }

        // Nutrition
        composable(NavigationRoute.Nutrition.route) {
            NutritionScreen()
        }
        composable(NavigationRoute.Foods.route) {
            FoodsScreen()
        }
        composable("${NavigationRoute.MealDetail.route}/{mealId}") { backStackEntry ->
            val mealId = backStackEntry.arguments?.getString("mealId") ?: ""
            MealDetailScreen()
        }

        // Body
        composable(NavigationRoute.Body.route) {
            BodyScreen()
        }

        // Settings
        composable(NavigationRoute.Settings.route) {
            SettingsScreen()
        }
    }
}