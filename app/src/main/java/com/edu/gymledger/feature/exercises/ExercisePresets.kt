package com.edu.gymledger.feature.exercises

data class ExercisePreset(
    val name: String,
    val category: String?,
    val primaryMuscle: String?,
    val secondaryMuscles: String?,
    val equipment: String?,
    val notes: String? = null
)

object CommonPresets {
    val list = listOf(
        ExercisePreset(
            name = "Bench Press",
            category = "Upper Body",
            primaryMuscle = "Pectoralis Major",
            secondaryMuscles = "Triceps, Shoulders",
            equipment = "Barbell"
        ),
        ExercisePreset(
            name = "Squat",
            category = "Lower Body",
            primaryMuscle = "Quadriceps",
            secondaryMuscles = "Glutes, Hamstrings",
            equipment = "Barbell"
        ),
        ExercisePreset(
            name = "Deadlift",
            category = "Full Body",
            primaryMuscle = "Erector Spinae",
            secondaryMuscles = "Hamstrings, Glutes",
            equipment = "Barbell"
        ),
        ExercisePreset(
            name = "Overhead Press",
            category = "Upper Body",
            primaryMuscle = "Deltoids",
            secondaryMuscles = "Triceps",
            equipment = "Barbell"
        ),
        ExercisePreset(
            name = "Pull-Up",
            category = "Upper Body",
            primaryMuscle = "Latissimus Dorsi",
            secondaryMuscles = "Biceps",
            equipment = "Bodyweight"
        ),
        ExercisePreset(
            name = "Dumbbell Curl",
            category = "Upper Body",
            primaryMuscle = "Biceps",
            secondaryMuscles = null,
            equipment = "Dumbbell"
        ),
        ExercisePreset(
            name = "Tricep Pushdown",
            category = "Upper Body",
            primaryMuscle = "Triceps",
            secondaryMuscles = null,
            equipment = "Cable"
        ),
        ExercisePreset(
            name = "Leg Press",
            category = "Lower Body",
            primaryMuscle = "Quadriceps",
            secondaryMuscles = "Glutes, Hamstrings",
            equipment = "Machine"
        ),
        ExercisePreset(
            name = "Lat Pulldown",
            category = "Upper Body",
            primaryMuscle = "Latissimus Dorsi",
            secondaryMuscles = "Biceps",
            equipment = "Cable"
        ),
        ExercisePreset(
            name = "Plank",
            category = "Core",
            primaryMuscle = "Rectus Abdominis",
            secondaryMuscles = null,
            equipment = "Bodyweight"
        ),
        ExercisePreset(
            name = "Lateral Raise",
            category = "Upper Body",
            primaryMuscle = "Deltoids",
            secondaryMuscles = null,
            equipment = "Dumbbell"
        ),
        ExercisePreset(
            name = "Romanian Deadlift",
            category = "Lower Body",
            primaryMuscle = "Hamstrings",
            secondaryMuscles = "Glutes, Erector Spinae",
            equipment = "Barbell"
        ),
        ExercisePreset(
            name = "Incline Dumbbell Press",
            category = "Upper Body",
            primaryMuscle = "Pectoralis Major",
            secondaryMuscles = "Shoulders, Triceps",
            equipment = "Dumbbell"
        ),
        ExercisePreset(
            name = "Leg Curl",
            category = "Lower Body",
            primaryMuscle = "Hamstrings",
            secondaryMuscles = null,
            equipment = "Machine"
        ),
        ExercisePreset(
            name = "Push-Up",
            category = "Upper Body",
            primaryMuscle = "Pectoralis Major",
            secondaryMuscles = "Triceps, Shoulders",
            equipment = "Bodyweight"
        )
    )
}

val CategoryOptions = listOf(
    "Upper Body",
    "Lower Body",
    "Full Body",
    "Core",
    "Cardio"
)

val EquipmentOptions = listOf(
    "Barbell",
    "Dumbbell",
    "Cable",
    "Machine",
    "Bodyweight",
    "Kettlebell",
    "Band",
    "None"
)
