package com.edu.gymledger.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.edu.gymledger.app.AppContainer
import com.edu.gymledger.domain.model.Exercise
import com.edu.gymledger.feature.body.BodyScreen
import com.edu.gymledger.feature.dashboard.DashboardScreen
import com.edu.gymledger.feature.exercises.ExerciseFormScreen
import com.edu.gymledger.feature.exercises.ExercisesScreen
import com.edu.gymledger.feature.nutrition.FoodsScreen
import com.edu.gymledger.feature.nutrition.MealDetailScreen
import com.edu.gymledger.feature.nutrition.NutritionScreen
import com.edu.gymledger.feature.routines.RoutineDetailScreen
import com.edu.gymledger.feature.routines.RoutinesScreen
import com.edu.gymledger.feature.settings.SettingsScreen
import com.edu.gymledger.feature.workouts.WorkoutDetailScreen
import com.edu.gymledger.feature.workouts.WorkoutsScreen
import kotlinx.coroutines.launch

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
            WorkoutsScreen(
                onNavigateToExercises = { navController.navigate(NavigationRoute.Exercises.route) },
                onNavigateToWorkoutDetail = { workoutId ->
                    navController.navigate("${NavigationRoute.WorkoutDetail.route}/$workoutId")
                },
                onNavigateToRoutines = { navController.navigate(NavigationRoute.Routines.route) }
            )
        }
        composable("${NavigationRoute.WorkoutDetail.route}/{workoutId}") { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getString("workoutId")?.toLongOrNull()
            WorkoutDetailScreen(
                sessionId = workoutId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Exercises
        composable(NavigationRoute.Exercises.route) {
            ExercisesScreen(
                onNavigateToForm = { exerciseId ->
                    if (exerciseId == null) {
                        navController.navigate(NavigationRoute.ExerciseAdd.route)
                    } else {
                        navController.navigate(NavigationRoute.exerciseEditRoute(exerciseId))
                    }
                }
            )
        }
        composable(NavigationRoute.ExerciseAdd.route) {
            val scope = rememberCoroutineScope()
            ExerciseFormScreen(
                exercise = null,
                onSave = { name, category, primaryMuscle, secondaryMuscles, equipment, notes ->
                    val repository = AppContainer.exerciseRepository
                    scope.launch {
                        repository.create(
                            name = name,
                            category = category,
                            primaryMuscle = primaryMuscle,
                            secondaryMuscles = secondaryMuscles,
                            equipment = equipment,
                            notes = notes
                        )
                    }
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(NavigationRoute.ExerciseEdit.route) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId")?.toLongOrNull()
            var exercise by remember { mutableStateOf<Exercise?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(exerciseId) {
                exerciseId?.let {
                    exercise = AppContainer.exerciseRepository.getById(it)
                }
            }

            ExerciseFormScreen(
                exercise = exercise,
                onSave = { name, category, primaryMuscle, secondaryMuscles, equipment, notes ->
                    exercise?.let { existing ->
                        val repository = AppContainer.exerciseRepository
                        scope.launch {
                            repository.update(
                                existing.copy(
                                    name = name,
                                    category = category,
                                    primaryMuscle = primaryMuscle,
                                    secondaryMuscles = secondaryMuscles,
                                    equipment = equipment,
                                    notes = notes
                                )
                            )
                        }
                    }
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
                onDelete = {
                    exercise?.let { ex ->
                        scope.launch {
                            AppContainer.exerciseRepository.delete(ex)
                        }
                    }
                    navController.popBackStack()
                }
            )
        }

        // Routines
        composable(NavigationRoute.Routines.route) {
            RoutinesScreen(
                onNavigateToRoutineDetail = { routineId ->
                    navController.navigate(NavigationRoute.routineDetailRoute(routineId))
                }
            )
        }
        composable("${NavigationRoute.RoutineDetail.route}/{routineId}") { backStackEntry ->
            val routineId = backStackEntry.arguments?.getString("routineId")?.toLongOrNull()
            RoutineDetailScreen(
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToWorkoutDetail = { sessionId ->
                    navController.navigate(NavigationRoute.workoutDetailRoute(sessionId))
                }
            )
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