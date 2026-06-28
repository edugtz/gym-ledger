# GymLedger v1.5 Plan — Paid-App Feel Without Paid Dependencies

This plan assumes MVP includes basic smart food entry, online food lookup, manual barcode lookup, and food recents/favorites. v1.5 focuses on polish, speed, reuse, insights, and hardening.

---

## Phase 38 — Workout Quick Log UX

### Objective

Make workout logging fast enough for real use during a gym session.

### Product Quality Goal

Fast set entry matters more than pretty CRUD.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Add quick add-set flow.
- Pre-fill exercise when appropriate.
- Compact set entry.
- Support inline edit of recent set.

### Do Not Do

- Do not add progression algorithms.
- Do not add charts.
- Do not rewrite repository.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: improve workout quick logging
```

---

## Phase 39 — Copy Previous Workout

### Objective

Reduce repeated logging by copying a previous workout.

### Product Quality Goal

Common workout repetition should be one tap, not manual recreation.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Duplicate previous session and sets.
- Use new date/time.
- Allow editing copied sets.
- Preserve original.

### Do Not Do

- Do not mutate history.
- Do not auto-progress.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: copy previous workout
```

---

## Phase 40 — Rest Timer and Workout Timer

### Objective

Add simple local timers for workout flow.

### Product Quality Goal

Timer should help while app is open without background complexity.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Elapsed workout timer.
- Rest timer after set.
- Start/pause/reset rest timer.

### Do Not Do

- Do not add foreground service.
- Do not add notifications yet.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: add workout rest timer
```

---

## Phase 41 — Exercise History

### Objective

Show historical data for selected exercise.

### Product Quality Goal

Exercise page should immediately tell the user what they did last time.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Query previous sets.
- Show last performed date.
- Show latest weight/reps.
- Show compact history.

### Do Not Do

- Do not add charts yet.
- Do not add PR detection yet.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: add exercise history
```

---

## Phase 42 — PR and Estimated 1RM

### Objective

Add local PR and estimated 1RM insights.

### Product Quality Goal

Progress should be visible without manual spreadsheet analysis.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Calculate rep PRs.
- Calculate weight PRs.
- Estimate 1RM with simple formula.
- Show clear labels.

### Do Not Do

- Do not add coaching AI.
- Do not add cloud.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: add exercise PRs
```

---

## Phase 43 — Routine Duplication and Templates

### Objective

Make routine creation faster through duplication/templates.

### Product Quality Goal

Repeated setup should take seconds.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Duplicate routine.
- Rename duplicated routine.
- Preserve exercise order/targets.
- Add simple local templates if scoped.

### Do Not Do

- Do not add online template catalog.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: duplicate routines
```

---

## Phase 44 — Dashboard Insights

### Objective

Upgrade dashboard with helpful local insights.

### Product Quality Goal

Dashboard should feel like a paid app home screen.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Weekly workout count.
- Macro adherence summary.
- Latest body trend.
- Quick actions.

### Do Not Do

- Do not add advanced charts.
- Do not add cloud analytics.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: add dashboard insights
```

---

## Phase 45 — Nutrition Quick Add UX

### Objective

Make meal/food logging faster from Nutrition screens.

### Product Quality Goal

Common nutrition actions should take seconds.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Quick add from recent/favorite.
- Fast quantity entry.
- One-tap add to meal where safe.

### Do Not Do

- Do not add new providers.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: improve nutrition quick add
```

---

## Phase 46 — Meal Templates

### Objective

Add meal templates for repeated meals.

### Product Quality Goal

Frequent meals should not be recreated manually.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Create meal template.
- Apply template to day/meal.
- Edit template.

### Do Not Do

- Do not add cloud template sync.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: add meal templates
```

---

## Phase 47 — Copy Previous Meal or Day

### Objective

Allow copying previous meal/day.

### Product Quality Goal

Real nutrition tracking repeats; the app should exploit that.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Copy meal.
- Copy previous day items.
- Allow edits after copy.

### Do Not Do

- Do not mutate original.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: copy previous meals
```

---

## Phase 48 — Body Trends

### Objective

Show simple body trends.

### Product Quality Goal

Body data should become insight, not just history.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Weight trend.
- Measurement trend summaries.
- Simple local calculations.

### Do Not Do

- Do not add progress photos here.
- Do not add Health Connect.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: add body trends
```

---

## Phase 49 — Search and Filter Polish

### Objective

Improve search/filter across app.

### Product Quality Goal

Large local data should stay usable.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Search/filter workouts/exercises/foods.
- Ranking improvements.
- Persist useful filter state.

### Do Not Do

- Do not add external search service.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
feat: polish search and filters
```

---

## Phase 50 — Accessibility Polish

### Objective

Improve accessibility and touch ergonomics.

### Product Quality Goal

App should be comfortable and inclusive.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Content descriptions.
- 48dp touch targets.
- Labels not placeholders only.
- Contrast review.

### Do Not Do

- Do not redesign whole app.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
fix: improve accessibility
```

---

## Phase 51 — Performance and Data Integrity Hardening

### Objective

Harden app under realistic local data.

### Product Quality Goal

Personal app should remain fast and reliable.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Test larger local datasets.
- Optimize slow queries.
- Review transactions.
- Check recomposition hotspots.

### Do Not Do

- Do not add caching frameworks.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
chore: harden performance
```

---

## Phase 52 — Backup Restore Polish

### Objective

Make backup/restore safer and clearer.

### Product Quality Goal

User trust depends on recoverable data.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Better summaries.
- Safer confirmations.
- Roundtrip QA.
- Error messages.

### Do Not Do

- Do not add cloud backup.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
fix: polish backup restore
```

---

## Phase 53 — v1.5 Release Hardening

### Objective

Prepare v1.5 release candidate.

### Product Quality Goal

v1.5 should feel notably more polished than MVP.

### Recommended AI Route

Tier 2 OpenCode Go for product-critical UX; local builder for data/tests; Gemini for Android platform; Codex for final release review.

### Tasks

- Full regression.
- Known issues.
- Release APK.
- Final review.

### Do Not Do

- Do not add features.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature works within scope.
- Validation passes.
- Manual QA completed.
- No future work included.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run the core user flow.
- Restart app for persistence-sensitive changes.

### Suggested Commit

```text
chore: prepare v1.5 release
```
