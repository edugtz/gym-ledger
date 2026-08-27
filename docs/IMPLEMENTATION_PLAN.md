# Phase 17H — Food Recents and Favorites Implementation Plan

> Approved post-functional-QA alignment: this plan describes the current
> Phase 17H contract, including `favoriteAt`, Room v8, and the single-scroll
> Foods layout. Earlier pre-QA wording below is superseded where it conflicts.

## 1. Status

**COMPLETE — IMPLEMENTED, VALIDATED, REVIEWED, FUNCTIONAL QA PASS**

Phase:

```text
17H — Food Recents and Favorites
```

Implementation branch:

```text
17h-food-recents-favorites
```

Base branch:

```text
dev
```

---

## 2. Objective

Add recents and favorites to the saved Foods list so frequent foods are
accessible in seconds.

The user must be able to:

```text
see all saved foods with favorites pinned to top
filter by All / Favorites / Recent
toggle favorite on any saved food
see foods ordered by recency after meal logging (Phase 20)
search within any active filter
```

Phase 17H implements the persistence layer, DAO queries, repository API,
ViewModel filter state, and Foods UI filter/favorite toggle.

Phase 17H does NOT call `markUsed` from any production UI.
Phase 20 will be the normal production caller of `markUsed`.

---

## 3. Product Principles

GymLedger remains:

```text
local-first
offline-capable
explicit-action only
```

Favorites are a local preference. They are not synced, not shared,
and not derived from remote state.

Recency is driven solely by explicit `markUsed(foodId)` calls.
Viewing, searching, filtering, editing, creating, or favoriting a food
does NOT update recency.

A normal user may see an empty Recent tab until Phase 20 exists.
Do not fabricate recent entries in Phase 17H.

---

## 4. Existing Foundation to Reuse

### Room / Entity pattern

`ExerciseEntity` already has:

```kotlin
@ColumnInfo(defaultValue = "0")
val isFavorite: Boolean = false
```

Phase 17H follows this exact pattern for `FoodEntity`.

### DAO pattern

`FoodDao` uses simple suspend + Flow methods.
Phase 17H adds new query methods to the same interface.

### Repository pattern

`FoodRepository` wraps `FoodDao`, maps entities to domain models,
and validates inputs. Phase 17H extends this class.

### ViewModel pattern

`FoodsViewModel` uses `flatMapLatest` on a `searchQuery` `MutableStateFlow`
to switch between `listAll()` and `searchByName()`.
Phase 17H extends this to also consider an `activeFilter` state.

### Test patterns

- `FoodRepositoryTest` uses Robolectric + in-memory Room.
- `FoodsViewModelTest` uses a handwritten `FakeFoodDao`.
- `SmartFoodEntryViewModelRemoteTest` has its own `FakeFoodDao`.

Phase 17H extends all three test files.

---

## 5. Authoritative Data Model

### FoodEntity additions

```kotlin
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPerServing: Int,
    val servingSize: Double? = null,
    @ColumnInfo(defaultValue = "0.0")
    val proteinPerServing: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val carbsPerServing: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0")
    val fatPerServing: Double = 0.0,

    // Phase 17H additions:
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,
    @ColumnInfo(defaultValue = "NULL")
    val favoriteAt: Long? = null,
    @ColumnInfo(defaultValue = "NULL")
    val lastUsedAt: Long? = null
)
```

### Food domain model additions

```kotlin
data class Food(
    val id: Long = 0,
    val name: String,
    val caloriesPerServing: Int,
    val servingSize: Double? = null,
    val proteinPerServing: Double = 0.0,
    val carbsPerServing: Double = 0.0,
    val fatPerServing: Double = 0.0,
    // Phase 17H additions:
    val isFavorite: Boolean = false,
    val favoriteAt: Long? = null,
    val lastUsedAt: Long? = null
)
```

`Food.toEntity()` and `Food.from(entity)` must map both new fields.

No other fields are added.

No `useCount`. No remote favorite state. No barcode persistence.

---

## 6. Room Migration Strategy

### Current / final DB version: 8

### Migration SQL

```sql
ALTER TABLE foods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0;
ALTER TABLE foods ADD COLUMN lastUsedAt INTEGER DEFAULT NULL;
ALTER TABLE foods ADD COLUMN favoriteAt INTEGER DEFAULT NULL;
```

`MIGRATION_6_7` adds `isFavorite` and `lastUsedAt`; `MIGRATION_7_8`
adds `favoriteAt`. Version 8 exists because the Phase 17H QA device had
already opened v7 before the favorite-ordering requirement was discovered.
Supported paths are v6 → v7 → v8, v7 → v8, and fresh v8.

### Migration implementation

Add an explicit `MIGRATION_6_7` object:

```kotlin
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE foods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "ALTER TABLE foods ADD COLUMN lastUsedAt INTEGER DEFAULT NULL"
        )
    }
}
```

### fallbackToDestructiveMigration removal

`fallbackToDestructiveMigration()` is NOT a safety net for migrations
that execute incorrectly. It allows destructive recreation when Room
cannot find a migration path.

For Phase 17H, remove `fallbackToDestructiveMigration()` from both:

- `GymLedgerDatabase.create()`
- `AppContainer.initialize()`

Replace with explicit `MIGRATION_6_7` and `MIGRATION_7_8`.

### Centralize database creation

There are currently two independent DB builders.
Preferred implementation: centralize so `AppContainer.initialize(context)`
delegates to `GymLedgerDatabase.create(context)`, leaving migration
configuration in one authoritative builder.

Do not perform a broader database architecture refactor beyond this
centralization.

### Existing Food rows MUST survive

The migration adds nullable/default columns only. No data is lost.
No rows are deleted. No columns are removed.

---

## 7. DAO Query Design

### New queries on FoodDao

Do NOT add `getRecentByFoodId()`.

Do not silently repurpose existing `listAll()` / `searchByName()` semantics.

Add explicit Phase 17H ranked/filter queries:

```kotlin
// All foods, ranked: favorites first, then recency, then alphabetical
@Query("""
    SELECT * FROM foods
    ORDER BY
        isFavorite DESC,
        favoriteAt DESC,
        lastUsedAt DESC,
        name COLLATE NOCASE ASC,
        id DESC
""")
fun listAllRanked(): Flow<List<FoodEntity>>

// Search within all foods, same ranking
@Query("""
    SELECT * FROM foods
    WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
    ORDER BY
        isFavorite DESC,
        favoriteAt DESC,
        lastUsedAt DESC,
        name COLLATE NOCASE ASC,
        id DESC
""")
fun searchRanked(query: String): Flow<List<FoodEntity>>

// Favorites only
@Query("""
    SELECT * FROM foods
    WHERE isFavorite = 1
    ORDER BY
        favoriteAt DESC,
        lastUsedAt DESC,
        name COLLATE NOCASE ASC,
        id DESC
""")
fun listFavorites(): Flow<List<FoodEntity>>

// Search within favorites
@Query("""
    SELECT * FROM foods
    WHERE isFavorite = 1
      AND name LIKE '%' || :query || '%' COLLATE NOCASE
    ORDER BY
        favoriteAt DESC,
        lastUsedAt DESC,
        name COLLATE NOCASE ASC,
        id DESC
""")
fun searchFavorites(query: String): Flow<List<FoodEntity>>

// Recent only (lastUsedAt not null)
@Query("""
    SELECT * FROM foods
    WHERE lastUsedAt IS NOT NULL
    ORDER BY
        lastUsedAt DESC,
        name COLLATE NOCASE ASC,
        id DESC
""")
fun listRecent(): Flow<List<FoodEntity>>

// Search within recent
@Query("""
    SELECT * FROM foods
    WHERE lastUsedAt IS NOT NULL
      AND name LIKE '%' || :query || '%' COLLATE NOCASE
    ORDER BY
        lastUsedAt DESC,
        name COLLATE NOCASE ASC,
        id DESC
""")
fun searchRecent(query: String): Flow<List<FoodEntity>>

// Deterministic favorite setter (not read-modify-write toggle)
@Query("UPDATE foods SET isFavorite = :isFavorite, favoriteAt = CASE WHEN :isFavorite THEN :favoriteAtMillis ELSE NULL END WHERE id = :foodId")
suspend fun setFavorite(foodId: Long, isFavorite: Boolean, favoriteAtMillis: Long?)

// Deterministic markUsed setter
@Query("UPDATE foods SET lastUsedAt = :usedAtMillis WHERE id = :foodId")
suspend fun markUsed(foodId: Long, usedAtMillis: Long)
```

### Ordering semantics

All/no query ordering:

```text
isFavorite DESC
favoriteAt DESC
lastUsedAt DESC
name COLLATE NOCASE ASC
id DESC
```

Favorites ordering:

```text
favoriteAt DESC
lastUsedAt DESC
name COLLATE NOCASE ASC
id DESC
```

Recent:

```text
WHERE lastUsedAt IS NOT NULL
ORDER BY lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
```

Search applies the `name LIKE` filter first, then the same ordering
for the active filter.

### Existing queries preserved

`listAll()`, `searchByName()`, `insert()`, `update()`, `delete()`,
`getById()` remain unchanged. They are still used by other consumers
(e.g., `FoodRepository.getAll()`, `getById()`, `create()`, `update()`).

---

## 8. Repository / Domain Behavior

### FoodRepository additions

```kotlin
// Filter-dependent flow
fun getAllRanked(): Flow<List<Food>>
fun searchRanked(query: String): Flow<List<Food>>
fun getFavorites(): Flow<List<Food>>
fun searchFavorites(query: String): Flow<List<Food>>
fun getRecent(): Flow<List<Food>>
fun searchRecent(query: String): Flow<List<Food>>

// Deterministic setters
suspend fun setFavorite(foodId: Long, isFavorite: Boolean, favoriteAtMillis: Long = System.currentTimeMillis())
suspend fun markUsed(foodId: Long, usedAtMillis: Long = System.currentTimeMillis())
```

### Entity ↔ domain mapping

`Food.from(entity)` and `food.toEntity()` must map `isFavorite` and
`lastUsedAt` without loss.

### create() defaults

`FoodRepository.create()` does NOT accept `isFavorite` or `lastUsedAt`.
New foods always get `isFavorite = false` and `lastUsedAt = null`.
Favoriting and usage tracking are separate operations.

### edit preserves favorites and recency

`FoodRepository.update()` uses the existing `Food.copy(...)` pattern.
The caller passes the full `Food` object including `isFavorite` and
`lastUsedAt`. These values are preserved through update.

---

## 9. Recency Semantics

### markUsed

```kotlin
FoodRepository.markUsed(
    foodId: Long,
    usedAtMillis: Long = System.currentTimeMillis()
)
```

DAO receives the explicit timestamp so tests can be deterministic.

### What MUST NOT count as usage

- view / browse
- search
- filter
- edit
- favorite / unfavorite
- create / save Food
- Smart Food Entry save
- remote lookup
- barcode lookup

### Phase 17H vs Phase 20

Phase 17H implements the `markUsed` API.
No normal production UI in Phase 17H calls it.

Phase 20 will call `markUsed` only after successful `MealItem` persistence.
This is the correct production caller.

---

## 10. ViewModel State

### FoodsViewModel additions

```kotlin
enum class FoodFilter { ALL, FAVORITES, RECENT }

data class FoodsUiState(
    val searchQuery: String = "",
    val foods: List<Food> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    // Phase 17H additions:
    val activeFilter: FoodFilter = FoodFilter.ALL
)
```

### Flow rewiring

Current pattern:

```kotlin
searchQuery.flatMapLatest { query ->
    if (query.isBlank()) repository.getAll()
    else repository.searchByName(query)
}
```

Phase 17H pattern:

```kotlin
// Combine activeFilter and searchQuery
activeFilter.combine(searchQuery) { filter, query -> filter to query }
    .flatMapLatest { (filter, query) ->
        when (filter) {
            FoodFilter.ALL -> {
                if (query.isBlank()) repository.getAllRanked()
                else repository.searchRanked(query)
            }
            FoodFilter.FAVORITES -> {
                if (query.isBlank()) repository.getFavorites()
                else repository.searchFavorites(query)
            }
            FoodFilter.RECENT -> {
                if (query.isBlank()) repository.getRecent()
                else repository.searchRecent(query)
            }
        }
    }
```

### New ViewModel methods

```kotlin
fun setFilter(filter: FoodFilter)
fun toggleFavorite(food: Food)
```

`setFilter` updates `activeFilter` and resets `searchQuery` to empty.
`toggleFavorite` uses per-Food serialization so rapid actions do not
derive repeated targets from the same stale Food object. Pending state is
cleaned up on success and failure.

---

## 11. Foods UI Behavior

### Filter chips

Three `FilterChip` options in a `FlowRow` below the Smart Entry card
and above the search bar:

```text
[ All ] [ Favorites ] [ Recent ]
```

`All` is selected by default.

### Favorite toggle

Small star/heart icon button on each `FoodCard`, positioned next to
the existing Edit and Delete icons. Tapping calls
`viewModel.toggleFavorite(food)`.

Visual state: filled when `food.isFavorite == true`, outlined when false.

### Search + filter interaction

- Filter and search are independent.
- Search applies within the active filter scope.
- Clearing search shows the full filter results.

### Empty states

| Filter | Empty state |
|--------|-------------|
| ALL / no foods | "No foods yet" (existing) |
| ALL / search no match | "No foods found" (existing) |
| FAVORITES / no favorites | "No favorites yet. Star foods you use often." |
| RECENT / no recent foods | "No recent foods yet. Foods appear here after you log them in meals." |

Do NOT fabricate Recent entries for Phase 17H.

### Summary text

Update `FoodsSummary` to reflect the active filter:

- All: "X foods saved" / "X foods found"
- Favorites: "X favorite foods" / "X favorites found"
- Recent: "X recent foods" / "X recent foods found"

---

## 12. Search / Ranking Rules

### All filter, no query

```sql
SELECT * FROM foods
ORDER BY isFavorite DESC, favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
```

### All filter, with query

```sql
SELECT * FROM foods
WHERE name LIKE '%' || :query || '%' COLLATE NOCASE
ORDER BY isFavorite DESC, favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
```

### Favorites filter, no query

```sql
SELECT * FROM foods
WHERE isFavorite = 1
ORDER BY favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
```

### Favorites filter, with query

```sql
SELECT * FROM foods
WHERE isFavorite = 1
  AND name LIKE '%' || :query || '%' COLLATE NOCASE
ORDER BY favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
```

### Recent filter, no query

```sql
SELECT * FROM foods
WHERE lastUsedAt IS NOT NULL
ORDER BY lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
```

### Recent filter, with query

```sql
SELECT * FROM foods
WHERE lastUsedAt IS NOT NULL
  AND name LIKE '%' || :query || '%' COLLATE NOCASE
ORDER BY lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC
```

---

## 13. Error / Failure Behavior

### Migration failure

If `MIGRATION_6_7` fails, Room throws an exception.
Without `fallbackToDestructiveMigration`, the app crashes on upgrade
rather than silently losing data. This is the correct behavior for
a local-first app.

### markUsed with invalid foodId

`UPDATE foods SET lastUsedAt = ... WHERE id = :foodId` is a no-op
if the foodId does not exist. No error is thrown. This is acceptable
because Phase 20 will call `markUsed` only for foods that exist in
the current meal context.

### setFavorite with invalid foodId

Same as `markUsed` — a no-op if the foodId does not exist.

### toggleFavorite race condition

`toggleFavorite` serializes pending changes per Food ID and advances each
target from the serialized pending state, rather than repeatedly deriving
it from a stale Food object. Pending state is cleaned up on success and
failure. The repository API remains the deterministic `setFavorite` setter.

---

## 14. Test Strategy

### Migration test

**New file:** `app/src/test/java/com/edu/gymledger/data/db/Migration6To7Test.kt`

Because `exportSchema = false`, do not use `MigrationTestHelper`.

Use the existing Robolectric/SQLite infrastructure:

1. Construct a legitimate file-backed v6 schema and insert a Food row.
2. Open it through current Room v8 with `MIGRATION_6_7` and
   `MIGRATION_7_8` registered; verify all metadata defaults and schema
   validation.
3. Construct a legitimate v7 schema containing `isFavorite` and
   `lastUsedAt`, then open it through Room v8 with `MIGRATION_7_8`.
4. Verify old values survive, `favoriteAt` defaults to null, and a v8
   row round-trips independent favorite and usage timestamps.

Alternatively, use `SupportSQLiteOpenHelper` directly to create the
v6 schema, then run migration via `MigrationTestHelper`-style manual
invocation if the Robolectric route is too cumbersome.

### DAO test

**New file:** `app/src/test/java/com/edu/gymledger/data/db/FoodDaoTest.kt`

Test coverage:

```text
listAllRanked orders: favorites first, then recency, then alphabetical
searchRanked filters by name within ranked order
listFavorites returns only isFavorite = 1
searchFavorites filters by name within favorites
listRecent returns only lastUsedAt IS NOT NULL
searchRecent filters by name within recent
setFavorite(foodId, true, exactTimestamp) sets isFavorite = 1 and favoriteAt = exactTimestamp
setFavorite(foodId, false, anyTimestamp) sets isFavorite = 0 and favoriteAt = NULL
markUsed(foodId, timestamp) sets lastUsedAt to exact timestamp
existing CRUD (insert/update/delete/getById/listAll/searchByName) unchanged
```

### Repository test (extend existing)

**Extend:** `app/src/test/java/com/edu/gymledger/data/repository/FoodRepositoryTest.kt`

Add:

```text
create defaults favorite=false, lastUsedAt=null
getAllRanked returns domain Foods with correct ranking
searchRanked filters and ranks correctly
getFavorites returns only favorites
getRecent returns only recent
setFavorite changes favorite state
markUsed sets exact timestamp
edit preserves isFavorite and lastUsedAt
```

### ViewModel test (extend existing)

**Extend:** `app/src/test/java/com/edu/gymledger/feature/nutrition/FoodsViewModelTest.kt`

Add:

```text
default filter is ALL
setFilter(FAVORITES) shows only favorites
setFilter(RECENT) shows only recent
setFilter resets search query
toggleFavorite changes food state
search within ALL filters all foods
search within FAVORITES filters favorites only
search within RECENT filters recent only
empty state derives correctly per filter
```

### FakeFoodDao updates

Both `FoodsViewModelTest.FakeFoodDao` and
`SmartFoodEntryViewModelRemoteTest.FakeFoodDao` must be updated with:

```text
new DAO method signatures (listAllRanked, searchRanked, etc.)
setFavorite implementation
markUsed implementation
stored foods track isFavorite and lastUsedAt
```

Do not add test-only production hooks to create recent foods.

---

## 15. Exact Expected Files

### Production files to modify (8)

| File | Change |
|------|--------|
| `app/src/main/java/com/edu/gymledger/data/db/entity/FoodEntity.kt` | Add `isFavorite`, `favoriteAt`, and `lastUsedAt` fields |
| `app/src/main/java/com/edu/gymledger/domain/model/Food.kt` | Mirror new fields; update `toEntity()` and `from()` |
| `app/src/main/java/com/edu/gymledger/data/db/dao/FoodDao.kt` | Add ranked/filter queries, `setFavorite`, `markUsed` |
| `app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt` | Expose new DAO methods; add `setFavorite`, `markUsed` |
| `app/src/main/java/com/edu/gymledger/data/db/GymLedgerDatabase.kt` | Bump version 7→8; add `MIGRATION_7_8` alongside `MIGRATION_6_7`; remove destructive fallback |
| `app/src/main/java/com/edu/gymledger/app/AppContainer.kt` | Centralize DB builder to use `GymLedgerDatabase.create()` |
| `app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsViewModel.kt` | Add `activeFilter`, filter-dependent flow, `toggleFavorite` |
| `app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsScreen.kt` | Add filter chips, favorite toggle, filter-aware empty states |

### Files unchanged

| File | Reason |
|------|--------|
| `FoodsViewModelFactory.kt` | ViewModel constructor unchanged (still takes `FoodRepository`) |
| `SmartFoodEntryViewModel.kt` | No favorites/recents logic |
| `SmartFoodEntryScreen.kt` | Unchanged |
| `SmartFoodEntryViewModelFactory.kt` | Unchanged |
| `FoodReferenceRepository.kt` | Unchanged |
| `FoodReferenceCalculator.kt` | Unchanged |
| `SmartFoodEntryAvailabilityUi.kt` | Unchanged |
| All remote files | Out of scope |
| All Worker files | Out of scope |

### Test files (2 new, 3 extend)

| File | Action |
|------|--------|
| `app/src/test/java/com/edu/gymledger/data/db/Migration6To7Test.kt` | **New** — migration preservation test |
| `app/src/test/java/com/edu/gymledger/data/db/FoodDaoTest.kt` | **New** — DAO query/filter/ranking tests |
| `app/src/test/java/com/edu/gymledger/data/repository/FoodRepositoryTest.kt` | **Extend** — new repository methods |
| `app/src/test/java/com/edu/gymledger/feature/nutrition/FoodsViewModelTest.kt` | **Extend** — filter/toggle/ranking |
| `app/src/test/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModelRemoteTest.kt` | **Extend** — FakeFoodDao interface compliance |

---

## 16. Explicit Out-of-Scope List

Do NOT add:

```text
meal logging
MealItem changes
quick-add to meals
meal templates
copy previous day/meal
cloud sync
Worker changes
USDA changes
Open Food Facts changes
barcode changes
remote/reference favorites
unified remote/local search
camera scanner
new dependencies
future-phase work
useCount field
```

---

## 17. Validation Commands

### Implementation gate

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

Expected:

```text
BUILD SUCCESSFUL
0 test failures
0 test errors
```

### Code quality

```bash
git diff --check
```

### Scope checks

```bash
git diff -- worker/
git diff -- app/src/main/java/com/edu/gymledger/data/remote/
```

Both must be empty.

### After documentation changes only

```bash
git diff --check
```

---

## 18. Functional QA Checklist

### Filter switching

```text
PASS — All filter shows all saved foods
PASS — Favorites filter shows only favorited foods
PASS — Recent filter shows only recently used foods
PASS — switching filter resets search query
PASS — All filter is selected by default
```

### Favorite toggle

```text
PASS — tap star icon toggles favorite on
PASS — tap star icon toggles favorite off
PASS — favorited food appears in Favorites filter
PASS — unfavorited food disappears from Favorites filter
PASS — favorite state survives app restart
PASS — favorite state survives force-stop
```

### Search within filter

```text
PASS — search within All filters all foods
PASS — search within Favorites filters favorites only
PASS — search within Recent filters recent only
PASS — clearing search restores full filter results
PASS — empty search state shows correct message per filter
```

### Ranking

```text
PASS — All: favorites pinned to top
PASS — All: within same favorite status, most recent first
PASS — All: within same recency, alphabetical
PASS — Favorites: most recently used first
PASS — Recent: most recently used first
```

### Empty states

```text
PASS — All / no foods: "No foods yet"
PASS — All / search empty: "No foods found"
PASS — Favorites / no favorites: "No favorites yet. Star foods you use often."
PASS — Recent / no recent: "No recent foods yet. Foods appear here after you log them in meals."
```

### Persistence

```text
PASS — new foods created with isFavorite=false, lastUsedAt=null
PASS — editing a food preserves isFavorite and lastUsedAt
PASS — favorite toggle persists across restart
PASS — all existing Foods CRUD unchanged
PASS — Smart Food Entry save still works
PASS — barcode lookup save still works
```

### Regression

```text
PASS — local reference flow unchanged
PASS — generic USDA online flow unchanged
PASS — barcode lookup flow unchanged
PASS — Smart Food Entry mode switching unchanged
```

---

## 19. AI Implementation / Review Route

### Planning / architecture

```text
ChatGPT + GitHub — COMPLETE
```

### Repository preflight

```text
/cloud-mimo — COMPLETE, reviewed PASS_WITH_CHANGES
```

### Primary builder

```text
GPT-5.6 Luna Medium
```

Suitable for:

```text
Room entity/DAO/migration
ViewModel state design
Compose filter UI
repository integration
cross-layer implementation
```

### Technical fallback

```text
DeepSeek V4 Flash Max
/cloud-ds-max
```

Suitable for:

```text
SQL query design
Room migration correctness
DAO testing
technical debugging
targeted implementation patches
```

Do not have Luna Medium and DeepSeek modify the same task concurrently.

### Independent code review

```text
/local-review
Qwen3.8 27B AWQ 5bpw + Lightning MTP
```

Reviewer must not edit.

Review specifically:

```text
migration correctness
existing row preservation
DAO query ordering
filter state management
favorite toggle semantics
recency semantics
FakeFoodDao compliance
scope creep
```

### Final review

```text
ChatGPT + GitHub
```

Final result:

```text
PASS
PASS_WITH_NOTES
BLOCKED
```

The user owns commits and pushes.

---

## 20. Completion Criteria

Phase 17H is complete when all are true:

```text
FoodEntity has isFavorite, favoriteAt, and lastUsedAt
Food domain model mirrors all three fields
FoodDao has ranked/filter queries, setFavorite, markUsed
FoodRepository exposes all new DAO methods
MIGRATION_6_7 adds isFavorite + lastUsedAt; MIGRATION_7_8 adds favoriteAt
fallbackToDestructiveMigration removed
database creation centralized
FoodsViewModel has filter state and toggleFavorite
FoodsScreen shows filter chips and favorite toggle
empty states are filter-aware
search works within each filter
All filter ranking: favorites → favoriteAt → recency → alphabetical
Favorites filter ranking: favoriteAt → recency → alphabetical
Recent filter: lastUsedAt IS NOT NULL, ordered by recency
markUsed API exists but is not called from Phase 17H UI
setFavorite uses deterministic setter with favoriteAtMillis, not toggle
existing Foods CRUD unchanged
Smart Food Entry unchanged
barcode lookup unchanged
USDA lookup unchanged
no new dependencies
no Worker changes
no remote changes
```

---

## Final Validation

```text
automated gate PASS (421 / 421 tests, 0 failures, 0 errors)
lint PASS
APK built
git diff --check PASS
scope checks PASS (worker/ and data/remote/ diffs empty)
independent review PASS
Functional QA PASS
```

---

## Suggested Commit

```text
feat: add food recents and favorites
```

Do not commit automatically.

The user commits manually after:

```text
automated validation PASS
manual QA PASS
independent review PASS/PASS_WITH_NOTES
```

Then:

```text
push branch
→ ChatGPT GitHub review
→ merge to dev only after PASS
```
