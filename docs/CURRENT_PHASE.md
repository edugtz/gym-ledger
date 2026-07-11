# Phase 17E.2 — USDA Generic Food Lookup Provider

## Objective

Add generic food lookup through USDA FoodData Central to the GymLedger Food Lookup Worker.

This phase builds on the D1 cache, usage budget, and runtime configuration foundation created in Phase 17E.1.

The Worker must:

* search USDA FoodData Central for generic foods
* normalize provider results into a stable GymLedger DTO
* return calories, protein, carbohydrates, and fat per 100 g
* include source attribution
* mark provider-derived nutrition as approximate
* use cache before calling USDA
* enforce safe mode, online lookup configuration, and daily external-call budget
* handle provider failures with stable public errors
* keep provider payloads private
* keep Android unchanged

This phase does not implement Open Food Facts.

This phase does not implement barcode lookup.

This phase does not modify Android.

## Product Quality Goal

GymLedger should provide useful generic-food suggestions without making USDA part of the app's local source of truth.

Cloud helps discover data.

Room owns saved data.

USDA results are suggestions only. They must be:

* normalized
* cacheable
* reviewable
* editable by the user in a later Android phase
* clearly attributed
* clearly marked approximate
* non-blocking when the provider is unavailable

## Recommended AI Route

* Planning: ChatGPT
* Optional second planning review: Qwen3.6 35B A3B local
* Preferred builder: OpenCode Go Qwen3.6 Plus
* Local builder alternative: Qwen Coder 30B A3B 5bit
* Local focused patch model: Devstral Small 2 24B 6bit
* Local review: Qwen3.6 27B 8bit
* Debug escalation: OpenCode Go DeepSeek V4 Pro
* Cloudflare/provider edge-case review: Codex
* Gemini: not needed because this phase does not touch Android

## Scope

Implement only Backend Phase B3:

```text
USDA Provider
```

This is the second implementation slice of Phase 17E.

Phase 17E.1 is already complete and provides:

* D1 binding
* cache schema and helpers
* daily usage counters
* external-call budget logic
* runtime config and safe defaults
* Worker health/config endpoints

Use those foundations. Do not replace them with a separate implementation.

## Required Endpoint

Add:

```text
GET /v1/foods/generic?q=<query>
```

Example:

```text
GET /v1/foods/generic?q=egg
```

## Query Rules

* Method must be GET.
* Query parameter name must be `q`.
* Trim surrounding whitespace.
* Reject missing queries.
* Reject queries shorter than the existing public `minQueryLength`.
* Do not support search-as-you-type.
* Do not accept arbitrary provider parameters from the client.
* Do not expose USDA pagination controls directly to Android.
* Limit provider results to a small fixed number.
* Normalize the query before building the cache key.
* Use UTC dates for budget tracking.

## Provider

Use USDA FoodData Central.

Provider base URL:

```text
https://api.nal.usda.gov/fdc/v1
```

Use the USDA food search endpoint.

The USDA API key must come from the Worker environment:

```text
USDA_API_KEY
```

The API key must not be:

* committed
* included in responses
* included in logs
* stored in D1
* sent to Android
* placed in `wrangler.toml` as a plaintext variable

Use Cloudflare secrets for deployed environments.

A local `.dev.vars` file may be used but must remain ignored by Git.

## Runtime Gates

Before making an external USDA request, enforce:

1. `safe_mode`
2. `online_lookup_enabled`
3. `usda_provider_enabled`
4. `daily_external_call_budget`

Required behavior:

* If `safe_mode = true`, block the external call.
* If `online_lookup_enabled = false`, block the external call.
* If `usda_provider_enabled = false`, block the external call.
* If the daily budget is exhausted, block the external call.
* Increment `blocked_calls` when a runtime or budget gate blocks the provider call.
* Do not consume external-call budget for cache hits.
* Increment `external_calls` only when an actual USDA request is attempted.
* Increment `cache_hits` for valid cache hits.
* Increment `cache_misses` when no valid cache result exists.

Runtime defaults from Phase 17E.1 remain conservative.

This phase may require explicit local test overrides to exercise provider logic.

Do not change production-safe defaults merely to make tests pass.

## Cache Behavior

Use the existing D1 cache foundation.

Cache key pattern:

```text
usda:generic:<normalized-query>
```

Cache lookup happens before runtime provider gates.

A valid non-expired cache hit should:

* return the normalized cached DTO
* increment the cache entry hit count
* increment daily `cache_hits`
* avoid a USDA request
* avoid incrementing `external_calls`

A cache miss should:

* increment daily `cache_misses`
* evaluate runtime/provider/budget gates
* call USDA only when allowed
* normalize the result
* cache normalized Worker DTOs
* never cache raw USDA payloads

Expired cache entries must not be returned as valid results.

## Provider Request Rules

* Use `fetch`.
* Use an explicit timeout with `AbortController`.
* Use a small fixed result count.
* Prefer generic USDA data types suitable for generic food lookup.
* Do not use USDA branded-food results as the primary generic-food result set.
* Do not make one provider detail request per search result.
* Do not create an N+1 request pattern.
* Do not retry repeatedly in the same request.
* Do not log provider payloads.
* Do not expose USDA response fields directly.

Recommended request strategy:

```text
POST /foods/search
```

with a controlled request body.

## Normalized DTO

Return a stable Worker-owned DTO.

Suggested response shape:

```json
{
  "query": "egg",
  "source": "USDA",
  "attribution": "USDA FoodData Central",
  "isApproximate": true,
  "results": [
    {
      "externalId": "123456",
      "name": "Egg, whole, cooked",
      "description": "Egg, whole, cooked",
      "dataType": "Foundation",
      "nutritionPer100g": {
        "caloriesKcal": 155,
        "proteinG": 12.6,
        "carbohydrateG": 1.1,
        "fatG": 10.6
      }
    }
  ]
}
```

Required top-level fields:

* `query`
* `source`
* `attribution`
* `isApproximate`
* `results`

Required result fields:

* `externalId`
* `name`
* `description`
* `dataType`
* `nutritionPer100g`

Required nutrient fields:

* `caloriesKcal`
* `proteinG`
* `carbohydrateG`
* `fatG`

Rules:

* Use `null` when a nutrient is unavailable.
* Do not invent nutrient values.
* Do not convert missing values to zero.
* Do not expose raw nutrient arrays.
* Do not expose the provider API key.
* Do not include provider payloads.
* Do not include personal user data.
* Keep field names stable and Android-friendly.
* User-facing names/descriptions remain provider data.
* API field names remain English.

## Nutrient Mapping

Normalize USDA nutrient data to:

```text
caloriesKcal
proteinG
carbohydrateG
fatG
```

The normalizer must handle provider variation safely.

Map nutrients using stable nutrient identifiers when available.

Do not rely only on display-name string matching when a stable nutrient number or identifier exists.

Support common USDA representations for:

* Energy in kcal
* Protein in g
* Carbohydrate by difference in g
* Total lipid/fat in g

If energy is supplied in kilojoules but no kcal value exists, conversion may be performed only in a centralized tested normalizer.

Do not perform silent unit conversions elsewhere.

Nutrition must represent values per 100 g.

If a provider result does not provide a reliable per-100-g basis, either:

* omit the result, or
* return unavailable nutrient fields as `null`

Do not fabricate serving conversions.

## Public Errors

Use the existing stable error response shape.

Required error codes:

```text
invalid_query
lookup_disabled
provider_disabled
budget_exceeded
provider_timeout
provider_rate_limited
provider_unavailable
provider_error
not_found
configuration_error
```

Required behavior:

* Missing or too-short query: `invalid_query`
* Safe mode or online lookup disabled: `lookup_disabled`
* USDA provider disabled: `provider_disabled`
* Daily external-call budget exhausted: `budget_exceeded`
* USDA timeout: `provider_timeout`
* USDA HTTP 429: `provider_rate_limited`
* USDA temporary 5xx failure: `provider_unavailable`
* Invalid/unexpected provider response: `provider_error`
* Valid search with zero usable normalized results: `not_found`
* Missing USDA API key when a provider call is otherwise allowed: `configuration_error`

Do not expose:

* USDA response bodies
* stack traces
* API keys
* internal D1 errors
* internal exception messages

## Files Expected

Likely files to create:

```text
worker/food-lookup/src/providers/usda.ts
worker/food-lookup/src/providers/usda.test.ts
worker/food-lookup/src/normalizers/usdaFood.ts
worker/food-lookup/src/normalizers/usdaFood.test.ts
worker/food-lookup/src/types/foodLookup.ts
```

Likely files to modify:

```text
worker/food-lookup/src/index.ts
worker/food-lookup/src/index.test.ts
worker/food-lookup/src/cache.ts
worker/food-lookup/src/runtimeConfig.ts
worker/food-lookup/src/usage.ts
worker/food-lookup/src/errors.ts
worker/food-lookup/README.md
```

Modify only files that are actually required after preflight inspection.

## Do Not Do

* Do not modify Android app code.
* Do not modify Android Gradle files.
* Do not add OkHttp.
* Do not add Retrofit.
* Do not add Open Food Facts.
* Do not add barcode lookup.
* Do not add packaged-food lookup.
* Do not add `GET /v1/foods/search`.
* Do not add `GET /v1/foods/barcode/:barcode`.
* Do not deploy the Worker.
* Do not apply remote D1 migrations.
* Do not add authentication changes unless required to preserve existing behavior.
* Do not expose raw USDA payloads.
* Do not store raw USDA payloads.
* Do not store API keys in D1.
* Do not log API keys.
* Do not store personal meals, workouts, body measurements, photos, device IDs, or user IDs.
* Do not implement Android integration.
* Do not implement Phase 17E.3, 17E.4, or 17F.
* Do not change safe defaults to enabled.
* Do not commit; the user commits manually.

## Acceptance Criteria

* `GET /v1/foods/generic?q=egg` exists.
* Missing/short query returns `invalid_query`.
* Cache lookup happens before an external provider call.
* Valid cache hit avoids USDA.
* Valid cache hit increments cache hit counters.
* Cache miss increments cache miss counters.
* Safe mode blocks external requests.
* Disabled online lookup blocks external requests.
* Disabled USDA provider blocks external requests.
* Exhausted daily budget blocks external requests.
* Blocked provider attempts increment `blocked_calls`.
* Actual provider attempts increment `external_calls`.
* USDA API key comes from `USDA_API_KEY`.
* Missing key returns `configuration_error` when provider access is otherwise enabled.
* USDA request uses an explicit timeout.
* HTTP 429 maps to `provider_rate_limited`.
* Provider timeout maps to `provider_timeout`.
* Provider 5xx maps to `provider_unavailable`.
* Unexpected provider responses map to `provider_error`.
* Zero usable results maps to `not_found`.
* Results use a stable Worker-owned DTO.
* Calories, protein, carbohydrates, and fat are normalized per 100 g.
* Missing nutrients return `null`, not fabricated zero values.
* Results include USDA attribution.
* Results are marked approximate.
* Only normalized DTOs are cached.
* No raw USDA payloads are stored or returned.
* Existing `/v1/health` remains stable.
* Existing `/v1/config` remains safe and stable.
* No Open Food Facts code exists.
* No Android files are modified.
* No secrets are committed.
* README documents USDA local setup and validation.
* `npm run typecheck` passes.
* `npm test` passes.

## Validation Commands

From repo root:

```bash
git status --short --untracked-files=all
git diff --name-status
git diff --stat
git diff --check
```

Android no-touch gate:

```bash
git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

From Worker folder:

```bash
cd worker/food-lookup
npm run typecheck
npm test
```

D1 local validation:

```bash
npx wrangler d1 migrations list gymledger-food-lookup --local
npx wrangler d1 migrations apply gymledger-food-lookup --local
```

Provider no-scope gate:

```bash
grep -R -nE "openfoodfacts|world\.openfoodfacts\.org|barcode" worker/food-lookup/src || true
```

Raw payload no-storage gate:

```bash
grep -R -nE "rawPayload|raw_json|providerPayload|usdaResponse" worker/food-lookup/src || true
```

Secret no-commit gate:

```bash
git grep -nE "USDA_API_KEY\s*=|api_key=[A-Za-z0-9_-]{10,}" -- \
  ':!worker/food-lookup/README.md' \
  ':!docs/CURRENT_PHASE.md' \
  ':!docs/IMPLEMENTATION_PLAN.md' || true
```

## Manual QA

Manual provider QA requires:

* a local `.dev.vars` file
* a valid `USDA_API_KEY`
* explicit local runtime config overrides enabling lookup
* local D1 migrations applied

Do not commit `.dev.vars`.

Start Worker:

```bash
cd worker/food-lookup
npm run dev
```

Then test:

```bash
curl -i "http://localhost:8787/v1/health"
curl -i "http://localhost:8787/v1/config"
curl -i "http://localhost:8787/v1/foods/generic?q=egg"
curl -i "http://localhost:8787/v1/foods/generic?q="
curl -i "http://localhost:8787/v1/foods/generic?q=ab"
```

Repeat the same valid query to verify cache behavior.

Manual QA must confirm:

* first allowed lookup may call USDA
* second identical lookup is served from cache
* cache hit does not increment external calls
* attribution is present
* nutrients are normalized per 100 g
* no raw USDA response fields appear
* disabling provider/runtime config blocks the external call
* no API key appears in response or logs

## Stop Conditions

Stop implementation and report before continuing if:

* the USDA API response does not provide enough information for reliable per-100-g normalization
* provider data types require a product decision
* nutrient identifiers conflict with existing project documentation
* Phase 17E.1 helpers require a breaking schema change
* a remote D1 migration appears necessary
* Android files would need modification
* a secret appears in tracked files
* tests require live USDA network access
* the implementation would add Open Food Facts or barcode scope
* the first real validation error cannot be fixed in two focused attempts

## Suggested Commit

```text
feat: add USDA generic food lookup provider
```
