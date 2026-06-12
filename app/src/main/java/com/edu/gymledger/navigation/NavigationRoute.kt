package com.edu.gymledger.navigation

sealed class NavigationRoute(val route: String) {
    object Dashboard : NavigationRoute("dashboard")
    object Workouts : NavigationRoute("workouts")
    object WorkoutDetail : NavigationRoute("workout_detail")
    object Exercises : NavigationRoute("exercises")
    object ExerciseAdd : NavigationRoute("exercise_add")
    object ExerciseEdit : NavigationRoute("exercise_edit/{exerciseId}")
    object Routines : NavigationRoute("routines")
    object RoutineDetail : NavigationRoute("routine_detail")
    object Nutrition : NavigationRoute("nutrition")
    object Foods : NavigationRoute("foods")
    object MealDetail : NavigationRoute("meal_detail")
    object Body : NavigationRoute("body")
    object Settings : NavigationRoute("settings")

    companion object {
        fun exerciseEditRoute(exerciseId: Long): String =
            "exercise_edit/$exerciseId"

        fun routineDetailRoute(routineId: Long): String =
            "routine_detail/$routineId"
    }
}