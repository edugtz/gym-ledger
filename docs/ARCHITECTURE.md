# GymLedger — Architecture

## Architecture Goal

Use a simple, maintainable Android architecture.

Avoid overengineering.

The v1 architecture is:

```text
Compose UI
  ↓
ViewModel
  ↓
Repository
  ↓
Room / DataStore / File IO
```

## Module Structure

Use a single Android module in v1:

```text
:app
```

Do not use multi-module architecture in v1.

## Dependency Injection

Do not use Hilt in v1.

Use a manual `AppContainer`.

Example:

```kotlin
class AppContainer(context: Context) {
    val database = GymLedgerDatabase.create(context)

    val exerciseRepository = ExerciseRepository(database.exerciseDao())
    val workoutRepository = WorkoutRepository(
        workoutSessionDao = database.workoutSessionDao(),
        workoutSetDao = database.workoutSetDao()
    )
    val routineRepository = RoutineRepository(
        routineDao = database.routineDao(),
        routineExerciseDao = database.routineExerciseDao()
    )
    val foodRepository = FoodRepository(database.foodDao())
    val nutritionRepository = NutritionRepository(
        mealDao = database.mealDao(),
        mealItemDao = database.mealItemDao(),
        foodDao = database.foodDao()
    )
    val bodyRepository = BodyRepository(database.bodyMeasurementDao())
    val settingsRepository = SettingsRepository(context)
}
```

## Suggested Package Structure

```text
com.edu.gymledger
├── MainActivity.kt
├── GymLedgerApplication.kt
├── app
│   ├── AppContainer.kt
│   ├── GymLedgerApp.kt
│   └── AppNavGraph.kt
├── core
│   ├── date
│   ├── file
│   ├── result
│   ├── ui
│   └── validation
├── data
│   ├── db
│   │   ├── GymLedgerDatabase.kt
│   │   ├── dao
│   │   └── entity
│   ├── datastore
│   ├── importexport
│   ├── photo
│   └── repository
├── domain
│   └── model
├── feature
│   ├── dashboard
│   ├── workouts
│   ├── exercises
│   ├── routines
│   ├── nutrition
│   ├── foods
│   ├── body
│   ├── importexport
│   └── settings
└── design
    ├── components
    └── theme
```

## Navigation

Use Navigation Compose.

Main tabs:

- Dashboard
- Workouts
- Nutrition
- Body
- Settings

Secondary screens:

- Exercises
- Routines
- Foods
- Meal Detail
- Workout Detail
- Routine Detail
- Import / Export

Suggested routes:

```kotlin
object Routes {
    const val Dashboard = "dashboard"
    const val Workouts = "workouts"
    const val WorkoutDetail = "workouts/{sessionId}"
    const val Exercises = "exercises"
    const val Routines = "routines"
    const val RoutineDetail = "routines/{routineId}"
    const val Nutrition = "nutrition"
    const val Foods = "foods"
    const val MealDetail = "meals/{mealId}"
    const val Body = "body"
    const val ImportExport = "import_export"
    const val Settings = "settings"
}
```

## Data Storage

Use Room for structured app data.

Use DataStore Preferences for settings.

Use app-specific storage for meal photos.

Use Storage Access Framework for import/export when possible.

## Date Handling

Use ISO-8601 strings for database and import/export.

Example:

```text
2026-05-23T18:00:00-06:00
```

## Room Entities

### ExerciseEntity

```kotlin
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String?,
    val primaryMuscle: String?,
    val secondaryMuscles: String?,
    val equipment: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### WorkoutSessionEntity

```kotlin
@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long?,
    val title: String,
    val startedAt: String,
    val endedAt: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### WorkoutSetEntity

```kotlin
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("sessionId"),
        Index("exerciseId")
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
    val rpe: Double?,
    val rir: Double?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### RoutineEntity

```kotlin
@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### RoutineExerciseEntity

```kotlin
@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("routineId"),
        Index("exerciseId")
    ]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val targetWeight: Double?,
    val restSeconds: Int?,
    val notes: String?
)
```

### FoodEntity

```kotlin
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String?,
    val servingGrams: Double,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### MealEntity

```kotlin
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val eatenAt: String,
    val photoUri: String?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

### MealItemEntity

```kotlin
@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("mealId"),
        Index("foodId")
    ]
)
data class MealItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val foodId: Long?,
    val name: String,
    val grams: Double?,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val estimationSource: String,
    val notes: String?
)
```

Allowed `estimationSource` values:

```text
manual
food_database
photo_assisted
imported
```

### BodyMeasurementEntity

```kotlin
@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measuredAt: String,
    val bodyWeight: Double?,
    val waist: Double?,
    val chest: Double?,
    val arm: Double?,
    val thigh: Double?,
    val hip: Double?,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String
)
```

## DataStore Settings

Use DataStore Preferences for:

```text
weight_unit = kg | lb
theme = system | light | dark
daily_calorie_goal
daily_protein_goal
daily_carbs_goal
daily_fat_goal
```

## Repositories

Create simple repositories:

```text
ExerciseRepository
WorkoutRepository
RoutineRepository
FoodRepository
NutritionRepository
BodyRepository
SettingsRepository
ImportExportRepository
PhotoRepository
```

## ViewModels

Use one ViewModel per feature screen or screen group.

Examples:

```text
DashboardViewModel
ExercisesViewModel
WorkoutsViewModel
WorkoutDetailViewModel
RoutinesViewModel
RoutineDetailViewModel
NutritionViewModel
MealDetailViewModel
FoodsViewModel
BodyViewModel
SettingsViewModel
ImportExportViewModel
```

## Photo Estimation Interface

Do not implement real ML in v1.

Create an interface so the implementation can be replaced later.

```kotlin
interface FoodPhotoEstimator {
    suspend fun estimate(input: FoodPhotoEstimateInput): FoodPhotoEstimateResult
}
```

```kotlin
data class FoodPhotoEstimateInput(
    val photoUri: String,
    val userDescription: String?,
    val selectedFoodId: Long?,
    val estimatedGrams: Double?
)
```

```kotlin
data class FoodPhotoEstimateResult(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val confidence: Double,
    val requiresUserConfirmation: Boolean,
    val warning: String?
)
```

v1 implementation:

```text
AssistedFoodPhotoEstimator
```

Rules:

- If selected food and grams are available, calculate macros proportionally.
- If only a description is available, return an editable placeholder.
- Always require confirmation.
- Always show an approximation warning.
- Do not add AI or network dependencies in v1.

## Testing Strategy

Minimum unit tests:

- Exercise validation
- Workout set validation
- Food validation
- Macro calculation by grams
- Body measurement validation
- JSON serialization roundtrip
- CSV parser
- Room in-memory smoke test

## Validation Commands

Build:

```bash
./gradlew assembleDebug
```

Full validation:

```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
```

Install APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```