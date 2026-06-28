# Phase 17D — Cloudflare Worker Foundation — Implementation Plan

## 1. Objective

Create the backend foundation for GymLedger Food Lookup Gateway using a TypeScript Cloudflare Worker.

This phase is backend-only.

It creates the Worker skeleton, endpoint conventions, response helpers, error helpers, API key middleware, local dev setup, and README.

This phase must not implement external food providers yet.

---

## 2. Product / Architecture Context

GymLedger is local-first and offline-capable.

The backend exists only to help with online-assisted lookup, cache, normalization, and optional future catalogs.

The backend is not a full SaaS.

Core rule:

```text
Cloud helps discover data.
Room owns saved data.
```

This phase does not store personal user data.

This phase does not affect Android behavior.

---

## 3. Phase Mapping

This phase maps to:

```text
Backend Phase B1 — Worker Foundation
Roadmap Phase 17D — Cloudflare Worker Foundation
```

Before implementing, confirm the B0 decisions:

```text
Repo location: same repo under worker/food-lookup
Endpoint strategy: workers.dev first
Auth strategy: simple personal API key via X-GymLedger-Key
Cost target: $0/month
Secrets strategy: no secrets committed
```

If any B0 decision is unresolved, report it during preflight.

---

## 4. Scope

### In scope

```text
worker/food-lookup project
TypeScript Worker skeleton
Wrangler config
health endpoint
config endpoint
JSON success response helper
JSON error response helper
stable error codes
simple API key middleware
README for setup/dev/deploy
basic tests if practical
```

### Out of scope

```text
Android app changes
Gradle changes
OkHttp
Retrofit
USDA
Open Food Facts
D1
KV
provider cache
barcode
Android remote lookup
food search
food normalization
user accounts
cloud sync
personal data storage
photo upload
paid APIs
custom domain
```

---

## 5. Recommended Project Location

Use:

```text
worker/food-lookup
```

Reason:

```text
Keeping the Worker inside the same repo keeps roadmap/docs/backend contract versioned together.
The Worker can be split into a separate repo later only if needed.
```

---

## 6. Files to Create

### `worker/food-lookup/package.json`

Purpose:

```text
Define Worker package scripts and dependencies.
```

Required scripts:

```json
{
  "scripts": {
    "dev": "wrangler dev",
    "typecheck": "tsc --noEmit",
    "test": "vitest run",
    "deploy": "wrangler deploy"
  }
}
```

Dependencies should be minimal.

Preferred:

```text
typescript
wrangler
vitest
@cloudflare/workers-types
```

Do not add frameworks unless justified during preflight.

No Hono in this phase unless the builder provides a strong reason and gets approval.

---

### `worker/food-lookup/tsconfig.json`

Purpose:

```text
TypeScript config for Worker.
```

Should include Cloudflare Worker types.

---

### `worker/food-lookup/vitest.config.ts`

Purpose:

```text
Test configuration if tests are added.
```

Keep simple.

---

### `worker/food-lookup/wrangler.toml`

Purpose:

```text
Cloudflare Worker config.
```

Required:

```text
name
main
compatibility_date
```

Preferred name:

```text
gymledger-food-lookup
```

Do not include secrets.

Do not include production account-specific sensitive values.

---

### `worker/food-lookup/src/index.ts`

Purpose:

```text
Worker entrypoint and route dispatch.
```

Required routes:

```text
GET /v1/health
GET /v1/config
```

Unknown routes should return stable `not_found` error.

Unsupported methods should return stable `method_not_allowed` error.

---

### `worker/food-lookup/src/response.ts`

Purpose:

```text
Reusable JSON response helpers.
```

Suggested success shape:

```json
{
  "ok": true,
  "data": {}
}
```

Suggested error shape:

```json
{
  "ok": false,
  "error": {
    "code": "not_found",
    "message": "Route not found"
  }
}
```

---

### `worker/food-lookup/src/errors.ts`

Purpose:

```text
Stable error code definitions.
```

Initial error codes:

```text
bad_request
unauthorized
forbidden
not_found
method_not_allowed
online_lookup_disabled
provider_disabled
budget_exceeded
provider_timeout
provider_error
internal_error
```

Not all codes need to be used in 17D, but they can be defined for contract stability.

---

### `worker/food-lookup/src/config.ts`

Purpose:

```text
Safe public runtime config.
```

`GET /v1/config` should return only safe values such as:

```json
{
  "onlineLookupAvailable": true,
  "providers": {
    "usda": false,
    "openFoodFacts": false
  },
  "features": {
    "genericFoodSearch": false,
    "barcodeLookup": false
  },
  "minQueryLength": 3,
  "safeMode": true
}
```

Important:

```text
Do not return secrets.
Do not return API keys.
Do not return internal Cloudflare account details.
```

---

### `worker/food-lookup/src/auth.ts`

Purpose:

```text
Simple API key helper for future protected endpoints.
```

Use header:

```text
X-GymLedger-Key
```

Rules:

```text
Read expected key from Worker environment binding.
Do not hardcode a real key.
If no expected key is configured in local dev, allow unprotected health/config or use a documented test key only.
Do not require auth for /v1/health.
Do not expose whether a key exists.
```

For this phase, it is acceptable for `/v1/health` and `/v1/config` to be public safe endpoints.

If adding a protected test route, it must be clearly marked dev/test only and not necessary for Android.

---

### `worker/food-lookup/src/index.test.ts`

Purpose:

```text
Basic tests for route behavior.
```

Test cases:

```text
GET /v1/health returns ok true
GET /v1/config returns safe config
unknown route returns not_found
unsupported method returns method_not_allowed
error response shape is stable
```

---

### `worker/food-lookup/README.md`

Purpose:

```text
Document local setup and usage.
```

Must include:

```text
What this Worker is
What this Worker is not
Local setup
Validation commands
Local curl examples
Secrets policy
Deployment notes
Current phase limitations
Future phases
```

Must explicitly say:

```text
No USDA provider yet.
No Open Food Facts provider yet.
No D1/KV cache yet.
No personal data storage.
No Android integration yet.
```

---

## 7. Files to Modify

Prefer no existing file modifications.

Allowed only if needed:

```text
.gitignore
```

Reason:

```text
Add Worker local secret/env ignores if missing.
```

If modified, add only:

```gitignore
worker/food-lookup/.dev.vars
worker/food-lookup/.wrangler/
worker/food-lookup/node_modules/
worker/food-lookup/dist/
```

Do not re-ignore `docs/` or `AGENTS.md`.

Do not modify Android files.

---

## 8. Secrets and Local Env Policy

Never commit:

```text
.dev.vars
.env
.env.*
Cloudflare API tokens
USDA API key
Open Food Facts credentials
personal API keys
```

For local testing, document:

```text
wrangler secret put GYMLEDGER_API_KEY
```

or `.dev.vars` local usage, but do not commit `.dev.vars`.

Expected env binding name:

```text
GYMLEDGER_API_KEY
```

---

## 9. Endpoint Contract

### `GET /v1/health`

Success:

```json
{
  "ok": true,
  "data": {
    "service": "gymledger-food-lookup",
    "status": "ok"
  }
}
```

No auth required.

No secrets.

---

### `GET /v1/config`

Success:

```json
{
  "ok": true,
  "data": {
    "onlineLookupAvailable": true,
    "providers": {
      "usda": false,
      "openFoodFacts": false
    },
    "features": {
      "genericFoodSearch": false,
      "barcodeLookup": false
    },
    "minQueryLength": 3,
    "safeMode": true
  }
}
```

No auth required unless the implementation plan explicitly decides otherwise.

No secrets.

---

### Unknown route

```json
{
  "ok": false,
  "error": {
    "code": "not_found",
    "message": "Route not found"
  }
}
```

---

### Unsupported method

```json
{
  "ok": false,
  "error": {
    "code": "method_not_allowed",
    "message": "Method not allowed"
  }
}
```

---

## 10. Implementation Order

1. Inspect root repo structure.
2. Confirm `worker/food-lookup` does not already exist.
3. Confirm `.gitignore` does not already cover Worker local secrets.
4. Create Worker package files.
5. Create TypeScript config.
6. Create Wrangler config.
7. Create response helpers.
8. Create error definitions.
9. Create config helper.
10. Create auth helper.
11. Create route dispatch in `index.ts`.
12. Add tests.
13. Add README.
14. Update `.gitignore` only if necessary.
15. Run validation.
16. Run manual curl QA.
17. Report files changed and results.

---

## 11. Validation Commands

From repo root:

```bash
cd worker/food-lookup
npm install
npm run typecheck
npm test
npm run dev
```

Manual curl QA while dev server is running:

```bash
curl http://localhost:8787/v1/health
curl http://localhost:8787/v1/config
curl http://localhost:8787/unknown
curl -X POST http://localhost:8787/v1/health
```

---

## 12. Quality Gates

From repo root:

```bash
git diff --name-status
git diff --stat
git diff --check
```

Android no-touch gate:

```bash
git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

Expected: no diff.

Provider no-call gate:

```bash
grep -R -nE "openfoodfacts|fdc\.nal\.usda|USDA|Open Food Facts" worker/food-lookup/src || true
```

Expected: no provider implementation. Error-code names or comments should avoid provider references in this phase unless clearly contract-only.

Secrets gate:

```bash
grep -R -nE "sk-|ghp_|github_pat_|CF_API_TOKEN|CLOUDFLARE_API_TOKEN|USDA_API_KEY|OPENAI_API_KEY|password\s*=" worker/food-lookup || true
```

Expected: no results.

Docs/source-of-truth gate:

```bash
grep -R -n "Phase 17D" docs/CURRENT_PHASE.md docs/IMPLEMENTATION_PLAN.md
```

Expected: both files mention Phase 17D.

---

## 13. Manual QA Checklist

* Worker dev server starts.
* `/v1/health` returns `ok: true`.
* `/v1/config` returns `ok: true`.
* `/v1/config` exposes no secrets.
* Unknown route returns `not_found`.
* Unsupported method returns `method_not_allowed`.
* README commands are accurate.
* No Android files changed.
* No provider calls exist.
* No `.dev.vars`, tokens, or secrets are staged.
* Cost target remains $0/month.

---

## 14. Risks and Guardrails

### Risk: Overbuilding backend too early

Mitigation:

```text
No providers.
No D1.
No KV.
No barcode.
No Android integration.
```

### Risk: Secret leakage

Mitigation:

```text
No real keys in repo.
Use Cloudflare secrets or local .dev.vars ignored by Git.
```

### Risk: Backend contract drift

Mitigation:

```text
Use stable response shape from the first Worker phase.
```

### Risk: Modifying Android accidentally

Mitigation:

```text
Android no-touch gate must show no diff.
```

---

## 15. Builder Preflight Requirements

Before editing, builder must report:

```text
1. Whether worker/food-lookup already exists.
2. Whether package manager context is npm/pnpm/yarn.
3. Files to create.
4. Files to modify.
5. Whether .gitignore needs worker additions.
6. Exact validation commands.
7. Manual curl QA.
8. Any blocker.
```

Builder must stop and wait for approval.

---

## 16. Suggested Commit

```text
feat: add food lookup worker foundation
```
