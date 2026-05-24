# GymLedger — Local AI Workflow Guide

## Purpose

This guide explains how to use local AI models to build GymLedger phase by phase.

The project should be implemented through small, validated, committable phases.

Core rule:

```text
PROJECT_SPEC.md + ARCHITECTURE.md + IMPORT_FORMATS.md + TASKS.md = global context
CURRENT_PHASE.md = only active implementation scope
```

The local coding agent must implement only `CURRENT_PHASE.md`.

---

# 1. Tool Strategy

## What should be done in OpenCode?

Use OpenCode for:

- Reading the repository
- Editing files
- Implementing the current phase
- Running validation commands
- Applying small build fixes
- Summarizing changed files

## What should not always be done in OpenCode?

Do not use OpenCode for every planning/debug/review step if it may start editing too early.

Use LM Studio Chat or ChatGPT for:

- Mini planning
- Debug reasoning
- Second opinion
- Diff review
- Documentation review

Then paste the final diagnosis or plan into OpenCode.

## Recommended workflow

```text
1. Replace CURRENT_PHASE.md with the active phase.
2. Decide if the phase needs mini planning.
3. If needed, run mini planning with the planning model.
4. Use OpenCode with the builder model to implement only CURRENT_PHASE.md.
5. Run validation.
6. If build fails, stop and debug the first real error.
7. Apply minimal fix.
8. Review git diff.
9. Commit.
10. Move to next phase.
```

---

# 2. Local Model Roles

## Primary Planning Model

Use:

```text
qwen3.6-35b-a3b
```

Best for:

- Phase planning
- Architecture decisions
- Risk analysis
- Room schema planning
- Import/export strategy
- Storage/photo flow planning
- Reviewing whether the agent is staying in scope

Use before risky phases.

---

## Primary Builder Model

Use in OpenCode:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Best for:

- Kotlin implementation
- Compose UI
- Room DAOs
- Repositories
- Gradle changes
- Tests
- Feature implementation

Default builder for most phases.

---

## Stable Fallback Builder

Use if 6bit becomes unstable, slow, or loops:

```text
qwen3-coder-30b-a3b-instruct-mlx@5bit
```

Best for:

- Same work as 6bit
- More stable fallback
- Faster iteration if 6bit is not worth it

---

## Second Implementation / Challenger Model

Use when Qwen Coder gets stuck or creates messy code:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Best for:

- Alternative implementation
- Simpler second attempt
- Build-fix implementation
- Refactoring an overcomplicated solution back to something practical

Use as a challenger, not necessarily as default.

---

## Debug Reasoning Model

Use:

```text
deepseek-r1-distill-qwen-32b
```

Best for:

- Understanding Gradle errors
- Kotlin compiler errors
- Room annotation errors
- Compose state bugs
- Debugging why the builder is looping
- Finding the real first error

Use for diagnosis, not necessarily for editing.

---

## Documentation / Writing Model

Use:

```text
gemma-4-31b-it-mlx
```

Best for:

- README
- Documentation cleanup
- Error message wording
- UI copy review
- Consistency checks
- Polishing markdown files

---

## Heavy Build-Fix Challenger

Use only if needed:

```text
qwen3-coder-next-mlx
```

Best for:

- Hard build fixes
- Complex multi-file code corrections
- Alternative implementation if Qwen Coder 30B and Devstral fail

Important:

```text
Do not load this together with another large model.
Use it alone.
```

---

## Models to avoid for this workflow

Do not use:

```text
qwen3.6-27b-ud-mlx@6bit
```

Reason:

```text
It previously caused repeated segmentation faults.
```

---

# 3. Model Loading Rules

## Normal implementation session

Load only:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

If unstable:

```text
qwen3-coder-30b-a3b-instruct-mlx@5bit
```

## Planning session

Load only:

```text
qwen3.6-35b-a3b
```

## Debug session

Load only one of:

```text
deepseek-r1-distill-qwen-32b
mistralai_devstral-small-2-24b-instruct-2512-mlx
qwen3-coder-next-mlx
```

## Documentation session

Load:

```text
gemma-4-31b-it-mlx
```

---

# 4. Should You Mini Plan Every Phase?

No.

Mini planning every phase creates unnecessary overhead.

Use this rule:

```text
Simple phase → go directly to OpenCode
Complex/risky phase → mini planning first
Broken build/loop → debug planning
```

---

# 5. Phases That Should Use Mini Planning

Use mini planning before these phases:

```text
Phase 1 — Base Dependencies
Phase 4 — Room Foundation
Phase 8 — Workout Repository
Phase 10 — Workout Detail and Sets UI
Phase 11 — Routine Repository
Phase 18 — Nutrition Repository
Phase 20 — Meal Detail and Items UI
Phase 24 — JSON Backup Models
Phase 25 — JSON Export
Phase 26 — JSON Import
Phase 27 — CSV Parser
Phase 28 — CSV Export
Phase 29 — Simple CSV Import
Phase 30 — Relational CSV Import
Phase 31 — Import / Export UI
Phase 32 — Meal Photo Storage and UI
Phase 33 — Food Photo Estimator
Phase 34 — Photo-Assisted Estimate UI
Phase 36 — Final QA and Hardening
```

Reason:

```text
These phases touch Gradle, Room relations, import/export, file IO, photos, tests, or multi-step flows.
```

---

# 6. Phases That Can Go Directly to Build

These phases can usually go directly to OpenCode:

```text
Phase 0 — Project Bootstrap
Phase 2 — App Shell and Theme
Phase 3 — Main Navigation
Phase 5 — Room Smoke Tests
Phase 6 — Exercise Repository
Phase 7 — Exercises UI
Phase 9 — Workout List UI
Phase 12 — Routines UI
Phase 13 — Start Workout From Routine
Phase 14 — Body Repository
Phase 15 — Body Measurements UI
Phase 16 — Food Repository
Phase 17 — Foods UI
Phase 19 — Nutrition Day UI
Phase 21 — Settings Repository
Phase 22 — Settings UI
Phase 23 — Real Dashboard Data
Phase 35 — Empty States and Validation Polish
Phase 37 — Post-MVP Backlog
```

If any of these phases becomes messy, stop and use debug planning.

---

# 7. Phase-by-Phase AI Plan

## Phase 0 — Project Bootstrap

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Fallback:

```text
qwen3-coder-30b-a3b-instruct-mlx@5bit
```

Recommended approach:

```text
Prefer creating the Android project manually in Android Studio, then let OpenCode validate and clean up.
```

Commit:

```text
chore: create Android project
```

---

## Phase 1 — Base Dependencies

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Gradle plugin mismatch
KSP version mismatch
Compose compiler compatibility
Room compiler configuration
Kotlin serialization plugin setup
```

Commit:

```text
chore: add base dependencies
```

---

## Phase 2 — App Shell and Theme

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Overcomplicating theme structure
Adding unnecessary design system
```

Commit:

```text
feat: add app shell and theme
```

---

## Phase 3 — Main Navigation

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Creating too many nested graphs too early
Breaking back stack behavior
```

Commit:

```text
feat: add main navigation
```

---

## Phase 4 — Room Foundation

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Room foreign key issues
Missing indices
Wrong nullable relationships
DAO Flow signatures
Annotation processor errors
```

Commit:

```text
feat: add Room database foundation
```

---

## Phase 5 — Room Smoke Tests

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Incorrect AndroidX test dependencies
Room in-memory setup issues
```

Commit:

```text
test: add Room smoke tests
```

---

## Phase 6 — Exercise Repository

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Over-abstracting repository
Adding unnecessary domain layer
```

Commit:

```text
feat: add exercise repository
```

---

## Phase 7 — Exercises UI

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Main risks:

```text
Overcomplicated form state
Poor validation UX
User-facing text not in English
```

Commit:

```text
feat: add exercises CRUD UI
```

---

## Phase 8 — Workout Repository

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Session/set relationship errors
Cascade delete mistakes
Incorrect validation logic
Session detail query complexity
```

Commit:

```text
feat: add workout repository
```

---

## Phase 9 — Workout List UI

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Navigation argument mismatch
Creating workout detail too early
```

Commit:

```text
feat: add workout list
```

---

## Phase 10 — Workout Detail and Sets UI

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Exercise selector state
Set editing flow
Invalid number parsing
RPE/RIR validation
Detail query performance
```

Commit:

```text
feat: add workout set logging
```

---

## Phase 11 — Routine Repository

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Routine exercise ordering
Deleting routines without deleting exercises
Historical workout independence
```

Commit:

```text
feat: add routine repository
```

---

## Phase 12 — Routines UI

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Main risks:

```text
Overcomplicated reordering
Too much UI polish too early
```

Commit:

```text
feat: add routines UI
```

---

## Phase 13 — Start Workout From Routine

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Creating fake completed sets
Mutating routine data when starting workout
```

Commit:

```text
feat: start workout from routine
```

---

## Phase 14 — Body Repository

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Validation edge cases
Latest measurement query
```

Commit:

```text
feat: add body measurement repository
```

---

## Phase 15 — Body Measurements UI

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Date input complexity
Numeric parsing
```

Commit:

```text
feat: add body measurements UI
```

---

## Phase 16 — Food Repository

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Macro validation
Search query behavior
```

Commit:

```text
feat: add food repository
```

---

## Phase 17 — Foods UI

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Main risks:

```text
Number parsing
Macro fields UX
User-facing text not in English
```

Commit:

```text
feat: add foods CRUD UI
```

---

## Phase 18 — Nutrition Repository

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Daily date filtering
Macro calculation precision
Meal/meal item relationships
Manual vs food-based meal items
```

Commit:

```text
feat: add nutrition repository
```

---

## Phase 19 — Nutrition Day UI

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Date picker scope creep
Daily summary state updates
```

Commit:

```text
feat: add daily nutrition UI
```

---

## Phase 20 — Meal Detail and Items UI

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Main risks:

```text
Food selector state
Macro auto-calculation plus manual override
Meal total recalculation
Numeric input validation
```

Commit:

```text
feat: add meal detail and items
```

---

## Phase 21 — Settings Repository

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
DataStore Flow defaults
Invalid preference values
```

Commit:

```text
feat: add settings repository
```

---

## Phase 22 — Settings UI

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Settings not saved immediately
Theme scope creep
```

Commit:

```text
feat: add settings UI
```

---

## Phase 23 — Real Dashboard Data

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Combining multiple flows incorrectly
Dashboard recomposition issues
```

Commit:

```text
feat: connect dashboard to real data
```

---

## Phase 24 — JSON Backup Models

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Serialization annotations
Nullable fields
Schema versioning
Entity/backup mapping
```

Commit:

```text
feat: add JSON backup models
```

---

## Phase 25 — JSON Export

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Storage Access Framework
File writing
Empty database export
Collecting data from all repositories
```

Commit:

```text
feat: add JSON export
```

---

## Phase 26 — JSON Import

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Transactional import
Relationship restoration
Invalid JSON handling
Replace-all safety
Duplicate handling
```

Commit:

```text
feat: add JSON import
```

---

## Phase 27 — CSV Parser

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Quoted commas
Escaped quotes
Empty fields
Header validation
Error reporting
```

Commit:

```text
feat: add CSV parser
```

---

## Phase 28 — CSV Export

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
CSV escaping
Headers matching IMPORT_FORMATS.md
Dates and decimal formatting
Empty database export
```

Commit:

```text
feat: add CSV export
```

---

## Phase 29 — Simple CSV Import

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Header validation
Row-level errors
Partial invalid import
Numeric parsing
Duplicate handling
```

Commit:

```text
feat: add simple CSV imports
```

---

## Phase 30 — Relational CSV Import

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
External ID mapping
Missing references
Transactional import
Workout set relationships
Meal item relationships
```

Commit:

```text
feat: add relational CSV imports
```

---

## Phase 31 — Import / Export UI

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Activity result contracts
Document picker result handling
Progress/error UI
Not freezing the UI
```

Commit:

```text
feat: add import export UI
```

---

## Phase 32 — Meal Photo Storage and UI

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Camera contracts
Content URI handling
App-specific storage
Cancellation handling
Avoiding unnecessary storage permissions
```

Commit:

```text
feat: add meal photos
```

---

## Phase 33 — Food Photo Estimator

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Accidentally adding AI/API dependency
Presenting estimate as accurate
Not requiring user confirmation
```

Commit:

```text
feat: add assisted food photo estimator
```

---

## Phase 34 — Photo-Assisted Estimate UI

Planning:

```text
Mini planning recommended.
```

Planning model:

```text
qwen3.6-35b-a3b
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Second implementation:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Main risks:

```text
Food selection
Editable estimate state
Saving as meal item
Clear approximation warning
```

Commit:

```text
feat: add photo assisted meal estimates
```

---

## Phase 35 — Empty States and Validation Polish

Planning:

```text
No mini planning needed.
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Docs/copy review:

```text
gemma-4-31b-it-mlx
```

Main risks:

```text
Accidentally changing behavior while polishing
```

Commit:

```text
chore: polish empty states and validation
```

---

## Phase 36 — Final QA and Hardening

Planning:

```text
Mini planning recommended.
```

Planning/review model:

```text
qwen3.6-35b-a3b
```

Debug:

```text
deepseek-r1-distill-qwen-32b
```

Builder/fixer:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Large unscoped refactors
Fixing non-blocking issues while introducing new bugs
Adding post-MVP features
```

Commit:

```text
chore: harden MVP
```

---

## Phase 37 — Post-MVP Backlog

Planning:

```text
No mini planning needed.
```

Docs model:

```text
gemma-4-31b-it-mlx
```

Builder:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Main risks:

```text
Accidentally implementing backlog instead of documenting it
```

Commit:

```text
docs: add post MVP backlog
```

---

# 8. Mini Planning Prompt

Use this with:

```text
qwen3.6-35b-a3b
```

Prompt:

```text
Act as a senior Android architect and product engineer.

You are working on GymLedger, a personal offline-first Android app.

Read:
- PROJECT_SPEC.md
- ARCHITECTURE.md
- IMPORT_FORMATS.md
- TASKS.md
- CURRENT_PHASE.md

Create a concise implementation plan for CURRENT_PHASE.md only.

Rules:
- Do not plan future phases.
- Do not add backend, authentication, cloud sync, Hilt, Retrofit, or multi-module architecture.
- Keep the architecture simple.
- All user-facing UI text must be English.
- Respect the existing project structure.
- Prefer small, committable changes.

Return:
1. Files likely to be created or modified.
2. Implementation order.
3. Main risks or build traps.
4. Validation commands to run.
5. What should be committed.

Do not write full code unless absolutely necessary.
Do not expand the scope beyond CURRENT_PHASE.md.
```

---

# 9. OpenCode Builder Prompt

Use this in OpenCode with:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Fallback:

```text
qwen3-coder-30b-a3b-instruct-mlx@5bit
```

Prompt:

```text
Read:
- PROJECT_SPEC.md
- ARCHITECTURE.md
- IMPORT_FORMATS.md
- TASKS.md
- CURRENT_PHASE.md

Implement only the work described in CURRENT_PHASE.md.

Hard rules:
- Do not implement later phases.
- Do not add backend, authentication, cloud sync, Hilt, Retrofit, or multi-module architecture.
- Do not add unnecessary dependencies.
- Keep the implementation simple and committable.
- All user-facing UI text must be English.
- Do not fake completed functionality.
- If the build breaks, fix only the first real blocking error before continuing.
- Stop when the phase acceptance criteria are met.

Process:
1. Inspect the existing project structure.
2. Identify the minimal files needed for this phase.
3. Implement the phase incrementally.
4. Run the validation commands listed in CURRENT_PHASE.md.
5. Fix blocking build/test errors only.
6. Stop and summarize:
   - files changed
   - what was implemented
   - validation results
   - suggested commit message

Do not continue into the next phase.
```

---

# 10. Builder Prompt With Mini Plan

Use this when you already created a mini plan.

Prompt:

```text
Read:
- PROJECT_SPEC.md
- ARCHITECTURE.md
- IMPORT_FORMATS.md
- TASKS.md
- CURRENT_PHASE.md

Implement CURRENT_PHASE.md only.

Use this implementation plan as guidance:

```text
PASTE_MINI_PLAN_HERE
```

Hard rules:
- The plan is guidance, not permission to expand scope.
- CURRENT_PHASE.md is the source of truth.
- Do not implement later phases.
- Do not add backend, authentication, cloud sync, Hilt, Retrofit, or multi-module architecture.
- Do not add unnecessary dependencies.
- All user-facing UI text must be English.
- Keep the implementation simple and committable.

After implementation:
1. Run the validation commands from CURRENT_PHASE.md.
2. Fix only blocking build/test errors.
3. Stop and summarize changed files and validation results.
```

---

# 11. Debug Prompt

Use this outside OpenCode first with:

```text
deepseek-r1-distill-qwen-32b
```

or:

```text
qwen3.6-35b-a3b
```

Prompt:

```text
Act as a senior Android build/debugging engineer.

The local coding agent is stuck fixing a build error.

Project:
- GymLedger
- Kotlin
- Jetpack Compose
- Room
- DataStore
- Navigation Compose
- Kotlinx Serialization
- Gradle

Current phase:
```md
PASTE_CURRENT_PHASE_HERE
```

First real build/test error:
```text
PASTE_FIRST_REAL_ERROR_HERE
```

Relevant git diff or file snippets:
```diff
PASTE_RELEVANT_DIFF_OR_SNIPPETS_HERE
```

Task:
1. Identify the most likely root cause.
2. Explain the minimal fix.
3. Tell me exactly which file(s) should change.
4. Avoid scope creep.
5. Do not suggest new dependencies unless absolutely required.
6. Do not rewrite architecture.
7. Do not implement future phases.

Return a minimal patch plan only.
```

Then give OpenCode this:

```text
Apply only this minimal build fix.

Do not refactor.
Do not implement new features.
Do not move to the next phase.

Fix plan:
```text
PASTE_DEBUG_PLAN_HERE
```

Then run:
./gradlew assembleDebug

Stop after the build result.
```

---

# 12. Second Implementation Prompt

Use this with:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Use when the first builder produced messy code or got stuck.

Prompt:

```text
Act as a pragmatic senior Android engineer.

The previous implementation attempt for CURRENT_PHASE.md got stuck or became too complex.

Read:
- PROJECT_SPEC.md
- ARCHITECTURE.md
- IMPORT_FORMATS.md
- TASKS.md
- CURRENT_PHASE.md

Your job:
- Propose a simpler implementation approach for CURRENT_PHASE.md only.
- Keep the architecture simple.
- Avoid large refactors.
- Do not implement future phases.
- Do not add backend, auth, cloud sync, Hilt, Retrofit, or multi-module architecture.
- All user-facing UI text must be English.

Return:
1. What to keep from the current implementation.
2. What to remove or simplify.
3. Minimal file changes needed.
4. Step-by-step implementation plan.
5. Validation commands.

Do not edit files unless explicitly asked.
```

If you want Devstral to implement directly in OpenCode:

```text
Implement CURRENT_PHASE.md using the simplest working approach.

Do not continue to future phases.
Do not refactor unrelated code.
Do not add unnecessary dependencies.
Run the validation commands from CURRENT_PHASE.md.
Stop after validation.
```

---

# 13. Review Before Commit Prompt

Use this with:

```text
qwen3.6-35b-a3b
```

or:

```text
gemma-4-31b-it-mlx
```

Prompt:

```text
Act as a senior Android reviewer.

Review this implementation against CURRENT_PHASE.md only.

Rules:
- Do not ask for future-phase work.
- Do not suggest overengineering.
- Do not suggest backend, auth, cloud sync, Hilt, Retrofit, or multi-module architecture.
- All user-facing UI text must be English.
- Focus on correctness, scope control, build stability, and MVP simplicity.

Current phase:
```md
PASTE_CURRENT_PHASE_HERE
```

Git diff:
```diff
PASTE_GIT_DIFF_HERE
```

Validation result:
```text
PASTE_VALIDATION_RESULT_HERE
```

Return:
1. Is this within scope? yes/no
2. Any obvious bugs?
3. Any build/test risks?
4. Any user-facing text not in English?
5. Should I commit now? yes/no
6. Suggested commit message
```

---

# 14. What To Do After Every Phase

After OpenCode says the phase is done, run:

```bash
git status
git diff
```

Then run the validation command from `CURRENT_PHASE.md`.

At minimum:

```bash
./gradlew assembleDebug
```

For bigger phases:

```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
```

If validation passes:

```bash
git add .
git commit -m "PASTE_SUGGESTED_COMMIT_MESSAGE"
```

Then:

```text
1. Replace CURRENT_PHASE.md with the next phase.
2. Load the right model.
3. Decide whether mini planning is needed.
4. Continue.
```

---

# 15. What To Do If Build Fails

Do not let the agent loop forever.

Do this:

```bash
./gradlew clean assembleDebug
```

Then:

```text
1. Copy the first real error.
2. Ignore repeated downstream errors.
3. Use the Debug Prompt.
4. Apply only the minimal fix.
5. Run build again.
```

Do not do this:

```text
Do not ask the builder to rewrite the app.
Do not allow broad refactors.
Do not move to the next phase.
Do not add dependencies randomly.
```

---

# 16. What To Do If OpenCode Overbuilds

If OpenCode starts implementing future phases, stop it.

Use this prompt:

```text
Stop.

You are expanding beyond CURRENT_PHASE.md.

Revert or isolate any work that belongs to later phases.

Keep only changes required for CURRENT_PHASE.md.

Do not implement future phases.
Do not refactor unrelated files.

After reducing scope, run the validation commands from CURRENT_PHASE.md and stop.
```

Then review the diff carefully.

---

# 17. Recommended Start

If the Android project does not exist yet:

```text
Create the project manually in Android Studio first.
```

Recommended Android Studio setup:

```text
Template: Empty Activity
Name: GymLedger
Package name: com.edu.gymledger
Language: Kotlin
UI: Jetpack Compose
```

Then validate:

```bash
./gradlew clean assembleDebug
```

Then add the docs:

```text
PROJECT_SPEC.md
ARCHITECTURE.md
IMPORT_FORMATS.md
TASKS.md
CURRENT_PHASE.md
AI_WORKFLOW.md
```

Initial commit:

```bash
git init
git add .
git commit -m "chore: create Android project"
```

Then start Phase 1.

---

# 18. Daily Working Loop

Use this loop every time:

```text
1. Confirm current branch is clean.
2. Replace CURRENT_PHASE.md.
3. Load the correct model.
4. Mini plan only if needed.
5. Build in OpenCode.
6. Validate.
7. Debug first real error if needed.
8. Review diff.
9. Commit.
10. Move to next phase.
```

Commands:

```bash
git status
./gradlew assembleDebug
git diff
git add .
git commit -m "commit message"
```

---

# 19. Golden Rules

```text
CURRENT_PHASE.md is the scope.
TASKS.md is the roadmap.
PROJECT_SPEC.md is the product truth.
ARCHITECTURE.md is the technical boundary.
IMPORT_FORMATS.md is the data contract.
```

Never let the agent violate these:

```text
No backend.
No login.
No cloud sync.
No Hilt in v1.
No multi-module in v1.
No unnecessary dependencies.
No fake functionality.
No inaccurate food-photo claims.
All user-facing UI text must be English.
```