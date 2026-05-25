## Phase 4

````md
# CURRENT_PHASE.md — Phase 4: Room Foundation

## Objective

Create the local database foundation.

## Tasks

Create:

- `GymLedgerDatabase`
- `ExerciseEntity`
- `WorkoutSessionEntity`
- `WorkoutSetEntity`
- `RoutineEntity`
- `RoutineExerciseEntity`
- `FoodEntity`
- `MealEntity`
- `MealItemEntity`
- `BodyMeasurementEntity`

Create basic DAOs for:

- insert
- update
- delete
- get by id
- observe/list

Create manual `AppContainer`.

## Do Not Do

- Do not create CRUD UI.
- Do not create ViewModels yet.
- Do not implement import/export.
- Do not add Hilt.

## Acceptance Criteria

- Room compiles.
- Database initializes without crash.
- Foreign keys compile.
- DAOs compile.
- App still opens.

## Validation Commands

```bash
./gradlew clean assembleDebug
```

## Suggested Commit

```text
feat: add Room database foundation
```
````