# Phase 4 — Implementation Plan: Room Foundation

## Objective

Create the local database foundation for GymLedger.

---

## 1. Files to Create or Modify

### Entities (9 files)

| # | Path | Description |
|---|---|---|
| 1 | `data/db/entity/ExerciseEntity.kt` | Exercise catalog entity |
| 2 | `data/db/entity/WorkoutSessionEntity.kt` | Workout session entity |
| 3 | `data/db/entity/WorkoutSetEntity.kt` | Workout set entity (FK → session + exercise) |
| 4 | `data/db/entity/RoutineEntity.kt` | Routine entity |
| 5 | `data/db/entity/RoutineExerciseEntity.kt` | Routine-exercise mapping (FK → routine + exercise) |
| 6 | `data/db/entity/FoodEntity.kt` | Food database entity |
| 7 | `data/db/entity/MealEntity.kt` | Meal entity |
| 8 | `data/db/entity/MealItemEntity.kt` | Meal item entity (FK → meal + food) |
| 9 | `data/db/entity/BodyMeasurementEntity.kt` | Body measurement entity |

### DAOs (9 files)

| # | Path | Operations |
|---|---|---|
| 10 | `data/db/dao/ExerciseDao.kt` | insert, update, delete, getById, listAll (Flow) |
| 11 | `data/db/dao/WorkoutSessionDao.kt` | insert, update, delete, getById, listAll (Flow) |
| 12 | `data/db/dao/WorkoutSetDao.kt` | insert, update, delete, getById, listBySession (Flow) |
| 13 | `data/db/dao/RoutineDao.kt` | insert, update, delete, getById, listAll (Flow) |
| 14 | `data/db/dao/RoutineExerciseDao.kt` | insert, update, delete, getById, listByRoutine (Flow) |
| 15 | `data/db/dao/FoodDao.kt` | insert, update, delete, getById, listAll (Flow) |
| 16 | `data/db/dao/MealDao.kt` | insert, update, delete, getById, listAll (Flow) |
| 17 | `data/db/dao/MealItemDao.kt` | insert, update, delete, getById, listByMeal (Flow) |
| 18 | `data/db/dao/BodyMeasurementDao.kt` | insert, update, delete, getById, listAll (Flow) |

### Database + Wiring (4 files)

| # | Path | Description |
|---|---|---|
| 19 | `data/db/GymLedgerDatabase.kt` | Singleton `@Database` referencing all 10 entities + `create()` factory |
| 20 | `app/AppContainer.kt` | Manual DI container: instantiates database, exposes DAOs |
| 21 | `GymLedgerApplication.kt` | `Application` class that initializes `AppContainer` |
| 22 | `AndroidManifest.xml` | Add `android:name=".GymLedgerApplication"` to `<application>` |

---

## 2. Implementation Order

1. **Entities** (files 1–9) — all entity data classes in `data/db/entity/`
2. **DAOs** (files 10–18) — all DAO interfaces in `data/db/dao/`
3. **Database** (file 19) — `GymLedgerDatabase.kt` tying all entities together
4. **AppContainer** (file 20) — manual container exposing DAOs
5. **Application + Manifest** (files 21–22) — wire lifecycle

---

## 3. Main Risks / Build Traps

| Risk | Mitigation |
|---|---|
| **FK compile errors** — `WorkoutSetEntity` and `RoutineExerciseEntity` reference `ExerciseEntity`; wrong column names cause KSP failure. | All entities in same package; use explicit `@ColumnInfo` or rely on Room's default naming consistently. |
| **Column name mismatch** — Room derives column names from property names (camelCase → snake_case). | Use `@ColumnInfo(name = "...")` on every property to be explicit. |
| **`exportSchema`** — Room 2.6+ requires `exportSchema = false` in debug builds or a schema directory. | Set `exportSchema = false` on the `@Database` annotation. |
| **Singleton thread safety** — `Room.databaseBuilder` must be called once. | Use `object : RoomDatabase()` with a companion `create()` that uses `Lazy` or `synchronized`. |
| **App crashes on launch** — Missing `Application` class or manifest reference. | Add `android:name` to manifest; keep `Application.onCreate()` minimal. |
| **KSP version mismatch** — KSP 2.0.21-1.0.26 with Room 2.6.1 should be compatible. | Versions already in `libs.versions.toml`; no change needed. |

---

## 4. Validation Commands

```bash
# Primary build validation
./gradlew clean assembleDebug

# If that passes, quick smoke test (on device/emulator)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. Quality Gate Checklist

- [ ] All 10 entities compile with correct `@Entity(tableName = "...")`
- [ ] Foreign keys on `WorkoutSetEntity`, `RoutineExerciseEntity`, `MealItemEntity` compile without errors
- [ ] All 9 DAO interfaces have: `@Insert`, `@Update`, `@Delete`, `@Query` getById, and at least one `Flow<List<...>>` observer
- [ ] `GymLedgerDatabase` is a singleton with `create(context)` factory method
- [ ] `AppContainer` instantiates the database and exposes all DAOs as `val` properties
- [ ] `GymLedgerApplication` exists and is referenced in `AndroidManifest.xml`
- [ ] `./gradlew clean assembleDebug` succeeds with zero errors
- [ ] App launches without crash on device/emulator
- [ ] No Hilt, no Retrofit, no backend, no ViewModels, no CRUD UI (per Phase 4 constraints)

---

## 6. Suggested Commit Message

```text
feat: add Room database foundation

- 10 entity classes (exercises, workout_sessions, workout_sets,
  routines, routine_exercises, foods, meals, meal_items, body_measurements)
- 9 DAOs with insert/update/delete/getById/observe
- GymLedgerDatabase singleton with create() factory
- Manual AppContainer wiring
- GymLedgerApplication + manifest registration
```
