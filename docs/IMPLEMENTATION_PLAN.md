# IMPLEMENTATION_PLAN — Phase 5: Room Smoke Tests

## Phase Overview

**Phase:** 5 of the GymLedger roadmap
**Title:** Room Smoke Tests
**Goal:** Verify the Room database foundation works before building repositories and features on top of it.

---

## 1. Phase Consistency Assessment

**Verdict: Internally consistent, but existing test file is broken.**

The phase spec says "Use local JVM unit tests with Robolectric" and lists four smoke tests (insert, read, delete, close). This is coherent with the architecture doc's testing strategy which lists "Room in-memory smoke test" as a minimum unit test.

**Conflict found:** The existing `ExerciseDaoTest.kt` under `app/src/test/` uses `InstrumentationRegistry.getInstrumentation().targetContext`, which is an Android instrumented-test API. It will not compile or run in a pure JVM test environment without Robolectric configured. The file must be rewritten, not patched.

**No other conflicts.** The phase scope (test only, no UI/ViewModel/repository) aligns with the "Do Not Do" list.

---

## 2. Test Strategy

### Approach
- **Framework:** JUnit 4 (already in `libs.versions.toml`)
- **Android shim:** Robolectric (to provide `Context` and `Application` in JVM tests)
- **Database:** In-memory Room via `Room.inMemoryDatabaseBuilder()`
- **Test location:** `app/src/test/java/com/edu/gymledger/data/db/`
- **Not instrumented:** No `androidTest`, no `connectedDebugAndroidTest`

### Test Cases (one class, four tests)

| # | Test | What it verifies |
|---|------|-----------------|
| 1 | `insertExercise_insertsSuccessfully_returnsId` | `ExerciseDao.insert()` returns a positive ID |
| 2 | `insertExercise_thenGetById_returnsCorrectEntity` | `ExerciseDao.getById()` returns the inserted entity with matching id, name, type, muscleGroup |
| 3 | `insertMultipleExercises_thenListAll_returnsAll` | `ExerciseDao.listAll()` returns all inserted exercises via Flow |
| 4 | `insertExercise_thenDelete_removesFromDatabase` | `ExerciseDao.delete()` removes the entity; `getById` returns null afterward |

### Test lifecycle
- `@Before`: Build in-memory database, obtain DAO reference
- `@After`: Close the database

### Robolectric usage
- Use `RuntimeEnvironment.getApplication()` for the Context parameter in `Room.inMemoryDatabaseBuilder()`.
- No `@Config` annotation needed for basic Room tests; default Robolectric SDK level is sufficient.

---

## 3. Files Likely to Change

| File | Action | Reason |
|------|--------|--------|
| `gradle/libs.versions.toml` | **Edit** | Add Robolectric and room-testing versions/libraries |
| `app/build.gradle.kts` | **Edit** | Add `testImplementation` for Robolectric and room-testing |
| `app/src/test/java/com/edu/gymledger/data/db/ExerciseDaoTest.kt` | **Rewrite** | Replace `InstrumentationRegistry` with Robolectric; align test cases to phase spec |
| `app/src/test/java/com/edu/gymledger/ExampleUnitTest.kt` | **Optional cleanup** | Remove if it adds noise; otherwise leave as-is |

### Files NOT to change (per phase scope)
- No `app/src/main/` source files
- No `app/src/androidTest/` files
- No ViewModel, Repository, or UI code
- No `GymLedgerDatabase.kt` (the database itself is already correct)

---

## 4. Dependencies Needed

### New dependencies to add

```toml
# gradle/libs.versions.toml

[versions]
robolectric = "4.13"          # or latest stable 4.x
roomTesting = "2.6.1"         # same Room version as runtime

[libraries]
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "roomTesting" }
```

```kotlin
// app/build.gradle.kts — add to dependencies block
testImplementation(libs.robolectric)
testImplementation(libs.androidx.room.testing)
```

### Existing dependencies (already sufficient)
- `junit:junit` — test framework
- `androidx.test.ext:junit:1.2.0` — currently in testImplementation (keep, may be unused by Robolectric tests but harmless)
- `kotlinx-coroutines-test:1.7.3` — for `runTest` in suspend test functions
- `androidx.room:room-runtime`, `room-ktx` — already present as implementation

---

## 5. Validation Commands

```bash
# Run all local JVM unit tests (Phase 5 acceptance)
./gradlew testDebugUnitTest

# Verify app still builds (Phase 5 acceptance)
./gradlew assembleDebug

# Full validation (recommended before moving to next phase)
./gradlew clean lintDebug testDebugUnitTest assembleDebug
```

### Expected results
- `testDebugUnitTest`: All tests pass (4 new + any existing)
- `assembleDebug`: APK builds at `app/build/outputs/apk/debug/app-debug.apk`
- No lint errors introduced by test code

---

## 6. Builder-Ready Prompt

Copy-paste this for the builder:

```
Implement Phase 5: Room Smoke Tests.

## Context
- Read docs/CURRENT_PHASE.md for requirements.
- Read docs/ARCHITECTURE.md for Room entity and DAO definitions.
- The app already has GymLedgerDatabase, ExerciseEntity, and ExerciseDao in place.

## What to do

### 1. Add test dependencies
In `gradle/libs.versions.toml`, add:
- version `robolectric = "4.13"` (or latest stable 4.x)
- library `robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }`
- library `androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }`

In `app/build.gradle.kts`, add to dependencies:
- `testImplementation(libs.robolectric)`
- `testImplementation(libs.androidx.room.testing)`

### 2. Rewrite ExerciseDaoTest.kt
Replace the entire content of `app/src/test/java/com/edu/gymledger/data/db/ExerciseDaoTest.kt` with a Robolectric-based test class that:
- Uses `@Before` to build an in-memory Room database via `Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GymLedgerDatabase::class.java).build()`
- Uses `@After` to close the database
- Has exactly these four tests (use `@Test` and `runTest`):
  1. `insertExercise_insertsSuccessfully_returnsId` — insert an ExerciseEntity, assert returned ID > 0
  2. `insertExercise_thenGetById_returnsCorrectEntity` — insert, getById, assert id/name/type/muscleGroup match
  3. `insertMultipleExercises_thenListAll_returnsAll` — insert two exercises, listAll().first(), assert size >= 2
  4. `insertExercise_thenDelete_removesFromDatabase` — insert, getById (not null), delete, getById (null)

Use the real `ExerciseEntity` constructor: `ExerciseEntity(id=0, name="...", type=ExerciseType.COMPOUND, muscleGroup=MuscleGroup.CHEST)`.

### 3. Validate
Run:
```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Both must pass.

## Constraints
- Do NOT create UI, ViewModels, or repositories.
- Do NOT move tests to androidTest.
- Do NOT modify any app/src/main/ files.
- Do NOT add Hilt, Retrofit, or cloud dependencies.
- All code must compile with Kotlin 2.0.21 and AGP 8.7.0.
```

---

## Risk & Escalation Notes

| Risk | Mitigation |
|------|-----------|
| Robolectric SDK compatibility with AGP 8.7.0 / Kotlin 2.0.21 | Use Robolectric 4.13+ which supports AGP 8.x; if sync fails, try 4.12 |
| `RuntimeEnvironment.getApplication()` returns null in some Robolectric versions | Add `@Config(manifest = Config.NONE)` if needed to bootstrap the app |
| Room KSP annotation processing in test scope | `room-testing` artifact provides the in-memory builder; KSP runs at compile time as usual |
| Existing `ExampleUnitTest` or broken imports cause test failures | Clean build (`./gradlew clean`) before running tests; remove `ExampleUnitTest` if it fails |

## Post-Phase Checklist (for next phase handoff)
- [ ] `testDebugUnitTest` passes with 4+ tests
- [ ] `assembleDebug` produces APK
- [ ] No changes to main source code (only test + build config)
- [ ] Ready for Phase 6: Repository layer
