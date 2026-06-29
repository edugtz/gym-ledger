# Phase 17E.1 — Worker D1 Cache and Budget Foundation — Implementation Plan

## Objective

Add the Cloudflare D1 cache and budget foundation for GymLedger Food Lookup Worker.

This phase prepares the Worker for safe, low-cost provider integration in later Phase 17E steps.

This phase does not call USDA.

This phase does not call Open Food Facts.

This phase does not modify Android.

## Product / Architecture Context

GymLedger remains local-first and offline-capable.

```text
Cloud helps discover data.
Room owns saved data.
```

The Worker is optional online-assisted infrastructure. It may help lookup, normalize, cache, and budget external food data in future phases, but it must not become the source of truth for user meals, workouts, body data, photos, or saved personal data.

The purpose of this phase is to add the backend storage and guardrails needed before any external provider is allowed.

## Phase Mapping

This phase implements:

```text
Backend Phase B2 — D1 Cache and Budget Foundation
```

It is the first implementation slice of:

```text
Phase 17E — Worker Food Providers and Cache
```

Provider work is intentionally deferred.

## In Scope

Create or update Worker backend files only.

### D1 Setup

Add Cloudflare D1 support for the Worker:

```text
- D1 database binding in wrangler.toml
- local/remote migration folder
- initial migration SQL
```

The D1 binding should be named:

```text
DB
```

Recommended D1 database name:

```text
gymledger-food-lookup
```

If Cloudflare creates a different database id, update only `database_id` in `wrangler.toml`.

### Tables

Add initial schema for:

```text
food_lookup_cache
usage_daily
runtime_config
```

### food_lookup_cache

Purpose:

Cache normalized lookup results returned by future providers.

This table must store normalized Worker DTOs, not raw provider payloads.

Suggested columns:

```text
cache_key TEXT PRIMARY KEY
source TEXT NOT NULL
lookup_type TEXT NOT NULL
query TEXT NOT NULL
normalized_json TEXT NOT NULL
attribution TEXT
is_approximate INTEGER NOT NULL DEFAULT 1
created_at TEXT NOT NULL
updated_at TEXT NOT NULL
expires_at TEXT NOT NULL
hit_count INTEGER NOT NULL DEFAULT 0
last_hit_at TEXT
```

Notes:

```text
cache_key    = stable key such as generic:egg or barcode:7501000112345
source       = future values like USDA or OpenFoodFacts
lookup_type  = generic, search, barcode
query        = original normalized query/barcode
```

Do not store personal user data.

Do not store meal logs.

Do not store Android device/user identifiers.

### usage_daily

Purpose:

Track daily Worker/provider usage to prevent accidental cost/rate-limit problems.

Suggested columns:

```text
usage_date TEXT PRIMARY KEY
external_calls INTEGER NOT NULL DEFAULT 0
cache_hits INTEGER NOT NULL DEFAULT 0
cache_misses INTEGER NOT NULL DEFAULT 0
blocked_calls INTEGER NOT NULL DEFAULT 0
last_updated_at TEXT NOT NULL
```

Notes:

```text
usage_date = YYYY-MM-DD in UTC
```

The table should support future logic:

```text
- increment external_calls before/after provider call
- increment cache_hits on cache hit
- increment cache_misses on cache miss
- increment blocked_calls when budget/safe-mode blocks a call
```

No provider calls happen in this phase.

### runtime_config

Purpose:

Allow safe operational switches without Android releases.

Suggested columns:

```text
key TEXT PRIMARY KEY
value TEXT NOT NULL
updated_at TEXT NOT NULL
```

Required default keys:

```text
safe_mode = true
online_lookup_enabled = false
daily_external_call_budget = 25
```

Meaning:

```text
safe_mode = true
  Conservative mode. Future provider calls should be blocked unless a later phase explicitly enables them.

online_lookup_enabled = false
  Future remote lookup endpoints should not call providers until enabled.

daily_external_call_budget = 25
  Future cap for external provider calls per UTC day.
```

This phase may expose these values through `/v1/config` only if they are safe and public.

## Helper Modules

Create small focused modules under:

```text
worker/food-lookup/src
```

Recommended files:

```text
src/db.ts
src/cache.ts
src/usage.ts
src/runtimeConfig.ts
```

### db.ts

Purpose:

Centralize D1 environment typing and small DB utilities.

Should define or support:

```text
Env.DB: D1Database
```

Existing `Env` type in `src/index.ts` may be moved or expanded if cleaner.

### cache.ts

Purpose:

Provide cache helper functions for normalized lookup data.

Suggested functions:

```text
buildCacheKey(input): string
getCachedFoodLookup(db, cacheKey, now): Promise<CachedFoodLookup | null>
putCachedFoodLookup(db, entry): Promise<void>
```

Behavior:

```text
- expired cache returns null
- cache hit increments hit_count and last_hit_at
- put writes normalized JSON only
- no raw provider payload is stored
```

### usage.ts

Purpose:

Provide daily usage/budget helper functions.

Suggested functions:

```text
getUsageDate(now): string
getDailyUsage(db, usageDate): Promise<DailyUsage>
incrementCacheHit(db, usageDate): Promise<void>
incrementCacheMiss(db, usageDate): Promise<void>
incrementExternalCall(db, usageDate): Promise<void>
incrementBlockedCall(db, usageDate): Promise<void>
isDailyBudgetExceeded(usage, budget): boolean
```

Behavior:

```text
- creates row if missing
- uses UTC date
- does not call providers
```

### runtimeConfig.ts

Purpose:

Read safe runtime settings from D1 with conservative fallbacks.

Suggested functions:

```text
getRuntimeConfig(db): Promise<RuntimeConfig>
getRuntimeConfigValue(db, key): Promise<string | null>
```

Fallbacks if D1 config is missing:

```text
safeMode: true
onlineLookupEnabled: false
dailyExternalCallBudget: 25
```

## Endpoint Changes

Keep existing endpoints:

```text
GET /v1/health
GET /v1/config
```

### GET /v1/health

Keep behavior stable.

Do not require D1 for health to return OK unless the implementation explicitly adds a separate D1 diagnostic field.

Recommended response remains lightweight:

```json
{
  "ok": true,
  "data": {
    "status": "ok"
  }
}
```

### GET /v1/config

Update only if useful and safe.

Allowed public fields:

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
  "safeMode": true,
  "cacheAvailable": true,
  "budgetEnabled": true
}
```

Do not expose secrets.

Do not expose database ids.

Do not expose internal Cloudflare account data.

## Tests

Keep existing tests passing.

Add tests for:

```text
- cache key generation
- expired cache miss
- valid cache hit
- cache write stores normalized JSON
- UTC usage date
- daily usage default row behavior
- budget exceeded behavior
- runtime config fallback values
- /v1/config remains safe/public
```

Tests may use mocked D1 behavior or isolated helper-level tests if full D1 local test setup is too heavy.

Do not make tests depend on external network.

Do not call Cloudflare remote services during unit tests.

## Files to Create

Expected:

```text
worker/food-lookup/migrations/0001_cache_budget_foundation.sql
worker/food-lookup/src/db.ts
worker/food-lookup/src/cache.ts
worker/food-lookup/src/usage.ts
worker/food-lookup/src/runtimeConfig.ts
```

Optional if needed:

```text
worker/food-lookup/src/types.ts
worker/food-lookup/src/cache.test.ts
worker/food-lookup/src/usage.test.ts
worker/food-lookup/src/runtimeConfig.test.ts
```

## Files to Modify

Expected:

```text
worker/food-lookup/wrangler.toml
worker/food-lookup/src/index.ts
worker/food-lookup/src/config.ts
worker/food-lookup/src/index.test.ts
worker/food-lookup/README.md
```

Optional if required:

```text
worker/food-lookup/src/errors.ts
worker/food-lookup/package.json
worker/food-lookup/package-lock.json
```

Only modify `package.json` if a test/dev dependency is truly required.

Prefer avoiding new dependencies.

## Files Not to Touch

Do not modify:

```text
app/src
build.gradle.kts
settings.gradle.kts
gradle/libs.versions.toml
gradle/
```

Do not modify Android UI, repositories, Room entities, DataStore, Navigation, or Gradle.

Do not modify unrelated docs unless explicitly approved.

## Out of Scope

Do not implement:

```text
USDA provider
Open Food Facts provider
barcode lookup
generic provider lookup endpoint
GET /v1/foods/generic
GET /v1/foods/search
GET /v1/foods/barcode/:barcode
Android remote lookup integration
OkHttp
Retrofit
Android Settings changes
custom domain
Cloudflare Access
user accounts
cloud sync
personal data storage
paid runtime APIs
external AI runtime APIs
```

## Secrets Policy

Do not commit:

```text
.dev.vars
.env
.env.*
Cloudflare API tokens
USDA API key
Open Food Facts credentials
personal API keys
```

This phase should not require production secrets.

If a local D1 database id is generated, do not treat it as a secret, but avoid unnecessary churn in docs.

## Implementation Order

1. Confirm branch and clean state.
2. Inspect current Worker files.
3. Update `wrangler.toml` with D1 binding.
4. Add migration SQL.
5. Add shared types/helpers.
6. Add cache helper.
7. Add usage/budget helper.
8. Add runtime config helper.
9. Update config endpoint only with safe public flags if needed.
10. Add/update tests.
11. Update Worker README with D1 local setup and migration commands.
12. Run validation.
13. Stop for review.

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

If local D1 database has not been created yet, create it manually before applying migrations:

```bash
npx wrangler d1 create gymledger-food-lookup
```

Then copy the generated `database_id` into `wrangler.toml`.

Remote migration should not be applied until local tests pass and the user explicitly approves.

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

No Android manual QA required.

Worker manual QA after local validation:

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

## Acceptance Criteria

Phase 17E.1 is complete when:

```text
D1 binding is configured.
Initial migration exists.
food_lookup_cache schema exists.
usage_daily schema exists.
runtime_config schema exists.
Cache helpers exist and are tested.
Usage/budget helpers exist and are tested.
Runtime config fallback behavior exists and is tested.
Existing health/config endpoints still pass.
No provider calls exist.
No Android files are modified.
No secrets are committed.
README documents D1 setup and validation.
npm run typecheck passes.
npm test passes.
```

## Suggested Commit

```text
feat: add worker cache and budget foundation
```

The user commits manually.
