# Current Phase

## Status

**COMPLETE — FUNCTIONAL QA PASS**

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

## Functional QA Closeout

Real-device Functional QA passed. QA covered:

- real-device upgrade to v8 without losing existing Foods
- existing Foods preserved
- complete Foods screen scrolling
- corrected top spacing
- newly favorited Food moves to top
- next newly favorited Food becomes first
- unfavorite/refavorite ordering
- favorite/order persistence across restart
- Favorites search
- favoriting does not populate Recent
- rapid favorite interaction
- edit/delete/create smoke
- Smart Food Entry smoke
- no crash

Automated gate: 421 tests, 0 failures, 0 errors, lint PASS, APK built, git diff --check PASS, scope checks PASS, independent review PASS.

---

## Approved Functional-QA Realignment

Phase 17H's current contract is the post-functional-QA behavior below;
it supersedes earlier pre-QA wording where the two conflict.

Food persistence fields are `isFavorite: Boolean = false`,
`favoriteAt: Long? = null`, and `lastUsedAt: Long? = null`.
`isFavorite` is the saved local preference. `favoriteAt` is the exact
timestamp of the most recent transition to favorite. `lastUsedAt` is the
actual consumption/use timestamp only. Favoriting does not modify
`lastUsedAt`; `markUsed` does not modify `favoriteAt`. Unfavoriting sets
`isFavorite = false` and `favoriteAt = null`; refavoriting stores the new
exact timestamp.

The current Room target is version 8:

```text
v6 -> MIGRATION_6_7 -> v7 -> MIGRATION_7_8 -> v8
v7 -> MIGRATION_7_8 -> v8
fresh install -> v8
```

`MIGRATION_6_7` adds `isFavorite` and `lastUsedAt`; `MIGRATION_7_8`
adds `favoriteAt`. Version 8 exists because the Phase 17H QA device had
already opened the v7 schema before the favorite-ordering requirement was
discovered. No destructive migration is used.

All foods rank by `isFavorite DESC, favoriteAt DESC, lastUsedAt DESC,
name COLLATE NOCASE ASC, id DESC`. Favorites rank by
`favoriteAt DESC, lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC`.
Recent remains strictly `lastUsedAt IS NOT NULL`, ordered by
`lastUsedAt DESC, name COLLATE NOCASE ASC, id DESC`; `favoriteAt` never
affects Recent. Search within each filter preserves its ordering.

The Foods screen is one vertically scrollable page: the inline Foods
heading, controls, search, summary, and saved-food list share one
page-level scroll flow with no nested lower-only vertical list. The
Foods-specific inline heading avoids restoring the duplicate fixed app bar
that caused the excessive top gap during functional QA.

### Favorite API contract

`setFavorite(foodId, isFavorite, favoriteAtMillis)` accepts the target
state and exact timestamp for deterministic tests. The database enforces
`isFavorite = false => favoriteAt = NULL` regardless of any supplied
timestamp; `isFavorite = true` stores the supplied timestamp. Favorite
changes are serialized per Food ID so rapid stale-state taps remain
deterministic.

---

## Prior Phase

### Phase 17G — Manual Barcode Lookup

**COMPLETE — merged to `dev`**

Phase 17G delivered manual typed/pasted barcode lookup through the existing GymLedger Worker and Open Food Facts integration.

Functional QA passed on a real device and covered:

- barcode entry/navigation;
- unavailable/disabled states;
- barcode validation;
- real Open Food Facts lookup;
- UPC/EAN GTIN-equivalent normalization;
- `success_with_warnings`;
- `not_found`;
- nameless product → Empty/Create manually;
- incomplete or per-100ml nutrition → reviewable/manual-only;
- usable per-100g nutrition → Use product;
- quantity recalculation;
- edit/save as a custom local Food;
- saved Food appearing in Foods;
- human-readable Open Food Facts attribution/copy.

The OFF normalization interoperability issue discovered during 17G QA was fixed separately, reviewed, merged to `dev`, deployed, and live-validated before 17G was closed.

Phase 17H starts from that completed baseline.

---

## Objective

Add persistent favorites and recency foundations for saved local Foods so frequently used foods can be surfaced quickly in current and future nutrition flows.

The immediate user-facing result of Phase 17H is:

```text
Foods
→ All / Favorites / Recent
→ favorite/unfavorite saved Foods
→ search within the active filter
→ deterministic saved-food ranking
```

Phase 17H also creates the explicit local recency API that Phase 20 will use when a Food is actually logged into a MealItem.

---

## Product Principles

GymLedger remains:

```text
local-first
offline-capable
explicit-action only
editable
non-blocking
```

Favorites and recency belong only to persisted local `Food` records.

Remote/reference/barcode results do not become favorites or recent items merely because they were viewed or returned by lookup.

A remote/reference/barcode result must first become a normal saved local Food through the existing explicit-save flow.

---

## Authoritative Recency Semantics

`lastUsedAt` changes only through an explicit real-use event:

```text
FoodRepository.markUsed(foodId)
```

These actions MUST NOT update recency:

- viewing/browsing a Food;
- searching;
- changing filter;
- editing;
- favoriting/unfavoriting;
- creating/saving a Food;
- Smart Food Entry save;
- remote lookup;
- barcode lookup.

Phase 17H implements the `markUsed` API but no normal production UI in this phase calls it.

Phase 20 will become the first normal production caller after successful `MealItem` persistence.

A normal user may therefore see an empty Recent filter until Phase 20 exists.

Do not fabricate Recent entries to make the Phase 17H UI look populated.

---

## In Scope

### Persistence

- Add `FoodEntity.isFavorite`.
- Add nullable `FoodEntity.favoriteAt`.
- Add nullable `FoodEntity.lastUsedAt`.
- Mirror both fields in the `Food` domain model.
- Preserve both fields through entity/domain mapping.
- Preserve both fields during edits.

### Room

- Bump database version from 7 to 8.
- Add explicit `MIGRATION_6_7`.
- Add explicit `MIGRATION_7_8` for `favoriteAt`.
- Preserve all existing Food rows.
- Remove reliance on `fallbackToDestructiveMigration()`.
- Centralize database creation so migration registration has one authoritative location.

### DAO / Repository

- Add ranked All-food queries.
- Add Favorites queries.
- Add Recent queries.
- Add search within each active scope.
- Add deterministic `setFavorite(foodId, isFavorite, favoriteAtMillis)`.
- Add deterministic `markUsed(foodId, usedAtMillis)`.

### Foods UI

- Add `All`.
- Add `Favorites`.
- Add `Recent`.
- Add favorite/unfavorite action to saved Food cards.
- Search within the selected filter.
- Add filter-aware summary copy.
- Add filter-aware empty states.

### Tests

- Migration preservation.
- DAO filtering/ranking.
- Favorite persistence.
- Explicit recency timestamp behavior.
- Entity/domain mapping.
- Repository behavior.
- ViewModel filter behavior.
- Existing Food CRUD regression.
- Fake DAO compatibility required by the expanded `FoodDao`.

---

## Authoritative Data Model

Phase 17H adds only:

```kotlin
@ColumnInfo(defaultValue = "0")
val isFavorite: Boolean = false

@ColumnInfo(defaultValue = "NULL")
val favoriteAt: Long? = null

@ColumnInfo(defaultValue = "NULL")
val lastUsedAt: Long? = null
```

No additional usage counters are required.

Do NOT add:

```text
useCount
remote favorite state
reference favorite state
barcode persistence
cloud favorite state
```

---

## Room Migration Requirement

Current Room database version:

```text
8
```

Target:

```text
8
```

Required migrations:

```sql
ALTER TABLE foods
ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0;

ALTER TABLE foods
ADD COLUMN lastUsedAt INTEGER DEFAULT NULL;
```

```sql
ALTER TABLE foods
ADD COLUMN favoriteAt INTEGER DEFAULT NULL;
```

Existing Food rows MUST survive the migration unchanged apart from receiving the new defaults:

```text
isFavorite = false
lastUsedAt = null
favoriteAt = null
```

`fallbackToDestructiveMigration()` must not be used. The supported paths
are v6 → v7 → v8 and v7 → v8; fresh installs create v8 directly.

Database creation should be centralized so `AppContainer.initialize(context)` delegates to the authoritative `GymLedgerDatabase` creation path instead of maintaining a second independent Room builder.

Do not expand this into a broader database architecture refactor.

---

## Ranking Rules

### All

```text
isFavorite DESC
favoriteAt DESC
lastUsedAt DESC
name COLLATE NOCASE ASC
id DESC
```

### Favorites

```text
WHERE isFavorite = 1

favoriteAt DESC
lastUsedAt DESC
name COLLATE NOCASE ASC
id DESC
```

### Recent

```text
WHERE lastUsedAt IS NOT NULL

lastUsedAt DESC
name COLLATE NOCASE ASC
id DESC
```

Search applies the name filter first and then uses the same ordering for the active filter.

`favoriteAt` affects only All and Favorites ordering. It never makes a
Food Recent; Recent depends only on `lastUsedAt IS NOT NULL`.

Existing `listAll()` and `searchByName()` behavior should remain available for existing consumers.

---

## Foods UX

Quick-access filters:

```text
[ All ] [ Favorites ] [ Recent ]
```

`All` is selected by default.

Search applies within the current filter.

The entire Foods page is one vertically scrollable layout. The inline
Foods heading, controls, search, summary, empty states, and food cards
share one page-level scroll flow without a nested lower-only list. The
Foods-specific inline heading is intentional and avoids recreating the
excessive fixed-space gap caused by a duplicate Foods TopAppBar.

Favorite control:

```text
saved Food card
→ explicit favorite/unfavorite action
```

Required empty states:

```text
All / no foods:
existing no-food state

All / search no match:
existing search-empty state

Favorites:
"No favorites yet. Star foods you use often."

Recent:
"No recent foods yet. Foods appear here after you log them in meals."
```

---

## Expected Production Files

Expected implementation scope:

```text
app/src/main/java/com/edu/gymledger/data/db/entity/FoodEntity.kt
app/src/main/java/com/edu/gymledger/domain/model/Food.kt
app/src/main/java/com/edu/gymledger/data/db/dao/FoodDao.kt
app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt
app/src/main/java/com/edu/gymledger/data/db/GymLedgerDatabase.kt
app/src/main/java/com/edu/gymledger/app/AppContainer.kt
app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsViewModel.kt
app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsScreen.kt
```

Test files may be added/extended as defined in `docs/IMPLEMENTATION_PLAN.md`.

---

## Explicitly Out of Scope

Do NOT add or modify:

- meal logging;
- `MealItem` behavior;
- quick-add to a meal;
- meal templates;
- copy previous meal/day;
- cloud sync;
- Worker behavior;
- USDA behavior;
- Open Food Facts behavior;
- barcode behavior;
- remote/reference favorites;
- unified remote/local search;
- camera scanner;
- new providers;
- new dependencies;
- future-phase functionality.

Do not modify:

```text
worker/**
app/src/main/java/com/edu/gymledger/data/remote/**
```

unless a real compile-only interface dependency is discovered and explicitly approved.

---

## AI Route

Planning / architecture:

```text
ChatGPT + GitHub — COMPLETE
```

Repository discovery / preflight:

```text
/cloud-mimo — COMPLETE
reviewed: PASS_WITH_CHANGES
```

Primary implementation:

```text
GPT-5.6 Luna Medium
```

Technical fallback:

```text
DeepSeek V4 Flash Max
/cloud-ds-max
```

Independent read-only review:

```text
/local-review
Qwen3.8 27B AWQ 5bpw + Lightning MTP
```

Final review:

```text
ChatGPT + GitHub
```

The user owns all commits and pushes.

Agents must not commit or push.

---

## Validation Gate

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
git diff --check
```

Scope checks:

```bash
git diff -- worker/
git diff -- app/src/main/java/com/edu/gymledger/data/remote/
```

Both scope-check diffs should be empty.

---

## Functional QA Strategy

Phase-specific functional QA only, plus minimal smoke regression.

Do not run the full MVP manual regression suite after Phase 17H.

Required real-device QA will cover:

- All / Favorites / Recent switching;
- favorite on/off;
- favorite persistence across restart;
- search within All;
- search within Favorites;
- filter-specific empty states;
- existing Food create/edit/delete;
- Smart Food Entry save smoke check;
- no crash/offline dependency.

Because Phase 20 is not implemented yet, positive Recent population is validated through automated DAO/repository/ViewModel tests rather than fake production usage.

---

## Completion Criteria

Phase 17H is complete only when:

```text
implementation matches docs/IMPLEMENTATION_PLAN.md
Room 6 → 7 → 8 migration preserves existing Foods
fresh install creates v8 directly
favorites persist correctly with favoriteAt semantics
recency API is explicit and deterministic
no Phase 17H production UI fabricates usage
All/Favorites/Recent UI works with correct ranking
search works inside active filters
existing Food CRUD remains functional
Smart Food Entry remains functional
Worker/remote code remains unchanged
full automated gate passes
functional QA passes
independent review passes
```
