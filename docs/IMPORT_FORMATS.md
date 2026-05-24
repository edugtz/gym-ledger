# GymLedger — Import and Export Formats

## General Rules

- Use UTF-8.
- Use ISO-8601 date strings.
- Use dot decimal separator.
- CSV files must include headers.
- JSON backup must include `schemaVersion`.
- Invalid imports must show readable errors.
- Failed imports must not corrupt existing data.

## JSON Backup File

File name suggestion:

```text
gymledger_backup.json
```

## JSON Backup Structure

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-05-23T18:00:00-06:00",
  "app": "GymLedger",
  "settings": {
    "weightUnit": "kg",
    "theme": "system",
    "dailyCalorieGoal": 2500,
    "dailyProteinGoal": 180,
    "dailyCarbsGoal": 250,
    "dailyFatGoal": 80
  },
  "exercises": [],
  "routines": [],
  "routineExercises": [],
  "workoutSessions": [],
  "workoutSets": [],
  "foods": [],
  "meals": [],
  "mealItems": [],
  "bodyMeasurements": []
}
```

## JSON Models

### Exercise

```json
{
  "id": 1,
  "name": "Bench Press",
  "category": "Strength",
  "primaryMuscle": "Chest",
  "secondaryMuscles": "Triceps, Shoulders",
  "equipment": "Barbell",
  "notes": "",
  "createdAt": "2026-05-23T18:00:00-06:00",
  "updatedAt": "2026-05-23T18:00:00-06:00"
}
```

### Workout Session

```json
{
  "id": 1,
  "routineId": null,
  "title": "Push Day",
  "startedAt": "2026-05-23T18:00:00-06:00",
  "endedAt": "2026-05-23T19:10:00-06:00",
  "notes": "Good session",
  "createdAt": "2026-05-23T18:00:00-06:00",
  "updatedAt": "2026-05-23T19:10:00-06:00"
}
```

### Workout Set

```json
{
  "id": 1,
  "sessionId": 1,
  "exerciseId": 1,
  "setIndex": 1,
  "reps": 8,
  "weight": 80.0,
  "rpe": 8.5,
  "rir": 1.5,
  "notes": "",
  "createdAt": "2026-05-23T18:05:00-06:00",
  "updatedAt": "2026-05-23T18:05:00-06:00"
}
```

### Food

```json
{
  "id": 1,
  "name": "Chicken Breast",
  "brand": null,
  "servingGrams": 100.0,
  "caloriesPerServing": 165.0,
  "proteinPerServing": 31.0,
  "carbsPerServing": 0.0,
  "fatPerServing": 3.6,
  "notes": "",
  "createdAt": "2026-05-23T18:00:00-06:00",
  "updatedAt": "2026-05-23T18:00:00-06:00"
}
```

### Meal

```json
{
  "id": 1,
  "name": "Lunch",
  "eatenAt": "2026-05-23T14:30:00-06:00",
  "photoUri": "content://local/meal-photo-1.jpg",
  "notes": "Post workout meal",
  "createdAt": "2026-05-23T14:30:00-06:00",
  "updatedAt": "2026-05-23T14:30:00-06:00"
}
```

### Meal Item

```json
{
  "id": 1,
  "mealId": 1,
  "foodId": 1,
  "name": "Chicken Breast",
  "grams": 200.0,
  "calories": 330.0,
  "protein": 62.0,
  "carbs": 0.0,
  "fat": 7.2,
  "estimationSource": "food_database",
  "notes": ""
}
```

### Body Measurement

```json
{
  "id": 1,
  "measuredAt": "2026-05-23T08:00:00-06:00",
  "bodyWeight": 82.5,
  "waist": 86.0,
  "chest": 105.0,
  "arm": 39.0,
  "thigh": 62.0,
  "hip": 98.0,
  "notes": "",
  "createdAt": "2026-05-23T08:00:00-06:00",
  "updatedAt": "2026-05-23T08:00:00-06:00"
}
```

## CSV Files

### exercises.csv

```csv
external_id,name,category,primary_muscle,secondary_muscles,equipment,notes
ex_bench_press,Bench Press,Strength,Chest,"Triceps, Shoulders",Barbell,
ex_squat,Squat,Strength,Quads,"Glutes, Hamstrings",Barbell,
```

Required:

```text
name
```

Optional:

```text
external_id
category
primary_muscle
secondary_muscles
equipment
notes
```

### foods.csv

```csv
external_id,name,brand,serving_grams,calories,protein,carbs,fat,notes
food_chicken_breast,Chicken Breast,,100,165,31,0,3.6,
food_white_rice,White Rice Cooked,,100,130,2.7,28,0.3,
```

Required:

```text
name
serving_grams
calories
protein
carbs
fat
```

Rules:

```text
serving_grams > 0
calories >= 0
protein >= 0
carbs >= 0
fat >= 0
```

### body_measurements.csv

```csv
external_id,measured_at,body_weight,waist,chest,arm,thigh,hip,notes
body_001,2026-05-23T08:00:00-06:00,82.5,86,105,39,62,98,
```

Required:

```text
measured_at
```

Optional:

```text
external_id
body_weight
waist
chest
arm
thigh
hip
notes
```

### workout_sessions.csv

```csv
external_id,routine_external_id,title,started_at,ended_at,notes
session_001,,Push Day,2026-05-23T18:00:00-06:00,2026-05-23T19:10:00-06:00,Good session
```

Required:

```text
external_id
title
started_at
```

Optional:

```text
routine_external_id
ended_at
notes
```

### workout_sets.csv

```csv
external_id,session_external_id,exercise_external_id,set_index,reps,weight,rpe,rir,notes
set_001,session_001,ex_bench_press,1,8,80,8.5,1.5,
set_002,session_001,ex_bench_press,2,8,80,9,1,
```

Required:

```text
session_external_id
exercise_external_id
set_index
reps
weight
```

Optional:

```text
external_id
rpe
rir
notes
```

Rules:

```text
reps > 0
weight >= 0
rpe between 1 and 10 if present
rir >= 0 if present
```

### meals.csv

```csv
external_id,name,eaten_at,photo_uri,notes
meal_001,Lunch,2026-05-23T14:30:00-06:00,,Post workout meal
```

Required:

```text
external_id
name
eaten_at
```

Optional:

```text
photo_uri
notes
```

### meal_items.csv

```csv
external_id,meal_external_id,food_external_id,name,grams,calories,protein,carbs,fat,estimation_source,notes
meal_item_001,meal_001,food_chicken_breast,Chicken Breast,200,330,62,0,7.2,food_database,
meal_item_002,meal_001,,Salsa,30,20,0,4,0,manual,
```

Required:

```text
meal_external_id
name
calories
protein
carbs
fat
estimation_source
```

Optional:

```text
external_id
food_external_id
grams
notes
```

Allowed `estimation_source` values:

```text
manual
food_database
photo_assisted
imported
```

## Import Modes

Default mode:

```text
append
```

Optional mode:

```text
replace_all
```

`replace_all` should only be implemented if it is safe and transactional.

## Error Format

Show import errors like this:

```text
foods.csv row 4: serving_grams must be greater than 0
workout_sets.csv row 8: exercise_external_id "ex_deadlift" was not found
```

## Export Behavior

JSON export:

- Exports full backup.

CSV export:

- Exports individual CSV files.
- Empty data should still export valid headers.