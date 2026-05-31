# CURRENT_PHASE.md — Phase 5: Room Smoke Tests

## Objective

Verify the Room database foundation works before building repositories and features on top of it.

## Test Strategy

Use local JVM unit tests with Robolectric.

Tests must live under:

```text
app/src/test/
```

Do not move these tests to `androidTest`.

Do not use `connectedDebugAndroidTest`.

## Tasks

- Add only the minimum test dependencies required for local JVM Room tests.
- Create a Room in-memory database test setup under `app/src/test/`.
- Test inserting an exercise.
- Test reading exercises.
- Test deleting an exercise.
- Close the database after each test.

## Do Not Do

- Do not create UI.
- Do not create user flows.
- Do not add repositories.
- Do not add ViewModels.
- Do not add business logic.
- Do not move tests to `androidTest`.
- Do not use instrumented tests.
- Do not use `connectedDebugAndroidTest`.
- Do not implement future phases.

## Acceptance Criteria

- `./gradlew testDebugUnitTest` passes.
- `./gradlew assembleDebug` passes.
- Room works with an in-memory database in local JVM tests.
- Exercise insert/read/delete smoke tests pass.
- App still launches without crash.

## Validation Commands

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

## Suggested Commit

```text
test: add Room smoke tests
```