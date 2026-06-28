# GymLedger — MVP Phases (Updated Detailed Roadmap)

This file is the detailed MVP roadmap. `docs/CURRENT_PHASE.md` remains the only active scope.

Global principle: local-first, offline-capable, online-assisted optional where explicitly scoped.

---

## Phase 0 — Project Bootstrap

### Objective

Create a clean Android project skeleton that builds and installs.

### Product Quality Goal

A boring, stable baseline with no architecture experiments.

### Recommended AI Route

Tier 3 Gemini Android Studio first option for Android project/tooling risk; local only for follow-up patches.

### Tasks

- Create Android app project with package com.edu.gymledger.
- Create MainActivity and simple placeholder screen.
- Confirm debug APK generation.

### Do Not Do

- Do not add backend/cloud/auth.
- Do not add Room/Navigation/DataStore.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Project opens in Android Studio.
- App launches without crash.
- Debug APK builds.

### Validation Commands

```bash
./gradlew clean assembleDebug
```

### Manual QA Checklist

- Install APK.
- Open app.
- Confirm placeholder screen.

### Suggested Commit

```text
chore: create Android project
```

---

## Phase 1 — Base Dependencies

### Objective

Add core Android dependencies required by the project.

### Product Quality Goal

Gradle sync must be clean and maintainable.

### Recommended AI Route

Tier 3 Gemini Android Studio first option; Codex review recommended if dependency alignment is risky.

### Tasks

- Add Compose, Navigation Compose, Room, KSP, DataStore, Lifecycle, Kotlinx Serialization.
- Keep versions aligned in version catalog.
- Verify clean Gradle sync/build.

### Do Not Do

- Do not create entities/screens/repositories.
- Do not add backend/cloud/auth.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Gradle sync passes.
- assembleDebug passes.
- No unnecessary dependencies added.

### Validation Commands

```bash
./gradlew clean assembleDebug
```

### Manual QA Checklist

- Not required beyond validation unless UI/runtime touched.

### Suggested Commit

```text
chore: add base dependencies
```

---

## Phase 2 — App Shell and Theme

### Objective

Create app shell, base theme, and Dashboard placeholder.

### Product Quality Goal

The app should feel like a real shell, not a raw template.

### Recommended AI Route

Tier 1 local builder; review with Qwen3.6 27B if needed.

### Tasks

- Create app root composable.
- Create theme.
- Create Dashboard placeholder.
- Keep bottom-level architecture simple.

### Do Not Do

- Do not add navigation graph.
- Do not add database work.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Dashboard placeholder opens.
- Theme applies consistently.
- assembleDebug passes.

### Validation Commands

```bash
./gradlew assembleDebug
```

### Manual QA Checklist

- Run app and confirm Dashboard placeholder.

### Suggested Commit

```text
feat: add app shell and theme
```

---

## Phase 3 — Main Navigation

### Objective

Create main navigation and placeholder screens.

### Product Quality Goal

Navigation should be stable and predictable for future features.

### Recommended AI Route

Tier 1 local builder; Gemini only for Navigation/Compose runtime issues.

### Tasks

- Add Dashboard, Workouts, Nutrition, Body, Settings tabs.
- Add secondary routes for Exercises, Routines, Foods, details, Import/Export.
- Wire placeholders.

### Do Not Do

- Do not implement feature logic.
- Do not create database work.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Bottom nav works.
- Back behavior is reasonable.
- Placeholders compile.

### Validation Commands

```bash
./gradlew assembleDebug
```

### Manual QA Checklist

- Tap every bottom tab.
- Open any secondary routes wired.

### Suggested Commit

```text
feat: add main navigation
```

---

## Phase 4 — Room Foundation

### Objective

Create Room entities, DAOs, database, and manual AppContainer.

### Product Quality Goal

Schema should support MVP fields enough to avoid avoidable refactors.

### Recommended AI Route

Tier 3 Gemini Android Studio first option for Room/KSP; local builder for entities/tests.

### Tasks

- Create entities for exercises, workouts, sets, routines, foods, meals, meal items, body measurements.
- Create DAOs and GymLedgerDatabase.
- Create manual AppContainer.
- Add indices/foreign keys where appropriate.

### Do Not Do

- Do not create UI CRUD.
- Do not add migrations unless necessary.
- Do not add Hilt.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Entities and DAOs compile.
- KSP passes.
- App still opens.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Not required beyond validation unless runtime touched.

### Suggested Commit

```text
feat: add Room database foundation
```

---

## Phase 5 — Room Smoke Tests

### Objective

Verify Room works with local JVM tests.

### Product Quality Goal

Test foundation should prevent fragile DB work later.

### Recommended AI Route

Tier 1 local builder; review if tests are flaky.

### Tasks

- Create Robolectric local JVM in-memory Room tests.
- Test exercise insert/read/delete.
- Close DB after tests.

### Do Not Do

- Do not create UI.
- Do not use androidTest unless needed.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- testDebugUnitTest passes.
- In-memory Room test is meaningful.
- assembleDebug passes.

### Validation Commands

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```

### Manual QA Checklist

- Not required beyond validation.

### Suggested Commit

```text
test: add Room smoke tests
```

---

## Phase 6 — Exercise Repository

### Objective

Create exercise repository and validation.

### Product Quality Goal

Simple but real data layer; no fake production repository.

### Recommended AI Route

Tier 1 local builder.

### Tasks

- Create Exercise domain model if needed.
- Create ExerciseRepository backed by ExerciseDao.
- Validate name required/not blank/trimmed.
- Add repository tests.

### Do Not Do

- Do not create UI.
- Do not over-abstract with use cases.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Repository uses real DAO.
- Validation is tested.
- CRUD works through repository.

### Validation Commands

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```

### Manual QA Checklist

- Not required beyond validation.

### Suggested Commit

```text
feat: add exercise repository
```

---

## Phase 7A — Exercises CRUD Foundation

### Objective

Finish base Exercises CRUD UI.

### Product Quality Goal

Functional correctness first: create/list/edit/delete/validation/persistence.

### Recommended AI Route

Tier 1 local builder; UX/product pass mandatory.

### Tasks

- Ensure Exercises entry exists.
- Implement list, empty state, add/edit, delete confirmation.
- Fix tap/long-press behavior.
- Ensure ViewModel lifecycle is correct.

### Do Not Do

- Do not polish into large premium UI in this subphase.
- Do not touch workouts/routines/nutrition/body.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- User can create/edit/delete exercise.
- Empty name rejected.
- Data persists after restart.
- App builds.

### Validation Commands

```bash
./gradlew testDebugUnitTest && ./gradlew assembleDebug
```

### Manual QA Checklist

- Open Workouts → Exercises.
- Create Bench Press.
- Edit it.
- Restart and verify.
- Delete it.

### Suggested Commit

```text
feat: add exercises CRUD UI
```

---

## Phase 7B — Exercises Premium UX Pass

### Objective

Upgrade Exercises from CRUD form to fast premium exercise library flow.

### Product Quality Goal

Exercise creation should feel close to paid workout apps: quick presets, low friction, clean add/edit.

### Recommended AI Route

Tier 2 OpenCode Go recommended; local allowed for small patches.

### Tasks

- Replace oversized form with mobile-friendly sheet/full-screen form.
- Add local exercise presets.
- Add preset picker/search.
- Selecting preset pre-fills metadata.
- Use chips/dropdowns where practical.
- Collapse optional fields under More details.
- Improve empty state.
- Preserve manual custom exercise creation.

### Do Not Do

- Do not add external exercise database.
- Do not add exercise images/videos.
- Do not touch workouts/routines/nutrition/body.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Common exercise can be created in under 10 seconds.
- Manual custom exercise still works.
- Preset selection pre-fills fields.
- UI no longer feels generated CRUD.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Open Exercises.
- Tap Add.
- Select Bench Press preset.
- Save.
- Edit and verify fields.
- Restart and verify.

### Suggested Commit

```text
feat: polish exercises UX
```

---

## Phase 8 — Workout Repository

### Objective

Create workout session and workout set repository.

### Product Quality Goal

Workout data must be reliable before UI logging exists.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Create WorkoutRepository.
- Support session CRUD and set CRUD.
- Validate reps, weight, RPE, RIR.
- Test cascade/relationships.

### Do Not Do

- Do not create UI.
- Do not add progression algorithms.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add workout repository
```

---

## Phase 9 — Workouts History UI

### Objective

Create workout history/list UI.

### Product Quality Goal

Workouts tab should show useful workout history, not a placeholder.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Show sessions list.
- Add empty state.
- Create new workout session.
- Open workout detail.

### Do Not Do

- Do not implement set logging if Phase 10 owns it.
- Do not add charts.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add workouts history UI
```

---

## Phase 10 — Workout Detail and Set Logging

### Objective

Add workout detail UI and set entry.

### Product Quality Goal

Set logging must be fast enough for real gym use even if not fully premium yet.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Show workout detail.
- Add/edit/delete sets.
- Validate reps/weight/RPE/RIR.
- Preserve set ordering.

### Do Not Do

- Do not add timers yet.
- Do not add progressive overload.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add workout set logging
```

---

## Phase 11 — Routine Repository

### Objective

Create routines/repository layer.

### Product Quality Goal

Routine data must reliably support later start-workout flow.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Create Routine domain/repository.
- Create RoutineExercise repository if needed.
- Validate names/targets/order.
- Add tests.

### Do Not Do

- Do not create UI.
- Do not start workouts from routines yet.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add routine repository
```

---

## Phase 12 — Routines UI

### Objective

Create routines CRUD UI.

### Product Quality Goal

Routine creation should be practical and not a raw table.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- List routines.
- Add/edit/delete routines.
- Add exercises to routine.
- Show target sets/reps if supported.

### Do Not Do

- Do not implement start workout if Phase 13 owns it.
- Do not add progression algorithms.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add routines UI
```

---

## Phase 13 — Start Workout From Routine

### Objective

Allow starting a workout from a routine.

### Product Quality Goal

Routine should reduce repeated setup.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Create workout session from routine.
- Snapshot routine exercises into workout.
- Show planned exercises.
- Preselect exercise when adding set.

### Do Not Do

- Do not mutate routine history.
- Do not fake sets.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: start workout from routine
```

---

## Phase 14 — Body Measurement Repository

### Objective

Create body measurement repository and validation.

### Product Quality Goal

Body data must be reliable before UI.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Create BodyMeasurement domain/repository if needed.
- Add latest/all queries.
- Validate weight/measurements.
- Add tests.

### Do Not Do

- Do not create UI.
- Do not add charts.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add body measurement repository
```

---

## Phase 15 — Body Measurements UI

### Objective

Create body measurements UI.

### Product Quality Goal

Body tracking should be quick and polished, not raw CRUD.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Latest weight card.
- History list.
- Add/edit/delete measurement.
- Optional measurements grouped.
- Inline validation.

### Do Not Do

- Do not add charts.
- Do not add progress photos.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add body measurements UI
```

---

## Phase 16 — Food Repository

### Objective

Create FoodRepository with CRUD/search/macros.

### Product Quality Goal

Food data must support downstream meal macro calculations.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Add Food domain if needed.
- Add repository CRUD/search.
- Validate name/calories/serving/macros.
- Add tests.

### Do Not Do

- Do not create UI.
- Do not add food API/barcode.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add food repository
```

---

## Phase 17 — Foods UI

### Objective

Create offline-capable Foods UI for saved custom foods.

### Product Quality Goal

Food entry should feel faster and cleaner than a spreadsheet.

### Recommended AI Route

Tier 2 OpenCode Go recommended for product-critical UI; Tier 1 local for repository/test phases.

### Tasks

- Foods list/search.
- Add/edit/delete food.
- Macro chips.
- Empty/search-empty states.
- Inline validation.

### Do Not Do

- Do not add backend.
- Do not add external food database.
- Do not add barcode.
- Do not add meal logging.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Active phase behavior works.
- Invalid values rejected.
- Data persists where applicable.
- Build/tests pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the visible flow if UI changed.
- Restart app and verify persistence when applicable.

### Suggested Commit

```text
feat: add foods UI
```

---

## Phase 17A — Product Platform Strategy Update

### Objective

Update docs to officially allow optional online-assisted features while preserving local-first/offline-capable behavior.

### Product Quality Goal

Roadmap and agent rules must stop contradicting the new product direction.

### Recommended AI Route

ChatGPT plan + local docs model; review with Qwen3.6 27B.

### Tasks

- Replace local-only wording.
- Add online-assisted rules.
- Add backend/cloud phase guide.
- Update MVP/v1.5/v2 roadmap.
- Update AGENTS/AI workflow.

### Do Not Do

- Do not modify app code.
- Do not create Worker.
- Do not add dependencies.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
git diff --check
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
docs: update platform strategy
```

---

## Phase 17B — Settings Foundation for Online Assistance

### Objective

Add Android settings/data foundation for optional online lookup before network integration.

### Product Quality Goal

Online lookup must be configurable and disabled by default, never hardcoded.

### Recommended AI Route

Tier 1 local builder; Gemini only for DataStore/runtime issues.

### Tasks

- Add DataStore settings for online lookup enabled.
- Add endpoint URL setting.
- Add personal API key setting.
- Add USDA/Open Food Facts toggles.
- Add safe mode setting.
- Add Settings UI section.
- No network calls.

### Do Not Do

- Do not call network.
- Do not add Worker.
- Do not add OkHttp/Retrofit.
- Do not add barcode.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add online assistance settings
```

---

## Phase 17C — Smart Food Entry Local Foundation

### Objective

Add local smart food entry with reference foods and macro calculator.

### Product Quality Goal

User should calculate common foods like 10 eggs without knowing macros.

### Recommended AI Route

OpenCode Go for UX; local builder for calculator/tests.

### Tasks

- Add local reference food model/source.
- Add EN/ES aliases.
- Add per-100g and unit calculator.
- Show calculated preview.
- Save calculated result via FoodRepository.
- Label approximate values.

### Do Not Do

- Do not call backend.
- Do not call external APIs.
- Do not add barcode.
- Do not add meal logging.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add smart food entry foundation
```

---

## Phase 17D — Cloudflare Worker Foundation

### Objective

Create TypeScript Worker skeleton for GymLedger Food Lookup Gateway.

### Product Quality Goal

Backend foundation should be small, testable, cheap, and not store personal data.

### Recommended AI Route

Qwen Coder/Codex for TypeScript; ChatGPT architecture review.

### Tasks

- Create worker/food-lookup project.
- Add Wrangler config.
- Add /v1/health.
- Add /v1/config.
- Add API key middleware.
- Add structured JSON/error helpers.
- Add README.

### Do Not Do

- Do not call USDA.
- Do not call Open Food Facts.
- Do not add Android integration.
- Do not store personal data.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
npm run typecheck && npm test && npm run dev
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add food lookup worker foundation
```

---

## Phase 17E — Worker Food Providers and Cache

### Objective

Implement USDA and Open Food Facts behind Worker with cache and budgets.

### Product Quality Goal

Provider lookup should be normalized, cached, attributed, and free-tier safe.

### Recommended AI Route

Qwen Coder/Codex; DeepSeek Pro review for provider/rate-limit logic.

### Tasks

- Add normalized DTOs.
- Add USDA provider.
- Add Open Food Facts provider.
- Add D1 cache schema.
- Add usage_daily and budget guard.
- Add provider timeouts.
- Add /foods/generic/search/barcode endpoints.

### Do Not Do

- Do not add user accounts.
- Do not store personal meal/workout data.
- Do not add paid providers.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
npm run typecheck && npm test && npm run dev
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add food lookup providers
```

---

## Phase 17F — Android Remote Food Lookup Integration

### Objective

Connect Android to Worker while preserving offline/manual behavior.

### Product Quality Goal

Online lookup should feel helpful, optional, cacheable, and editable.

### Recommended AI Route

OpenCode Go for UI integration; Gemini for Android networking/runtime issues.

### Tasks

- Add OkHttp client if approved.
- Add remote DTOs/source.
- Add local lookup cache.
- Add FoodLookupRepository orchestration.
- Use settings endpoint/key.
- Show source/approximate badges.
- Save selected results locally.

### Do Not Do

- Do not call providers directly from Android.
- Do not require internet.
- Do not require login.
- Do not add meal logging.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add online food lookup
```

---

## Phase 17G — Manual Barcode Lookup

### Objective

Support typed/pasted barcode lookup before camera scanner.

### Product Quality Goal

Barcode value should be useful without Android camera complexity.

### Recommended AI Route

OpenCode Go for UI; local builder for tests/cache.

### Tasks

- Add barcode search action.
- Lookup local/cache/Worker.
- Show result review.
- Save mapped food.
- Unknown barcode fallback to manual create.

### Do Not Do

- Do not add CameraX scanner.
- Do not require internet.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add manual barcode food lookup
```

---

## Phase 17H — Food Recents and Favorites

### Objective

Add recents/favorites to reduce repeated nutrition entry.

### Product Quality Goal

Frequent foods should be accessible in seconds.

### Recommended AI Route

OpenCode Go for UX; local builder for repository/tests.

### Tasks

- Track recently used foods.
- Allow favorites.
- Rank search results by saved/recent/favorite/reference/online.
- Add quick add actions.

### Do Not Do

- Do not add full meal templates.
- Do not add copy previous day.
- Do not add cloud sync.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add food recents and favorites
```

---

## Phase 18 — Nutrition Repository

### Objective

Create meal/meal item repository with macro totals using saved/calculated/lookup foods.

### Product Quality Goal

Daily nutrition math must be reliable before UI.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Meal CRUD.
- Meal item CRUD.
- Daily totals.
- Manual override support.
- Source/approximate metadata if schema active.
- Tests.

### Do Not Do

- Do not create UI.
- Do not call remote providers directly.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add nutrition repository
```

---

## Phase 19 — Nutrition Day UI

### Objective

Create daily nutrition UI with calories/macros and meal list.

### Product Quality Goal

Nutrition tab should show real daily progress and fast entry.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Today summary.
- Macro progress.
- Meal list.
- Quick add meal/item entry.
- Empty states.

### Do Not Do

- Do not implement photo estimate.
- Do not add charts unless current phase asks.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add daily nutrition UI
```

---

## Phase 20 — Meal Detail and Items UI

### Objective

Create meal detail and item logging UI.

### Product Quality Goal

Meal logging should avoid manual macro entry whenever possible.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Add item from saved/recent/favorite.
- Add item from smart/reference food.
- Add online lookup result if enabled.
- Add manual item.
- Quantity editor.
- Editable calories/macros.

### Do Not Do

- Do not add camera/photo estimate.
- Do not add cloud sync.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add meal item logging
```

---

## Phase 21 — Settings Repository

### Objective

Expand settings repository/data model.

### Product Quality Goal

Settings should centralize units, goals, online-assistance config, and defaults.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Add unit/theme/macro goal settings.
- Keep online assistance settings.
- Validate defaults.
- Add tests if pattern exists.

### Do Not Do

- Do not add sync/accounts.
- Do not call network.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: expand settings repository
```

---

## Phase 22 — Settings UI

### Objective

Create/polish full settings UI.

### Product Quality Goal

Settings should be clear, safe, and not expose confusing technical defaults.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Units/theme.
- Macro goals.
- Online assistance section polish.
- Cache controls.
- Import/export entry.

### Do Not Do

- Do not implement import/export logic if later phases own it.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add full settings UI
```

---

## Phase 23 — Real Dashboard Data

### Objective

Connect Dashboard to real data.

### Product Quality Goal

Dashboard should show useful daily state, not placeholders.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Today calories/macros.
- Latest workout.
- Latest body weight.
- Quick actions.
- Active workout if any.

### Do Not Do

- Do not add advanced analytics.
- Do not add charts unless scoped.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: connect dashboard to real data
```

---

## Phase 24 — JSON Backup Export

### Objective

Export user data to JSON backup.

### Product Quality Goal

User must be able to protect data locally.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Define backup DTOs.
- Export exercises/workouts/routines/foods/meals/body/settings.
- Exclude secrets.
- Add tests.

### Do Not Do

- Do not export API keys.
- Do not export raw provider cache by default.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: export JSON backup
```

---

## Phase 25 — JSON Backup Import

### Objective

Import JSON backup safely.

### Product Quality Goal

Import must not corrupt existing data.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Validate schemaVersion.
- Parse backup.
- Import transactionally.
- Show readable errors.
- Add tests.

### Do Not Do

- Do not partially corrupt data.
- Do not import secrets.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: import JSON backup
```

---

## Phase 26 — CSV Export

### Objective

Export core data to CSV.

### Product Quality Goal

CSV should help personal analysis and portability.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Export supported tables/formats.
- Use headers.
- Use dot decimals.

### Do Not Do

- Do not implement import in this phase.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: export CSV data
```

---

## Phase 27 — CSV Import

### Objective

Import supported CSV formats.

### Product Quality Goal

Initial imports should be safe and readable.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Validate headers.
- Import foods/exercises where scoped.
- Report invalid rows.
- Avoid corrupting existing data.

### Do Not Do

- Do not import unsupported huge databases yet.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: import CSV data
```

---

## Phase 28 — Import Export UI

### Objective

Create UI for import/export.

### Product Quality Goal

Backup should be accessible without feeling dangerous.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Export buttons.
- Import picker.
- Warnings/confirmation.
- Result summary.

### Do Not Do

- Do not add cloud backup.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add import export UI
```

---

## Phase 29 — Backup QA and Data Integrity

### Objective

Harden backup/import/export.

### Product Quality Goal

Data protection must be trustworthy.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Roundtrip tests.
- Manual backup/restore QA.
- Invalid file QA.

### Do Not Do

- Do not add new feature scope.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
test: harden backup restore
```

---

## Phase 30 — Meal Photo Storage

### Objective

Add local meal photo storage.

### Product Quality Goal

Photos must stay local and manageable.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Select/take photo if scoped.
- Store URI/file metadata.
- Attach to meal.

### Do Not Do

- Do not upload photos.
- Do not add AI estimate.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add meal photo storage
```

---

## Phase 31 — Meal Photo UI

### Objective

Show and manage meal photos.

### Product Quality Goal

Photo feature should be simple and safe.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- View photo in meal detail.
- Remove photo.
- Handle missing file.

### Do Not Do

- Do not add AI estimate.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add meal photo UI
```

---

## Phase 32 — Photo-Assisted Estimate Foundation

### Objective

Add basic approximate estimate foundation.

### Product Quality Goal

Estimates must be clearly approximate and editable.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Create estimate model/service.
- Use description/known foods where possible.
- Label approximate.

### Do Not Do

- Do not require paid AI.
- Do not upload photos by default.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add meal estimate foundation
```

---

## Phase 33 — Estimate UI

### Objective

Add UI for approximate editable estimates.

### Product Quality Goal

The estimate flow should help, not pretend certainty.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Show estimate preview.
- Allow edit before save.
- Show source/approx label.

### Do Not Do

- Do not auto-save estimates.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
feat: add editable meal estimates
```

---

## Phase 34 — Estimate QA

### Objective

Harden estimate edge cases.

### Product Quality Goal

Wrong estimates must be recoverable and editable.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Manual QA.
- Invalid/missing input states.
- Persistence checks.

### Do Not Do

- Do not add new providers.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
test: harden meal estimates
```

---

## Phase 35 — Empty States and Validation Polish

### Objective

Polish empty/error/validation states app-wide.

### Product Quality Goal

Core flows should guide action clearly.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Audit empty states.
- Audit validation errors.
- Audit offline/online lookup states.
- Fix obvious UX gaps.

### Do Not Do

- Do not add new features.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
fix: polish empty states and validation
```

---

## Phase 36 — Final MVP Hardening

### Objective

Harden MVP for real use.

### Product Quality Goal

App should survive daily personal use.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Full regression.
- Offline/online-disabled tests.
- Import/export QA.
- Nutrition/workout/body QA.

### Do Not Do

- Do not add features.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
chore: harden MVP
```

---

## Phase 37 — MVP Release Candidate

### Objective

Prepare release candidate APK.

### Product Quality Goal

MVP should be installable and trustworthy.

### Recommended AI Route

Tier depends on risk: local for data/tests, OpenCode Go for premium UI, Gemini for Android platform, Codex for final review.

### Tasks

- Build release candidate.
- Final checklist.
- Known issues list.

### Do Not Do

- Do not add features.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within active scope.
- Validation passes.
- Manual QA completed when UI/runtime changed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific manual QA.
- Restart app when persistence is involved.

### Suggested Commit

```text
chore: prepare MVP release candidate
```
