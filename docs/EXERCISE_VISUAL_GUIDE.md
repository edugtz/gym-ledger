# GymLedger — Exercise Visual Guide Architecture

## Status

**Post-MVP design / discovery document**

This document defines the intended architecture for integrating an external open-source exercise illustration catalog into GymLedger.

Initial upstream evaluated:

```text
Repository: https://github.com/bryllim/workout-guide
Package: @bryllim/workout-guide
Initial evaluated package version: 1.0.0
```

The exact upstream tag/commit used by GymLedger MUST be pinned during implementation of the foundation phase. Do not consume a moving `main` branch in production.

---

## 1. Objective

Add a polished offline visual guide for exercises without replacing GymLedger's canonical exercise catalog or making exercise logging depend on network access.

Desired long-term flow:

```text
GymLedger Exercise
        │
        ├── canonical GymLedger ID / metadata / history
        │
        └── optional explicit visual mapping
                     │
                     ▼
           Workout Guide exercise slug
                     │
                     ▼
               3 visual frames
                     │
                     ▼
       Exercise detail / active workout guide
```

Workout Guide is a **visual enrichment source**, not GymLedger's source of truth for exercises.

---

## 2. Product Principles

The integration must preserve GymLedger's existing principles:

```text
local-first
offline-capable
explicitly mapped
deterministic
license-compliant
non-blocking when a visual is unavailable
```

Core workout logging must continue to work when:

- an exercise has no visual mapping;
- an asset fails to render;
- Workout Guide is unavailable upstream;
- the device is offline.

No visual asset is allowed to become a hard dependency for saving or performing a workout.

---

## 3. Upstream Reference

Initial evaluated upstream:

```text
bryllim/workout-guide
```

The project currently documents:

```text
302 exercises
3 frames per exercise
906 SVG assets
512 × 512 transparent visual frames
structured manifest metadata
framework-neutral package API
```

Canonical upstream locations:

```text
packages/workout-guide/manifest.json
packages/workout-guide/assets/<slug>/frame-1.svg
packages/workout-guide/assets/<slug>/frame-2.svg
packages/workout-guide/assets/<slug>/frame-3.svg
```

Relevant upstream package metadata:

```text
@bryllim/workout-guide
```

The npm package is useful as documentation/reference for the catalog structure, but GymLedger Android SHOULD NOT depend on Node/npm at runtime.

---

## 4. Upstream Data Model

The upstream catalog exposes exercise metadata conceptually equivalent to:

```text
id
slug
name
exerciseType
equipment
primaryMuscle
secondaryMuscles[]
isStretch
frames[3]
attribution
```

Each frame includes:

```text
index
path
width
height
format
attribution
```

This metadata can assist discovery and mapping, but GymLedger must never silently replace its own exercise metadata with upstream values.

---

## 5. Licensing

Workout Guide separates software and visual licenses.

### Code and documentation

```text
MIT
```

### Visual assets

```text
CC BY-SA 4.0
```

### Upstream provenance

Some original pose artwork comes from Everkinetic and was expanded/normalized by Bryl Lim.

Required upstream license/attribution documents include:

```text
LICENSE
LICENSE-ASSETS
LICENSES.md
ATTRIBUTION.md
```

### GymLedger obligations

Before shipping visual assets, GymLedger must:

- preserve required attribution;
- link to or bundle the applicable CC BY-SA 4.0 license information;
- identify Bryl Lim as required by upstream attribution;
- preserve Everkinetic attribution where applicable;
- indicate modifications if GymLedger changes an asset;
- treat modified/adapted visual assets as subject to the applicable ShareAlike requirement;
- keep visual-asset licensing separate from GymLedger's application/source-code licensing.

Do not assume that the MIT license for source code applies to the artwork.

---

## 6. Source-of-Truth Rule

GymLedger remains authoritative for:

```text
exercise ID
exercise name
exercise aliases
exercise type
equipment
muscle metadata
routine membership
workout history
sets/reps/load
user-created exercises
```

Workout Guide may provide:

```text
visual frames
visual attribution
mapping hints
optional metadata useful for review
```

Never use upstream display names as the permanent identity relation.

Bad:

```text
GymLedger name == Workout Guide name
```

Good:

```text
GymLedgerExerciseId -> explicit WorkoutGuideSlug
```

---

## 7. Explicit Mapping

Create a version-controlled mapping owned by GymLedger.

Example conceptual format:

```json
{
  "bench_press": "bench-press",
  "barbell_back_squat": "barbell-squat",
  "dumbbell_biceps_curl": "bicep-curl"
}
```

The actual format may be JSON, Kotlin-generated data, CSV, or another deterministic representation selected during implementation.

Requirements:

- GymLedger exercise ID is the key.
- Workout Guide slug is the external visual reference.
- mappings are reviewable in git;
- no runtime fuzzy matching;
- no silent automatic remapping after upstream updates;
- aliases may assist discovery but never become final identity by themselves.

---

## 8. Import Architecture

Preferred architecture:

```text
Pinned Workout Guide version/commit
              │
              ├── manifest.json
              └── assets/
                     │
                     ▼
          GymLedger import tooling
                     │
       ┌─────────────┼──────────────┐
       │             │              │
       ▼             ▼              ▼
 validate       map exercises   validate license/
 upstream                         attribution
       │             │              │
       └─────────────┼──────────────┘
                     ▼
              import report
                     │
                     ▼
            approved mappings
                     │
                     ▼
          copy/convert selected
                visual assets
                     │
                     ▼
          Android bundled assets
```

The importer should be deterministic and re-runnable.

---

## 9. Importer Responsibilities

A future import tool should:

1. Verify the expected upstream version/commit.
2. Read `manifest.json`.
3. Validate the expected schema.
4. Load GymLedger's explicit mapping.
5. Confirm every mapped Workout Guide slug exists.
6. Confirm frame paths exist.
7. Validate frame count where required.
8. Preserve required attribution data.
9. Copy or convert only approved assets.
10. Generate a human-readable mapping/import report.
11. Fail on broken explicit mappings.
12. Avoid silently changing existing mappings.

Suggested location:

```text
tools/workout-guide-import/
```

Exact implementation language should follow existing repo/tooling conventions at implementation time.

---

## 10. Mapping Discovery Report

Before importing production assets, generate a report such as:

```text
GymLedger exercises:            <N>
exact/high-confidence matches:  <N>
manual mappings confirmed:      <N>
needs manual review:            <N>
unmatched:                       <N>
upstream-only exercises:         <N>
```

Those values must come from actual repository discovery. Do not pre-fill estimates in committed documentation.

For each uncertain match, report enough context to review:

```text
GymLedger ID
GymLedger name
GymLedger equipment
GymLedger primary muscle

candidate Workout Guide slug
candidate name
candidate equipment
candidate primary muscle
confidence/reason
```

Final mappings require explicit approval.

---

## 11. Asset Selection Strategy

Do not copy all upstream assets blindly.

Initial production import should include only exercises that:

- exist in GymLedger;
- have an approved mapping;
- have valid visual assets;
- have understood attribution.

This reduces APK size and prevents incorrect exercise-to-image associations.

Unmapped exercises keep the current GymLedger UI without a visual.

---

## 12. Android Rendering — Open Decision

Workout Guide distributes SVG frames. GymLedger should evaluate at least two strategies before choosing the production path.

### Option A — Bundle SVG and render at runtime

Concept:

```text
upstream SVG
    ↓
importer
    ↓
app bundled asset
    ↓
Android SVG-capable image renderer
    ↓
Compose
```

Advantages:

- preserves vector quality;
- close to upstream source;
- avoids generating raster copies;
- easier asset updates.

Costs/risks:

- likely requires an SVG-capable Android dependency;
- runtime rendering behavior/performance must be validated;
- renderer compatibility with upstream SVG paths must be tested.

### Option B — Convert at build/import time

Concept:

```text
upstream SVG
    ↓
deterministic importer
    ↓
PNG / WebP / Android VectorDrawable
    ↓
Android resources/assets
    ↓
Compose
```

Advantages:

- simpler runtime rendering;
- can avoid an SVG parser dependency;
- predictable Android resource behavior.

Costs/risks:

- conversion tooling;
- potential APK-size increase;
- raster variants lose resolution independence;
- SVG-to-VectorDrawable compatibility may not be complete;
- converted artwork must be treated conservatively as an adapted CC BY-SA asset.

### Decision rule

Do not choose A or B only from theory.

Run a renderer/import spike with approximately 10–15 representative exercises covering:

```text
barbell
dumbbell
bodyweight
cable
machine
stretch/mobility
simple silhouette
complex silhouette
```

Evaluate:

```text
visual fidelity
render time
memory
APK impact
dark/light appearance
Compose integration effort
licensing implications
maintenance/update effort
```

The result must be recorded in this document before production integration.

---

## 13. Runtime Asset Resolution

Android UI should resolve visuals through a GymLedger-owned abstraction.

Conceptual API:

```kotlin
interface ExerciseVisualRepository {
    fun getVisual(exerciseId: String): ExerciseVisual?
}
```

Conceptual result:

```kotlin
data class ExerciseVisual(
    val exerciseId: String,
    val frames: List<ExerciseVisualFrame>,
    val attribution: ExerciseVisualAttribution
)
```

The UI should never need to know Workout Guide paths/slugs directly.

This isolates third-party asset structure from GymLedger product code.

---

## 14. Offline Requirement

Production exercise visuals should be bundled with GymLedger.

Do not require:

```text
GitHub at runtime
npm at runtime
jsDelivr/CDN at runtime
Workout Guide website at runtime
Worker request for each image
```

A versioned CDN may be useful for development comparison or future optional catalog-update research, but it is not the default production dependency.

Active workouts must remain fully usable in airplane mode.

---

## 15. Exercise Detail UX

Post-foundation exercise detail may display:

```text
Exercise name
primary GymLedger metadata

visual guide
  frame 1
  frame 2
  frame 3

optional compact attribution/action
```

Requirements:

- visual does not replace the exercise name;
- unmapped visual state is graceful;
- visual controls have accessibility descriptions;
- visual does not block other exercise actions;
- required attribution remains reachable.

---

## 16. In-Workout UX

Later phase:

```text
Active workout
  ↓
Exercise
  ↓
View guide
  ↓
local three-frame visual
  ↓
close
  ↓
continue set logging unchanged
```

Opening and closing the guide must not:

- reset workout state;
- lose entered sets;
- alter timers;
- trigger remote network calls;
- create a second exercise object.

No AI coaching, pose estimation, camera analysis, or streaming video is part of this integration.

---

## 17. Frame Presentation

The current upstream model provides three ordered frames.

Initial UI should treat them as:

```text
frame 1
frame 2
frame 3
```

Possible presentation modes:

- horizontal sequence;
- pager;
- simple manual stepper;
- optional lightweight local animation later.

Do not infer biomechanical instructions or coaching text solely from the images.

The first implementation should prioritize clarity over animation.

---

## 18. Attribution UX

Attribution should be compliant but not overwhelm the workout screen.

Possible product structure:

```text
Exercise visual
   ↓
small "Visual source" / info affordance
   ↓
Attribution details

Workout Guide / Bryl Lim
Everkinetic where applicable
CC BY-SA 4.0
license link/text
modification notice if applicable
```

Also bundle a global third-party notices/licenses surface if GymLedger adopts one.

Exact presentation must be reviewed before release.

---

## 19. Upstream Updates

Never automatically track upstream `main`.

Recommended update flow:

```text
new upstream release
        ↓
manual version bump
        ↓
run importer in report-only mode
        ↓
compare manifest/mappings/assets/licenses
        ↓
review changes
        ↓
explicit approval
        ↓
regenerate selected assets
        ↓
tests + manual QA
```

A catalog update must never silently remap an existing GymLedger exercise to a different upstream slug.

---

## 20. Integrity Checks

Automated tests/tooling should eventually verify:

- every explicit mapping references an existing upstream slug;
- every imported exercise has expected frame files;
- no duplicate GymLedger ID mappings;
- no accidental duplicate visual mapping unless intentionally allowed;
- attribution metadata exists for every imported visual;
- generated asset index matches bundled files;
- missing asset returns null/fallback instead of crashing;
- importer output is deterministic for the same pinned upstream version.

---

## 21. Performance

The feature should be evaluated for:

```text
APK size
cold-start impact
exercise-list scrolling
exercise-detail rendering
active-workout recomposition
memory use
```

Do not eagerly decode hundreds of exercise visuals.

Lists should prefer an efficient thumbnail/first-frame strategy if visuals are shown there.

Full three-frame content should be loaded only where needed.

---

## 22. Security and Privacy

The bundled visual guide requires no user data to leave the device.

No workout history, exercise history, or personal metrics should be sent to Workout Guide, GitHub, npm, or a CDN.

If a future optional catalog updater is added, it must fetch only catalog/asset metadata and remain separate from personal GymLedger data.

---

## 23. Proposed Roadmap Placement

Recommended placement in `GYMLEDGER_V1_5_PHASES.md`:

```text
Phase 42 — PR and Estimated 1RM

Phase 42A — Exercise Visual Guide Foundation
Phase 42B — Exercise Detail Visuals
Phase 42C — In-Workout Exercise Visual Guide

Phase 43 — Routine Duplication and Templates
```

### Phase 42A

Deliver:

```text
license audit
pinned upstream
mapping/import tooling
mapping report
renderer spike
production rendering decision
representative offline assets
GymLedger-owned visual resolver abstraction
```

### Phase 42B

Deliver:

```text
exercise-detail visual integration
three-frame presentation
fallback
attribution UX
```

### Phase 42C

Deliver:

```text
active-workout visual guide
state-safe open/close flow
offline validation
performance validation
```

---

## 24. Phase 42A Discovery / Preflight Checklist

Before implementation, the builder must discover:

1. Current GymLedger exercise entity/domain model.
2. Stable canonical exercise IDs.
3. Exercise repository/data-source location.
4. Current number of bundled/default exercises.
5. Existing alias/equipment/muscle taxonomy.
6. Existing image-loading dependencies.
7. Existing third-party-license/attribution UI.
8. Appropriate location for import tooling.
9. APK/resource constraints.
10. Current latest suitable Workout Guide release/tag.
11. Whether upstream manifest schema changed since this document was written.

Then STOP for approval before production edits.

---

## 25. Phase 42A Spike Questions

The foundation phase must answer:

```text
What percentage of GymLedger's catalog has a confident upstream match?

Which mappings require manual review?

How many GymLedger exercises have no Workout Guide visual?

What is the APK-size impact of bundling mapped frames?

Can the upstream SVGs be rendered reliably in Android as-is?

Is runtime SVG or build-time conversion preferable?

What attribution surface is sufficient and unobtrusive?

What exact upstream version/commit should GymLedger pin?

How should generated/imported assets be organized in the Android project?
```

Do not proceed to broad Exercise Detail integration until these are answered.

---

## 26. Non-Goals

This architecture does not include:

```text
AI-generated exercise artwork
camera pose estimation
form grading
rep counting
video streaming
online coaching
Workout Guide as exercise database source of truth
automatic exercise creation from upstream catalog
mandatory visual coverage
```

Those require separate future decisions.

---

## 27. Acceptance Criteria for the Overall Integration

The visual guide project is successful when:

- GymLedger exercise identity remains independent.
- Mapped exercises have correct visuals.
- Unmapped exercises remain fully usable.
- Visuals work offline.
- Attribution/license requirements are fulfilled.
- Updates are deterministic and reviewable.
- No moving upstream branch is consumed at runtime.
- No personal data is transmitted for visual rendering.
- Workout logging remains responsive.
- The visual system is reusable between exercise detail and active workout UX.

---

## 28. Reference Links

```text
Workout Guide:
https://github.com/bryllim/workout-guide

Package:
https://www.npmjs.com/package/@bryllim/workout-guide

Gallery:
https://bryllim.github.io/workout-guide/

Integration guide:
https://bryllim.github.io/workout-guide/guide/

CC BY-SA 4.0:
https://creativecommons.org/licenses/by-sa/4.0/

Everkinetic upstream attribution:
https://github.com/everkinetic/data
```

---

## 29. Open Decisions

Resolve during Phase 42A:

```text
[ ] exact pinned upstream tag/commit
[ ] runtime SVG vs build-time conversion
[ ] final asset directory structure
[ ] mapping-file format
[ ] import-tool implementation language
[ ] thumbnail/list policy
[ ] attribution-screen design
[ ] whether assets are all bundled or only mapped/used subset
[ ] update cadence
```

Until those decisions are resolved, this document is the architectural direction, not a production implementation specification.
