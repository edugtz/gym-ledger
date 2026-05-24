# CURRENT_PHASE.md — Phase 2: App Shell and Theme

## Objective

Create a minimal app shell with Material 3 theme, a visible top app bar, and a Dashboard placeholder.

## Tasks

- Configure Material 3 theme (Color, Type, Theme).
- Create `GymLedgerApp` using `Scaffold`.
- Add `TopAppBar` with title "GymLedger".
- Create `DashboardScreen` placeholder with "Dashboard" text.
- Ensure content is not clipped behind status bar.
- Update `MainActivity` to use the new shell and theme.

## Do Not Do

- Do not add Navigation Compose yet.
- Do not add Room, DataStore, or Repositories.
- Do not add features or real screens.
- Do not add backend/auth/cloud sync.
- Do not add Hilt or multi-module architecture.

## Acceptance Criteria

- Project compiles.
- Material 3 theme is applied.
- Top App Bar with "GymLedger" is visible.
- Dashboard placeholder is visible.
- Content is correctly padded (not behind status bar).
- No errors in lint or tests.

## Validation Commands

```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
```

## Suggested Commit

```text
feat: add app shell and theme
```
