# GymLedger — AI Implementation Workflow

## Goal

Build GymLedger with a local-first AI workflow, while using cloud AI only for critical Android tooling, Gradle, build, or rescue situations.

GymLedger is an Android app built with:

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Navigation Compose
- Kotlinx Serialization
- Gradle

The app must be offline-first, local-only in v1, and installable as an APK.

---

# 1. Source of Truth

Use these files as project control documents:

```text
PROJECT_SPEC.md      = product requirements
ARCHITECTURE.md      = technical boundaries
IMPORT_FORMATS.md    = JSON/CSV data contracts
TASKS.md             = full roadmap
CURRENT_PHASE.md     = active scope
AI_WORKFLOW.md       = AI operating guide
AGENTS.md            = OpenCode local agent rules
```

The active implementation scope is always:

```text
CURRENT_PHASE.md
```

Do not implement work outside `CURRENT_PHASE.md`.

---

# 2. Current AI Strategy

The current strategy is:

```text
Local-first for implementation.
Gemini in Android Studio for Android build/tooling.
Codex for terminal rescue.
OpenCode for controlled local implementation.
Continue for chat, edit, apply, and autocomplete.
```

Do not treat local AI as useless.

Do not treat local AI as fully autonomous.

Use local AI with tighter scope, shorter sessions, and better context control.

---

# 3. Tool Roles

## Gemini in Android Studio

Use Gemini in Android Studio for:

- Gradle build failures
- Android Gradle Plugin issues
- Kotlin plugin issues
- Compose compiler issues
- Android Studio sync issues
- Manifest/resource issues
- Logcat/crash analysis
- Camera/permission Android-specific issues
- Any Android build/tooling issue that repeats twice locally

Gemini in Android Studio is the preferred tool for Android-specific build and tooling problems.

## Codex

Use Codex for:

- Terminal-based rescue
- Build failures Gemini does not fix
- Broader repo-level fixes
- Strong second opinion on diffs
- Controlled terminal agent work

Codex is not the default builder.

Use Codex as a rescue tool when local agents or Gemini are stuck.

## OpenCode

Use OpenCode for:

- Controlled local implementation
- Small or medium phases
- Compose UI
- Repositories
- Validation logic
- Unit tests
- Small patches
- Applying approved plans

OpenCode must not run long autonomous sessions.

OpenCode must ask before editing.

## Continue

Use Continue for:

- Local chat in VS Code
- Small edits
- Apply patches
- Autocomplete
- Explaining code
- Comparing local models

Continue is not the primary Android build-fix tool.

## Antigravity CLI

Do not adopt Antigravity CLI yet.

Consider it later for:

- Multi-agent experiments
- Post-MVP workflows
- Comparing against OpenCode/Codex
- More complex orchestration

Do not add it to the MVP workflow yet.

---

# 4. Model Roles

## Primary local builder

Use:

```text
qwen3-coder-next-mlx
```

Role:

- Primary OpenCode builder
- Feature implementation
- Compose screens
- Repositories
- Tests
- Phase work after Gradle baseline is stable

Operating rules:

- Use short sessions.
- One phase per session.
- Ask before editing.
- Stop after first real build error.
- Do not attempt long build-fix loops.

## Secondary local builder / challenger

Use:

```text
mistralai_devstral-small-2-24b-instruct-2512-mlx
```

Role:

- Alternative implementation
- Controlled patching
- Simpler second implementation
- Fallback when Qwen Coder Next overcomplicates
- Good candidate for small and medium phases

Current status:

```text
Devstral is not slow in raw LM Studio tests.
Observed speed around 11 tok/sec is usable.
If slow in OpenCode, suspect context/tool/log overhead.
```

## Small patch model

Use:

```text
qwen3-coder-30b-a3b-instruct-mlx@5bit
```

Role:

- Small patches only
- Known fix application
- Short code edits

Do not use it as an autonomous builder if it loops.

## Disabled as default

Do not use as default:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Reason:

```text
It crashes on the current machine.
```

Keep it installed only if you want to retest later.

## Planning model

Use:

```text
qwen3.6-35b-a3b
```

Role:

- Mini planning
- Architecture review
- Phase decomposition
- Risk analysis
- Comparing implementation plans
- Review before complex phases

## Debug reasoning model

Use:

```text
deepseek-r1-distill-qwen-32b
```

Role:

- Explain first real build error
- Root cause analysis
- Debug reasoning
- Patch plan review

Do not use as primary builder.

## Documentation model

Use:

```text
gemma-4-31b-it-mlx
```

Role:

- Documentation
- README
- Markdown cleanup
- UI copy review
- Error message wording
- Consistency review

## Fast sidekick

Use:

```text
qwen3.5-9b
```

Role:

- Fast local explanations
- Small questions
- Quick review
- Lightweight code discussion

## Autocomplete

Default:

```text
qwen2.5-coder-1.5b-instruct-mlx
```

Higher quality fallback:

```text
qwen2.5-coder-7b-instruct-mlx
```

---

# 5. Context Strategy

Use a balanced long-context strategy.

Do not lower all contexts aggressively.

Current target context strategy:

```text
qwen3-coder-next-mlx                 65536
qwen3.6-35b-a3b                      65536
deepseek-r1-distill-qwen-32b         65536
mistralai_devstral-small-2-24b       32768
gemma-4-31b-it-mlx                   32768
qwen3.6-27b variants                 32768
small autocomplete models            8192–16384
```

OpenCode compaction strategy:

```text
auto: true
prune: true
reserved: 4096
```

Why:

```text
High context delays compaction.
Lower reserved delays compaction.
Reserved 4096 is a better default than 10000 for your workflow.
Reserved 2048 can be tested if compaction still happens too early.
```

Important:

```text
The context declared in OpenCode should match the context loaded in LM Studio.
```

If LM Studio loads a model at 8192 but OpenCode thinks it has 32768 or 65536, behavior may be confusing.

---

# 6. Session Rules

Use this rule:

```text
1 OpenCode session = 1 phase or 1 bug
```

After every phase or bug fix:

```text
Close the session.
Validate.
Commit.
Start a new session.
```

Do not run one long OpenCode session across many phases.

Do not let OpenCode keep old logs, failed attempts, or unrelated diffs in context.

---

# 7. What OpenCode Should Read

For normal implementation, OpenCode should read:

```text
CURRENT_PHASE.md
```

Then only if needed:

```text
PROJECT_SPEC.md      for product behavior
ARCHITECTURE.md      for architecture boundaries
IMPORT_FORMATS.md    only for import/export phases
TASKS.md             only if CURRENT_PHASE.md is unclear
AGENTS.md            automatically through OpenCode rules if configured
```

Do not ask OpenCode to read all markdown files every time.

Do not feed long Gradle logs unless debugging a specific error.

---

# 8. OpenCode Default Prompt

Use this for most local implementation phases:

```text
Read CURRENT_PHASE.md first.

Only read additional docs if needed:
- PROJECT_SPEC.md for product behavior
- ARCHITECTURE.md for architecture boundaries
- IMPORT_FORMATS.md only for import/export phases
- TASKS.md only if CURRENT_PHASE.md is unclear

Before editing, list:
1. Files you intend to modify.
2. Why each file is needed.
3. Validation command you will run.

Wait for my approval before editing.

Hard rules:
- Implement only CURRENT_PHASE.md.
- Do not implement later phases.
- Do not add backend, authentication, cloud sync, Hilt, Retrofit, or multi-module architecture.
- Do not add unnecessary dependencies.
- All user-facing UI text must be English.
- Do not fake completed functionality.
- If build fails, stop after the first real error.
- Do not loop on fixes.
```

After reviewing the file list, approve with:

```text
Approved.

Edit only the files you listed.
Do not touch unrelated files.
Implement CURRENT_PHASE.md only.
Run the validation command from CURRENT_PHASE.md.
If validation fails, stop after the first real error and report it.
```

---

# 9. Mini Planning Prompt

Use this with:

```text
qwen3.6-35b-a3b
```

Prompt:

```text
Act as a senior Android architect and product engineer.

Read:
- CURRENT_PHASE.md
- ARCHITECTURE.md if needed
- PROJECT_SPEC.md if product behavior is unclear
- IMPORT_FORMATS.md only if this phase touches import/export

Create a concise implementation plan for CURRENT_PHASE.md only.

Rules:
- Do not plan future phases.
- Do not add backend, authentication, cloud sync, Hilt, Retrofit, or multi-module architecture.
- Keep the architecture simple.
- All user-facing UI text must be English.
- Prefer small, committable changes.

Return:
1. Files likely to be created or modified.
2. Implementation order.
3. Main risks or build traps.
4. Validation commands.
5. Suggested commit message.

Do not write full code.
Do not expand the scope.
```

---

# 10. Debug Prompt

Use with:

```text
deepseek-r1-distill-qwen-32b
```

Prompt:

```text
Act as a senior Android debugging engineer.

Current phase:
```md
PASTE CURRENT_PHASE.md HERE
```

First real build/test error:
```text
PASTE FIRST REAL ERROR HERE
```

Relevant diff or snippets:
```diff
PASTE RELEVANT DIFF HERE
```

Task:
1. Identify the most likely root cause.
2. Explain the minimal fix.
3. List exact files that should change.
4. Avoid scope creep.
5. Do not suggest new dependencies unless required.
6. Do not rewrite architecture.
7. Do not implement future phases.

Return only a minimal patch plan.
```

Then apply with OpenCode:

```text
Apply only this minimal patch plan.

Do not refactor.
Do not implement new features.
Do not move to the next phase.

Patch plan:
PASTE PATCH PLAN HERE

Run validation once.
If it fails, stop after the first real error.
```

---

# 11. Gemini in Android Studio Prompt

Use for Gradle/build/Android tooling issues.

```text
Fix only the Android build/tooling issue.

Do not implement GymLedger features.
Do not add Room unless the current phase explicitly requires it.
Do not add DataStore unless the current phase explicitly requires it.
Do not add Navigation unless the current phase explicitly requires it.
Do not refactor unrelated code.

Current phase:
PASTE CURRENT_PHASE.md

Command failing:
./gradlew clean assembleDebug

First real error:
PASTE FIRST REAL ERROR

Inspect only files relevant to the failure, such as:
- settings.gradle.kts
- build.gradle.kts
- app/build.gradle.kts
- gradle/libs.versions.toml
- gradle/wrapper/gradle-wrapper.properties
- AndroidManifest.xml if needed

Goal:
Apply the smallest safe fix.
Run the build again.
Stop after the build result.
```

---

# 12. Codex Prompt

Use Codex only for rescue or stronger terminal-based fixes.

```text
Fix only the current blocking issue.

Read:
- CURRENT_PHASE.md
- relevant Gradle/build files
- relevant source files only if needed

Do not implement future phases.
Do not add unrelated dependencies.
Do not refactor unrelated code.
Do not add backend, auth, cloud sync, Hilt, Retrofit, or multi-module architecture.

Run:
./gradlew clean assembleDebug

Use the first real error as the source of truth.
Apply the smallest patch.
Run validation once.
Stop and report:
1. Files changed
2. Fix applied
3. Build result
4. Remaining first error, if any
```

---

# 13. Phase Strategy

## Phase 0 — Project Bootstrap

Tool:

```text
Gemini in Android Studio if build/sync fails.
Manual Android Studio setup is acceptable.
```

Local AI:

```text
Do not use local agent for Gradle bootstrap.
```

Commit:

```text
chore: create Android project
```

## Phase 1 — Base Dependencies

Tool:

```text
Gemini in Android Studio preferred.
```

Why:

```text
Gradle, AGP, Kotlin, Compose compiler, and KSP version alignment are fragile.
```

Commit:

```text
chore: add base dependencies
```

## Phase 2 — App Shell and Theme

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add app shell and theme
```

## Phase 3 — Main Navigation

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add main navigation
```

## Phase 4 — Room Foundation

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Escalate:

```text
Gemini in Android Studio if Room/KSP/Gradle errors repeat twice.
```

Commit:

```text
feat: add Room database foundation
```

## Phase 5 — Room Smoke Tests

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
test: add Room smoke tests
```

## Phase 6 — Exercise Repository

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add exercise repository
```

## Phase 7 — Exercises UI

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Fallback:

```text
Devstral if Qwen overcomplicates
```

Mini planning:

```text
No
```

Commit:

```text
feat: add exercises CRUD UI
```

## Phase 8 — Workout Repository

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add workout repository
```

## Phase 9 — Workout List UI

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add workout list
```

## Phase 10 — Workout Detail and Sets UI

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Fallback:

```text
Devstral for second implementation
Codex if repeated state/navigation issues persist
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add workout set logging
```

## Phase 11 — Routine Repository

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add routine repository
```

## Phase 12 — Routines UI

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Fallback:

```text
Devstral
```

Mini planning:

```text
No
```

Commit:

```text
feat: add routines UI
```

## Phase 13 — Start Workout From Routine

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: start workout from routine
```

## Phase 14 — Body Repository

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add body measurement repository
```

## Phase 15 — Body Measurements UI

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add body measurements UI
```

## Phase 16 — Food Repository

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add food repository
```

## Phase 17 — Foods UI

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Fallback:

```text
Devstral
```

Mini planning:

```text
No
```

Commit:

```text
feat: add foods CRUD UI
```

## Phase 18 — Nutrition Repository

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add nutrition repository
```

## Phase 19 — Nutrition Day UI

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add daily nutrition UI
```

## Phase 20 — Meal Detail and Items UI

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Fallback:

```text
Devstral
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add meal detail and items
```

## Phase 21 — Settings Repository

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add settings repository
```

## Phase 22 — Settings UI

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
No
```

Commit:

```text
feat: add settings UI
```

## Phase 23 — Real Dashboard Data

Tool:

```text
OpenCode + qwen3-coder-next-mlx
```

Mini planning:

```text
Optional
```

Commit:

```text
feat: connect dashboard to real data
```

## Phase 24 — JSON Backup Models

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add JSON backup models
```

## Phase 25 — JSON Export

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Escalate:

```text
Codex if file picker/document writing gets messy.
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add JSON export
```

## Phase 26 — JSON Import

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Escalate:

```text
Codex if transaction/restore logic gets messy.
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add JSON import
```

## Phase 27 — CSV Parser

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add CSV parser
```

## Phase 28 — CSV Export

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add CSV export
```

## Phase 29 — Simple CSV Import

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add simple CSV imports
```

## Phase 30 — Relational CSV Import

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Fallback:

```text
Codex if relationship mapping fails repeatedly.
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add relational CSV imports
```

## Phase 31 — Import / Export UI

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Escalate:

```text
Gemini in Android Studio for ActivityResult/document picker issues.
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add import export UI
```

## Phase 32 — Meal Photo Storage and UI

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Escalate:

```text
Gemini in Android Studio for camera, URI, and permissions issues.
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add meal photos
```

## Phase 33 — Food Photo Estimator

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add assisted food photo estimator
```

## Phase 34 — Photo-Assisted Estimate UI

Tool:

```text
qwen3.6-35b-a3b planning → OpenCode implementation
```

Fallback:

```text
Devstral
```

Mini planning:

```text
Yes
```

Commit:

```text
feat: add photo assisted meal estimates
```

## Phase 35 — Empty States and Validation Polish

Tool:

```text
OpenCode + qwen3-coder-next-mlx
Gemma 31B for copy review
```

Mini planning:

```text
No
```

Commit:

```text
chore: polish empty states and validation
```

## Phase 36 — Final QA and Hardening

Tool:

```text
qwen3.6-35b-a3b planning
deepseek-r1-distill-qwen-32b debug
Gemini in Android Studio for Android-specific issues
Codex for broad rescue
OpenCode for controlled fixes
```

Mini planning:

```text
Yes
```

Commit:

```text
chore: harden MVP
```

## Phase 37 — Post-MVP Backlog

Tool:

```text
gemma-4-31b-it-mlx
```

Mini planning:

```text
No
```

Commit:

```text
docs: add post MVP backlog
```

---

# 14. After Every Phase

Run:

```bash
git status
git diff
./gradlew assembleDebug
```

For critical phases:

```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
```

If validation passes:

```bash
git add .
git commit -m "PASTE_PHASE_COMMIT_MESSAGE"
```

Then:

```text
1. Replace CURRENT_PHASE.md with the next phase.
2. Start a new AI session.
3. Do not reuse old long agent sessions.
```

---

# 15. Stop Conditions

Stop the agent if:

```text
It starts editing files outside the approved list.
It starts implementing future phases.
It tries to fix multiple build errors in a loop.
It reads too many unrelated files.
It rewrites architecture.
It adds dependencies without approval.
It starts explaining instead of applying the approved scope.
```

Use:

```text
Stop.

You are going beyond CURRENT_PHASE.md.

Keep only changes required for CURRENT_PHASE.md.
Revert or isolate unrelated changes.
Do not continue to later phases.
Run the validation command once and stop.
```

---

# 16. Current Model Keep/Delete Policy

Do not delete these:

```text
qwen3-coder-next-mlx
mistralai_devstral-small-2-24b-instruct-2512-mlx
qwen3.6-35b-a3b
deepseek-r1-distill-qwen-32b
gemma-4-31b-it-mlx
qwen2.5-coder-1.5b-instruct-mlx
qwen2.5-coder-7b-instruct-mlx
qwen3.5-9b
```

Do not use as default:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
```

Possible delete candidates only if disk space becomes a problem:

```text
qwen3-coder-30b-a3b-instruct-mlx@6bit
gemma-4-26b-a4b-it-mlx
deepseek-r1-distill-qwen-32b-mlx
qwen3.6-35b-a3b-mlx
qwen3-coder-30b-a3b-instruct-mlx@8bit
```

Do not delete anything yet just because of one bad agent run.

---

# 17. Golden Rule

```text
Local AI implements scoped phases.
Gemini in Android Studio fixes Android tooling.
Codex rescues hard terminal problems.
OpenCode edits only with approval.
LM Studio context must match OpenCode model context.
Git is the checkpoint.
Gradle is the source of truth.
```