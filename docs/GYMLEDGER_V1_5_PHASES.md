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


## Phase 42A — Exercise Visual Guide Foundation

### Objective

Add a reproducible, license-safe foundation for offline exercise illustrations using the open-source Workout Guide catalog.

### Product Quality Goal

GymLedger should gain a polished visual exercise layer without replacing its canonical exercise data, requiring a network connection, or introducing paid asset dependencies.

### Recommended AI Route

ChatGPT + GitHub for upstream/license/mapping plan; MiMo for repository discovery/preflight; Luna Medium or DeepSeek V4 Flash Max for implementation; Gemini for Android SVG/rendering validation; independent local review before final GitHub review.

### Tasks

- Pin an explicit Workout Guide upstream version or commit.
- Audit and document code-vs-asset licensing and required attribution.
- Import/read the upstream manifest in a deterministic tooling step.
- Build an explicit GymLedger exercise ID ↔ Workout Guide slug mapping.
- Generate a mapping report with matched, manual-review, and unmatched exercises.
- Bundle only approved/mapped visual assets needed by GymLedger.
- Preserve per-asset/source attribution metadata needed for redistribution.
- Add a small reusable Android abstraction for resolving an exercise visual by GymLedger exercise ID.
- Keep assets available fully offline after app installation.

### Upstream Constraints

Current Workout Guide release data should be treated as an external visual/metadata source, not as GymLedger's exercise source of truth.

The current upstream repository documents:

- 302 exercises.
- Three consistent frames per exercise.
- 906 transparent 512 × 512 SVG assets.
- Structured exercise metadata and attribution in its manifest.
- Code/documentation under MIT.
- Visual assets under CC BY-SA 4.0.
- Some original pose artwork derived from Everkinetic and expanded/normalized by Bryl Lim.

### Do Not Do

- Do not replace GymLedger exercise IDs or canonical exercise catalog with upstream IDs.
- Do not determine exercise identity from display name alone.
- Do not require CDN/network access at runtime.
- Do not dynamically fetch artwork during a workout.
- Do not import every upstream asset blindly before mapping/audit.
- Do not modify upstream artwork unless there is a concrete product need.
- Do not strip attribution/license metadata.
- Do not implement animation or in-workout coaching in this foundation phase.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Upstream version/commit is pinned and documented.
- Licensing and attribution requirements are explicitly documented in-repo.
- GymLedger retains its own exercise IDs and data model as source of truth.
- Mapping is explicit and deterministic.
- Mapping report identifies matched/manual-review/unmatched exercises.
- Representative mapped SVG assets render locally in Android.
- No network access is required to display a bundled exercise visual.
- Unmapped exercises degrade gracefully with the existing UI.
- Validation passes.
- Manual QA completed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Verify representative bodyweight, barbell, dumbbell, cable/machine, and stretch mappings.
- Verify an unmapped exercise still works without a visual.
- Put the device offline and verify bundled visuals still render.
- Verify attribution information is reachable from the app or bundled notices as designed.

### Suggested Commit

```text
feat: add exercise visual guide foundation
```

---

## Phase 42B — Exercise Detail Visuals

### Objective

Use the mapped Workout Guide assets to add clear offline technique visuals to GymLedger exercise detail/browse surfaces.

### Product Quality Goal

Exercise pages should look and feel substantially more polished while keeping visuals informative, lightweight, and optional.

### Recommended AI Route

Luna Medium for product-quality Compose implementation; Gemini for Android SVG/accessibility/rendering checks; local reviewer for independent regression review; ChatGPT + GitHub for final review.

### Tasks

- Show the mapped exercise visual on exercise detail.
- Present the three available frames in a clear ordered sequence.
- Add an accessible fallback when no mapped visual exists.
- Keep exercise name/equipment/muscle metadata owned by GymLedger.
- Surface required attribution without cluttering the primary workout UX.
- Ensure visuals respect dark/light appearance and phone-size layouts where practical.
- Keep the visual component reusable for later in-workout guidance.

### Do Not Do

- Do not turn frames into video.
- Do not add coaching claims or generated form advice.
- Do not hide core exercise information behind the visual.
- Do not require network/CDN access.
- Do not modify GymLedger exercise identity.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Mapped exercises show the correct visual sequence.
- Unmapped exercises remain fully usable.
- Visuals are available offline.
- Attribution is preserved.
- No exercise data is silently replaced by upstream metadata.
- Validation passes.
- Manual QA completed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Open several mapped exercise details and verify visual/exercise identity.
- Verify frame order is stable.
- Verify unmapped fallback.
- Verify offline rendering.
- Verify accessibility labels/touch targets for any visual controls.

### Suggested Commit

```text
feat: add exercise detail visuals
```

---

## Phase 42C — In-Workout Exercise Visual Guide

### Objective

Reuse the established exercise visual component during workout flows for quick technique reference.

### Product Quality Goal

A user should be able to confirm an exercise movement visually during a session without leaving the workout or waiting for remote media.

### Recommended AI Route

Luna Medium for workflow/UX integration; Gemini for Android lifecycle/rendering/accessibility checks; local independent review; ChatGPT + GitHub final review.

### Tasks

- Add a lightweight "View guide" entry point from the active workout/exercise flow.
- Reuse the same explicit GymLedger ID ↔ Workout Guide mapping.
- Show the three frames as a simple ordered guide or lightweight local sequence.
- Keep the active workout state intact when opening/closing the guide.
- Preserve offline behavior and attribution.
- Measure rendering/recomposition cost on realistic workout screens.

### Do Not Do

- Do not add streaming video.
- Do not add pose estimation or camera-based form analysis.
- Do not add AI coaching.
- Do not block logging when a visual is unavailable.
- Do not create a second exercise catalog.
- Do not require internet access.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Guide opens from the workout flow without losing workout state.
- Correct mapped exercise visual is shown.
- Unmapped exercises remain fully loggable.
- Guide works offline.
- No material regression in workout logging responsiveness.
- Attribution remains compliant.
- Validation passes.
- Manual QA completed.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Start a workout, open/close the visual guide, and continue logging.
- Repeat across several exercise/equipment types.
- Verify unmapped fallback.
- Verify airplane-mode/offline behavior.
- Verify rotation/background-return behavior if those flows are supported.

### Suggested Commit

```text
feat: add in-workout exercise visual guide
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
