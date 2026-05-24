# GymLedger — Master Task Roadmap

## How to Use This File

This file is the complete roadmap.

Do not replace this file after every commit.

The active implementation scope must live in:

```text
CURRENT_PHASE.md
```

For each phase:

1. Copy the matching phase block into `CURRENT_PHASE.md`.
2. Ask the local agent to implement only `CURRENT_PHASE.md`.
3. Run validation.
4. Commit.
5. Replace `CURRENT_PHASE.md` with the next phase.

## Global Agent Rules

The local agent must follow these rules:

1. Implement one phase at a time.
2. Do not start later phases.
3. Do not add backend, authentication, or cloud sync.
4. Do not add Hilt in v1.
5. Do not create a multi-module app in v1.
6. Keep the architecture simple.
7. Prefer working code over perfect abstractions.
8. Every phase must compile.
9. Every phase must be committable.
10. Stop when validation passes.
11. Do not fake completed functionality.
12. Do not present food photo estimates as accurate.
13. All user-facing UI text must be English.

## Global Validation

Fast build:

```bash
./gradlew assembleDebug
```

Full validation:

```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
```

Install APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Phases

### Phase 0 — Project Bootstrap

Create the base Android project.

Commit:

```text
chore: create Android project
```

### Phase 1 — Base Dependencies

Add Compose, Navigation Compose, Room, DataStore, Kotlinx Serialization, Lifecycle, and KSP.

Commit:

```text
chore: add base dependencies
```

### Phase 2 — App Shell and Theme

Create app shell, theme, and placeholder Dashboard.

Commit:

```text
feat: add app shell and theme
```

### Phase 3 — Main Navigation

Create app navigation and placeholder screens.

Commit:

```text
feat: add main navigation
```

### Phase 4 — Room Foundation

Create Room database, entities, DAOs, and manual AppContainer.

Commit:

```text
feat: add Room database foundation
```

### Phase 5 — Room Smoke Tests

Add in-memory Room tests.

Commit:

```text
test: add Room smoke tests
```

### Phase 6 — Exercise Repository

Create exercise repository and validation.

Commit:

```text
feat: add exercise repository
```

### Phase 7 — Exercises UI

Create exercise CRUD UI.

Commit:

```text
feat: add exercises CRUD UI
```

### Phase 8 — Workout Repository

Create workout session and set repository.

Commit:

```text
feat: add workout repository
```

### Phase 9 — Workout List UI

Create workout history and new workout flow.

Commit:

```text
feat: add workout list
```

### Phase 10 — Workout Detail and Sets UI

Create workout detail and set logging.

Commit:

```text
feat: add workout set logging
```

### Phase 11 — Routine Repository

Create routine repository.

Commit:

```text
feat: add routine repository
```

### Phase 12 — Routines UI

Create routine CRUD UI.

Commit:

```text
feat: add routines UI
```

### Phase 13 — Start Workout From Routine

Create workout session from routine.

Commit:

```text
feat: start workout from routine
```

### Phase 14 — Body Repository

Create body measurement repository.

Commit:

```text
feat: add body measurement repository
```

### Phase 15 — Body Measurements UI

Create body measurement UI.

Commit:

```text
feat: add body measurements UI
```

### Phase 16 — Food Repository

Create food repository.

Commit:

```text
feat: add food repository
```

### Phase 17 — Foods UI

Create food CRUD UI.

Commit:

```text
feat: add foods CRUD UI
```

### Phase 18 — Nutrition Repository

Create meal and meal item repository.

Commit:

```text
feat: add nutrition repository
```

### Phase 19 — Nutrition Day UI

Create daily nutrition screen.

Commit:

```text
feat: add daily nutrition UI
```

### Phase 20 — Meal Detail and Items UI

Create meal detail and meal item flow.

Commit:

```text
feat: add meal detail and items
```

### Phase 21 — Settings Repository

Create DataStore settings repository.

Commit:

```text
feat: add settings repository
```

### Phase 22 — Settings UI

Create settings screen.

Commit:

```text
feat: add settings UI
```

### Phase 23 — Real Dashboard Data

Connect dashboard to repositories.

Commit:

```text
feat: connect dashboard to real data
```

### Phase 24 — JSON Backup Models

Create Kotlinx Serialization backup models.

Commit:

```text
feat: add JSON backup models
```

### Phase 25 — JSON Export

Implement full JSON export.

Commit:

```text
feat: add JSON export
```

### Phase 26 — JSON Import

Implement full JSON import.

Commit:

```text
feat: add JSON import
```

### Phase 27 — CSV Parser

Implement CSV parser and error model.

Commit:

```text
feat: add CSV parser
```

### Phase 28 — CSV Export

Implement CSV exports.

Commit:

```text
feat: add CSV export
```

### Phase 29 — Simple CSV Import

Import exercises, foods, and body measurements.

Commit:

```text
feat: add simple CSV imports
```

### Phase 30 — Relational CSV Import

Import workout sessions, workout sets, meals, and meal items.

Commit:

```text
feat: add relational CSV imports
```

### Phase 31 — Import / Export UI

Expose import/export features in the app.

Commit:

```text
feat: add import export UI
```

### Phase 32 — Meal Photo Storage and UI

Support local meal photos.

Commit:

```text
feat: add meal photos
```

### Phase 33 — Food Photo Estimator

Create assisted food photo estimator interface and implementation.

Commit:

```text
feat: add assisted food photo estimator
```

### Phase 34 — Photo-Assisted Estimate UI

Create UI to save approximate meal items from photo + user input.

Commit:

```text
feat: add photo assisted meal estimates
```

### Phase 35 — Empty States and Validation Polish

Improve empty states and validation messages.

Commit:

```text
chore: polish empty states and validation
```

### Phase 36 — Final QA and Hardening

Run end-to-end QA and stabilize MVP.

Commit:

```text
chore: harden MVP
```

### Phase 37 — Post-MVP Backlog

Document future features.

Commit:

```text
docs: add post MVP backlog
```