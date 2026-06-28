# GymLedger — Phase Prompts

## Mini Planning Prompt — Android Phase

```text
Read docs/CURRENT_PHASE.md and docs/AI_WORKFLOW.md.
Read docs/ARCHITECTURE.md.
Read docs/ONLINE_ASSISTED_PLATFORM.md only if the phase touches online/cloud settings or lookup.
Read docs/MVP_REVIEW_AND_CHANGES.md if the phase touches schema or product UX.
Read docs/IMPORT_FORMATS.md only for import/export phases.

Do not edit files.

Create or replace docs/IMPLEMENTATION_PLAN.md for the current phase.

Include:
- Objective
- Product quality goal
- Files to create
- Files to modify
- Files not to touch
- Implementation order
- Risks/build traps
- Recommended AI route
- Validation commands
- Manual QA checklist
- Builder preflight prompt
- Suggested commit
```

## Mini Planning Prompt — Backend Worker Phase

```text
Read docs/CURRENT_PHASE.md.
Read docs/BACKEND_CLOUD_PHASES.md.
Read docs/ONLINE_ASSISTED_PLATFORM.md.
Read docs/AI_WORKFLOW.md.

Do not edit files.

Create or replace docs/IMPLEMENTATION_PLAN.md for the current backend phase.

Include:
- Objective
- Worker/backend scope
- Endpoints
- Files to create
- Files to modify
- Env vars/secrets
- D1/KV usage if any
- Provider usage if any
- Cost/free-tier guardrails
- Validation commands
- curl/manual QA
- Builder preflight prompt
- Suggested commit
```

## Builder Preflight Prompt

```text
Implement only docs/CURRENT_PHASE.md and docs/IMPLEMENTATION_PLAN.md.

Role:
Builder preflight only. Do not edit files yet.

Before editing, list:
1. Files found
2. Files you intend to modify/create
3. Why each file is needed
4. Files you will not touch
5. Validation command
6. Quality gate
7. Conflicts with actual repo

Wait for approval before editing.
```

## Review Prompt

```text
Review the current phase diff only.
Do not edit files.

Check:
- matches docs/CURRENT_PHASE.md
- follows docs/IMPLEMENTATION_PLAN.md
- no scope creep
- no future phase work
- no unnecessary dependency
- product quality goal met
- manual QA sufficient
- validation commands correct

Return PASS/FAIL, blockers, concerns, validation before commit, suggested commit.
```

## Build-Fix Prompt

```text
Controlled build-fix mode.

Current failure:
PASTE FIRST REAL ERROR HERE

Task:
1. Identify root cause.
2. Inspect minimum files.
3. Propose smallest safe fix.
4. Apply only that fix.
5. Run failed validation command once.
6. Stop.

Do not refactor unrelated code.
Do not add dependencies unless clearly required.
Do not loop.
```
