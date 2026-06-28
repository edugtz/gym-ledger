# GymLedger Agent Rules

## Source of Truth

Implement only `docs/CURRENT_PHASE.md`.

Read `docs/CURRENT_PHASE.md` first.

Use additional docs only when needed:

- `docs/PROJECT_SPEC.md` for product behavior
- `docs/ARCHITECTURE.md` for architecture boundaries
- `docs/ONLINE_ASSISTED_PLATFORM.md` for optional online-assisted/backend rules
- `docs/BACKEND_CLOUD_PHASES.md` for Cloudflare Worker/backend phases
- `docs/IMPORT_FORMATS.md` only for import/export phases
- `docs/TASKS.md` for roadmap context only, not active scope
- `docs/IMPLEMENTATION_PLAN.md` for the approved current phase plan
- `docs/AI_WORKFLOW.md` for workflow, model choice, quality gates, and escalation rules
- `docs/MVP_REVIEW_AND_CHANGES.md` for schema/product guardrails

## Current Product Principle

GymLedger is local-first and offline-capable.

Online-assisted functionality is allowed only when `docs/CURRENT_PHASE.md` explicitly asks for it.

Room/local data remains the source of truth. Online results are suggestions, must be cacheable, reviewable, editable, and non-blocking.

## Before Editing

Before editing, list:

1. Files you intend to modify
2. Why each file is needed
3. Validation command
4. Quality gate

Wait for approval before editing.

## Hard Rules

- Implement only the active phase.
- Do not implement future phase work.
- Do not create or reference `com.gymledger` paths.
- Package must remain `com.edu.gymledger`.
- No authentication unless the active phase explicitly asks for it.
- No required cloud sync.
- No Hilt in v1.
- No Retrofit in v1.
- No multi-module Android architecture in v1.
- No paid runtime API dependency for core use.
- No unnecessary dependencies.
- No fake completed functionality.
- All user-facing Android UI text must be English.
- Do not commit; the user commits manually.

## Backend / Cloud Rules

Backend/cloud work is allowed only in explicitly approved backend phases.

Preferred backend for personal/low-cost use:

- Cloudflare Worker
- TypeScript
- Cloudflare D1 for structured cache when needed
- Cloudflare KV only when useful for exact key/barcode cache
- Open Food Facts for packaged foods/barcodes
- USDA FoodData Central for generic foods

Do not use unless explicitly approved:

- VPS
- always-on Node/Express server
- paid food API as required dependency
- user accounts
- required cloud sync
- uploading personal meal/workout data
- raw provider payloads exposed directly to Android

Cloud/backend phases must keep monthly personal-use cost target at $0 and maximum expected beta ceiling around $5 USD/month.

## Tool-Calling Rules

Avoid long autonomous tool loops.

If you cannot make progress after 8 tool calls, stop and summarize:

- What you inspected
- What you changed
- Current blocker
- First real error, if any
- Next recommended step

## Build Rules

Run the validation command from `docs/CURRENT_PHASE.md` or `docs/IMPLEMENTATION_PLAN.md`.

If validation fails:

1. Stop after the first real error.
2. Report the error.
3. Do not loop on fixes.
4. Do not refactor unrelated code.

After two failed local attempts, stop and escalate according to `docs/AI_WORKFLOW.md`.

## Escalation Rules

- Product-critical UI/UX feels CRUD or local models loop: use OpenCode Go.
- Gradle/KSP/Room/Compose compiler/Logcat/Android platform issues: use Gemini Android Studio.
- Cloudflare Worker/provider/rate-limit/security edge cases: use Codex or DeepSeek Pro review.
- Repo broken, risky migration, large refactor, release final review: use Codex.

## Sensitive Data Rule

Do not send secrets, tokens, signing files, credentials, private personal data, API keys, or Cloudflare secrets to cloud tools.

For personal API keys, use local environment variables, Cloudflare secrets, or user-entered app settings. Do not hardcode secrets in source code.
