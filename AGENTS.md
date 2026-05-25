# GymLedger Agent Rules

## Source of Truth

Implement only `docs/CURRENT_PHASE.md`.

Do not implement future phases.

Read `docs/CURRENT_PHASE.md` first.

Only read additional docs if needed:

- `docs/PROJECT_SPEC.md` for product behavior
- `docs/ARCHITECTURE.md` for architecture boundaries
- `docs/IMPORT_FORMATS.md` only for import/export phases
- `docs/TASKS.md` only if `docs/CURRENT_PHASE.md` is unclear
- `docs/AI_WORKFLOW.md` only for workflow, model choice, quality gates, and escalation rules

## Before Editing

Before editing, list:

1. Files you intend to modify
2. Why each file is needed
3. Validation command
4. Quality gate

Wait for approval before editing.

## Hard Rules

- No backend
- No authentication
- No cloud sync
- No Hilt in v1
- No Retrofit in v1
- No multi-module architecture in v1
- No unnecessary dependencies
- No fake completed functionality
- No future phase work
- All user-facing UI text must be English

## Tool-Calling Rules

Avoid long autonomous tool loops.

Do not read the entire repository unless explicitly asked.

Do not dump huge logs into the conversation.

Summarize long command output.

If you cannot make progress after 8 tool calls, stop and summarize:

- What you inspected
- What you changed
- Current blocker
- First real error, if any
- Next recommended step

## Build Rules

Run the validation command from `docs/CURRENT_PHASE.md`.

If validation fails:

1. Stop after the first real error.
2. Report the error.
3. Do not loop on fixes.
4. Do not refactor unrelated code.

After two failed local attempts, stop and escalate.

## Android Tooling Rule

For Gradle, Android Gradle Plugin, Kotlin plugin, Compose compiler, KSP, Android Studio sync, camera, URI, storage, permissions, emulator, or Logcat issues that fail twice locally, escalate to Gemini in Android Studio.

Use Codex only for hard terminal rescue.

## Free Cloud Rule

Free OpenCode Zen models may be used only for non-sensitive triage or one limited build-fix attempt.

Do not send:

- secrets
- credentials
- keys
- tokens
- signing files
- private personal data
- sensitive proprietary code

## Git Rule

Do not commit.

The user commits manually after review.