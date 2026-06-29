# Phase 17E.1 — Worker D1 Cache and Budget Foundation

## Objective

Add the Cloudflare D1 cache and budget foundation for GymLedger Food Lookup Worker.

This phase prepares the Worker for safe, low-cost provider integration in later Phase 17E steps.

This phase does not call USDA.

This phase does not call Open Food Facts.

This phase does not modify Android.

## Product Quality Goal

GymLedger should have backend guardrails before any external food provider is introduced.

This phase should establish:

* D1 database binding
* local/remote migration structure
* normalized food lookup cache schema
* daily usage/budget tracking schema
* runtime config schema
* cache helper functions
* usage/budget helper functions
* safe-mode / kill-switch foundation
* tests for cache, usage, budget, and runtime config logic
* README instructions for D1 setup and validation

## Recommended AI Route

* Planning: ChatGPT
* Optional second planning pass: Qwen3.6 35B local or Gemma 4 31B
* Builder: Qwen Coder 30B 5bit local
* Alternative builder: OpenCode Go Qwen3.6 Plus
* Review: ChatGPT with GitHub connector
* Debug/review escalation: OpenCode Go DeepSeek V4 Pro
* Codex: only if D1/Wrangler behavior becomes confusing or repo state becomes broken
* Gemini: not needed because this phase does not touch Android

## Scope

Implement only Backend Phase B2:

```text
D1 Cache and Budget Foundation
```

This is the first implementation slice of Phase 17E.

Provider work is intentionally deferred.

## Tasks

* Add Cloudflare D1 binding to the Worker configuration.
* Add initial D1 migration folder and SQL migration.
* Create `food_lookup_cache` table.
* Create `usage_daily` table.
* Create `runtime_config` table.
* Add cache key and cache read/write helpers.
* Add daily usage tracking helpers.
* Add budget checking helpers.
* Add runtime config helpers with conservative fallbacks.
* Keep `/v1/health` stable.
* Keep `/v1/config` safe and public.
* Add or update tests for cache, usage, budget, and runtime config behavior.
* Update Worker README with D1 local setup, migration, and validation notes.

## D1 Binding

Use this binding name:

```text
DB
```

Recommended D1 database name:

```text
gymledger-food-lookup
```

If Cloudflare generates a database id, update only the Worker D1 binding config.

Do not commit secrets.

## Tables

Required tables:

```text
food_lookup_cache
usage_daily
runtime_config
```

## Do Not Do

* Do not modify Android app code.
* Do not modify Android Gradle files.
* Do not add OkHttp.
* Do not add Retrofit.
* Do not call USDA.
* Do not call Open Food Facts.
* Do not add barcode lookup.
* Do not add `GET /v1/foods/generic`.
* Do not add `GET /v1/foods/search`.
* Do not add `GET /v1/foods/barcode/:barcode`.
* Do not add Android remote lookup integration.
* Do not store personal workouts, meals, body measurements, photos, device ids, user ids, API keys, or secrets.
* Do not store raw provider payloads.
* Do not require user accounts.
* Do not require custom domain.
* Do not add paid provider dependencies.
* Do not implement future backend phases.
* Do not commit; the user commits manually.

## Acceptance Criteria

* D1 binding is configured.
* Initial migration exists.
* `food_lookup_cache` schema exists.
* `usage_daily` schema exists.
* `runtime_config` schema exists.
* Cache helpers exist and are tested.
* Usage/budget helpers exist and are tested.
* Runtime config fallback behavior exists and is tested.
* Existing `/v1/health` endpoint still passes.
* Existing `/v1/config` endpoint still passes.
* No external provider calls exist.
* No Android files are modified.
* No secrets are committed.
* README documents D1 setup and validation.
* `npm run typecheck` passes.
* `npm test` passes.

## Validation Commands

From repo root:

```bash
git status --short --untracked-files=all
```

From Worker folder:

```bash
cd worker/food-lookup
npm run typecheck
npm test
```

D1 local migration validation:

```bash
npx wrangler d1 migrations list gymledger-food-lookup --local
npx wrangler d1 migrations apply gymledger-food-lookup --local
```

If the D1 database has not been created yet:

```bash
npx wrangler d1 create gymledger-food-lookup
```

Then copy the generated `database_id` into `worker/food-lookup/wrangler.toml`.

Remote migration should not be applied until local validation passes and the user explicitly approves.

## Quality Gates

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

Provider no-call gate:

```bash
grep -R -nE "openfoodfacts|fdc\.nal\.usda|api\.nal\.usda\.gov|world\.openfoodfacts\.org" worker/food-lookup/src || true
```

Secret no-commit gate:

```bash
grep -R -nE "secret|token|password|api[_-]?key" worker/food-lookup \
  --exclude README.md \
  --exclude package-lock.json \
  --exclude auth.ts || true
```

Raw provider payload gate:

```bash
grep -R -nE "rawPayload|raw_json|providerPayload|usdaResponse|openFoodFactsResponse" worker/food-lookup/src || true
```

## Manual QA

No Android manual QA is required.

Worker local manual QA:

```bash
cd worker/food-lookup
npm run dev
```

Then in another terminal:

```bash
curl -i http://localhost:8787/v1/health
curl -i http://localhost:8787/v1/config
```

Remote deploy is optional at the end of this phase and should happen only after local validation and review.
