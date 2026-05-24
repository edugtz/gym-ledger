# GymLedger — Project Specification

## Product Summary

GymLedger is a personal Android app for tracking gym training, nutrition, body measurements, and local data backups.

The app is offline-first, local-only, and has no backend in v1.

The app must be installable as an APK.

## App Language

All user-facing UI text must be in English.

Examples:

- "Workouts"
- "Exercises"
- "Nutrition"
- "Meals"
- "Body Measurements"
- "Import / Export"
- "Settings"
- "Add Set"
- "Save Meal"
- "Estimated calories"
- "This is an approximation. Please review before saving."

## Target User

The target user is one person using the app for personal gym and nutrition tracking.

The app should be fast, practical, and simple to maintain.

## Core Requirements

GymLedger must support:

- Gym workouts
- Exercise catalog
- Workout sessions
- Sets, reps, weight, RPE, and RIR
- Routines
- Calories, macros, meals, and foods
- Body weight and body measurements
- CSV import/export
- JSON backup import/export
- Meal photos
- Approximate editable calorie estimates from meal photos

## Technical Stack

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Navigation Compose
- Kotlinx Serialization
- Gradle

## Product Principles

1. Offline-first.
2. Local data only in v1.
3. No account system.
4. No backend.
5. No overengineering.
6. No subscription or payment logic.
7. Every calculated or estimated value must be editable.
8. Photo-based food estimation must be clearly labeled as approximate.
9. The app should be usable even if photo estimation is basic.
10. The app should prioritize fast manual entry.

## MVP Scope

### Dashboard

The Dashboard should show:

- Today's calories
- Today's protein
- Today's carbs
- Today's fat
- Progress against macro goals
- Latest body weight
- Latest workout
- Quick actions:
  - New workout
  - Add meal
  - Add body measurement
  - Exercises
  - Foods

### Exercises

The user can:

- Create exercises
- Edit exercises
- Delete unused exercises
- View an exercise list
- Store:
  - name
  - category
  - primary muscle
  - secondary muscles
  - equipment
  - notes

### Workouts

The user can:

- Create workout sessions
- View workout history
- Open workout detail
- Add sets
- Edit sets
- Delete sets
- End a workout session

Each set can store:

- exercise
- set index
- reps
- weight
- RPE
- RIR
- notes

### Routines

The user can:

- Create routines
- Edit routines
- Delete routines
- Add exercises to routines
- Define target sets
- Define target rep range
- Define optional target weight
- Define optional rest time
- Start a workout from a routine

### Nutrition

The user can:

- Create foods
- Edit foods
- Search foods
- Create meals
- Add foods to meals
- Add manual meal items
- Edit calories and macros
- View daily nutrition summary

Food data includes:

- name
- brand
- serving grams
- calories per serving
- protein per serving
- carbs per serving
- fat per serving
- notes

### Meal Photos

The user can:

- Take or select a meal photo
- Attach the photo to a meal
- View the photo in meal detail
- Remove the photo
- Create an approximate meal item from:
  - photo
  - optional description
  - optional selected food
  - estimated grams

Important v1 rule:

The app does not need real computer vision in v1.

The v1 photo estimation flow is assisted and editable:

- If the user selects a known food and grams, calculate macros proportionally.
- If the user only provides a description, create an editable placeholder.
- Always require user confirmation before saving.
- Always show an approximation warning.

### Body Measurements

The user can track:

- body weight
- waist
- chest
- arm
- thigh
- hip
- notes
- measurement date

### Import / Export

The app must support:

- Full JSON backup export
- Full JSON backup import
- CSV export
- CSV import

Initial CSV support:

- exercises.csv
- foods.csv
- body_measurements.csv
- workout_sessions.csv
- workout_sets.csv
- meals.csv
- meal_items.csv

## Out of Scope for v1

- Backend
- Login
- Cloud sync
- Multi-user support
- Social features
- Payments
- Subscriptions
- Wear OS
- Health Connect
- Barcode scanning
- Real AI vision model
- Cloud AI estimation
- Advanced analytics
- Complex charts
- Multi-module architecture
- Hilt dependency injection

## Business Rules

### Exercises

- Exercise name is required.
- Exercise name must not be blank.
- Exercises used by workout sets should not be hard-deleted if it breaks history.

### Workout Sessions

- A workout session can exist without a routine.
- A workout session can reference a routine.
- A workout session can have zero or more sets.
- Ending a session sets `endedAt`.

### Workout Sets

- A set must belong to a workout session.
- A set must reference an exercise.
- Reps must be greater than 0.
- Weight must be greater than or equal to 0.
- RPE is optional.
- RPE must be between 1 and 10 if present.
- RIR is optional.
- RIR must be greater than or equal to 0 if present.

### Routines

- A routine can contain zero or more exercises.
- Deleting a routine must not delete the exercise catalog.
- Changing a routine must not change historical workout sessions.

### Nutrition

- A meal can have zero or more meal items.
- A meal item can reference a food or be fully manual.
- Macros calculated from food and grams must be editable.
- Daily totals are calculated from meal items.

Macro calculation:

```text
factor = grams / servingGrams
calories = caloriesPerServing * factor
protein = proteinPerServing * factor
carbs = carbsPerServing * factor
fat = fatPerServing * factor
```

### Meal Photos

- Photos are stored locally.
- Photos are associated with meals.
- Photo estimates are approximate.
- Photo estimates must require user confirmation.
- Photo estimates must be editable before saving.

### Body Measurements

- At least one measurement value should be present.
- Values must be positive if provided.

### Imports

- Imports must validate before writing where possible.
- Invalid rows must show readable errors.
- Failed imports must not corrupt existing data.
- Relational imports should use external IDs from CSV.

### Exports

- JSON backup should be enough to restore app data.
- CSV exports should be spreadsheet-friendly.
- Dates should use ISO-8601 strings.

## Package Name

```text
com.edu.gymledger
```

## APK Requirement

The app must build with:

```bash
./gradlew assembleDebug
```

Expected APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```