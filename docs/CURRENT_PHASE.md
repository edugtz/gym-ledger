````md
# CURRENT_PHASE.md — Phase 6: Exercise Repository

## Objective

Create the data layer for exercises.

## Tasks

Create `ExerciseRepository` with:

- get all exercises
- get exercise by id
- create exercise
- update exercise
- delete exercise

Create validation:

- name is required
- name is trimmed
- name cannot be blank

## Do Not Do

- Do not create UI.
- Do not create new navigation.
- Do not implement workouts yet.

## Acceptance Criteria

- Repository compiles.
- Validation has tests.
- Build passes.

## Validation Commands

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Suggested Commit

```text
feat: add exercise repository
```
````