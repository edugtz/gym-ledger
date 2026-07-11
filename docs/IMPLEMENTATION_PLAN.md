# Phase 17E.2 Implementation Plan — USDA Generic Food Lookup Provider

## 1. Goal

Implement the USDA generic-food provider slice of Phase 17E.

The Worker will add:

```text
GET /v1/foods/generic?q=<query>
```

The endpoint will:

1. validate and normalize the query
2. check D1 cache
3. return a valid normalized cache hit immediately
4. evaluate safe-mode, provider, and budget gates on cache miss
5. call USDA only when allowed
6. normalize provider results into a Worker-owned DTO
7. cache only normalized DTOs
8. return stable errors without exposing provider internals

No Android work is included.

No Open Food Facts work is included.

## 2. Existing Foundation to Reuse

Phase 17E.1 already provides:

```text
worker/food-lookup/src/db.ts
worker/food-lookup/src/cache.ts
worker/food-lookup/src/usage.ts
worker/food-lookup/src/runtimeConfig.ts
worker/food-lookup/migrations/0001_cache_budget_foundation.sql
```

Existing tables:

```text
food_lookup_cache
usage_daily
runtime_config
```

Existing runtime keys:

```text
safe_mode
online_lookup_enabled
usda_provider_enabled
open_food_facts_provider_enabled
generic_food_search_enabled
barcode_lookup_enabled
daily_external_call_budget
cache_enabled
cache_ttl_seconds
```

Do not create duplicate cache, usage, or runtime-config systems.

## 3. Implementation Strategy

Recommended request flow:

```text
request
  -> route/method validation
  -> query validation and normalization
  -> cache key generation
  -> cache read
  -> valid cache hit?
       yes:
         increment cache entry hit count
         increment usage cache_hits
         return normalized cached response
       no:
         increment usage cache_misses
         evaluate runtime gates
         evaluate provider configuration
         evaluate daily budget
         call USDA with timeout
         increment external_calls
         normalize results
         validate normalized response
         cache normalized response
         return normalized response
```

Do not place all logic directly inside `index.ts`.

Keep route orchestration small.

## 4. Proposed File Structure

Create:

```text
worker/food-lookup/src/types/foodLookup.ts
worker/food-lookup/src/providers/usda.ts
worker/food-lookup/src/providers/usda.test.ts
worker/food-lookup/src/normalizers/usdaFood.ts
worker/food-lookup/src/normalizers/usdaFood.test.ts
worker/food-lookup/src/services/genericFoodLookup.ts
worker/food-lookup/src/services/genericFoodLookup.test.ts
```

Modify as needed:

```text
worker/food-lookup/src/index.ts
worker/food-lookup/src/index.test.ts
worker/food-lookup/src/cache.ts
worker/food-lookup/src/cache.test.ts
worker/food-lookup/src/usage.ts
worker/food-lookup/src/usage.test.ts
worker/food-lookup/src/runtimeConfig.ts
worker/food-lookup/src/runtimeConfig.test.ts
worker/food-lookup/src/errors.ts
worker/food-lookup/src/config.ts
worker/food-lookup/README.md
```

Do not create every proposed file blindly.

The builder must inspect the existing code and choose the smallest coherent structure.

## 5. Environment Contract

Extend the Worker environment with:

```ts
USDA_API_KEY?: string;
```

Existing D1 binding remains:

```ts
DB: D1Database;
```

Rules:

* `USDA_API_KEY` is optional at TypeScript compile time.
* Provider execution requires it at runtime.
* Missing key returns `configuration_error`.
* Do not add the key to `wrangler.toml`.
* Do not add the key to `[vars]`.
* Use `.dev.vars` locally.
* Use `wrangler secret put USDA_API_KEY` for deployed environments.
* Do not commit `.dev.vars`.

## 6. Query Normalization

Create one reusable query normalizer.

Required normalization:

```text
trim surrounding whitespace
collapse repeated internal whitespace
lowercase for cache key
preserve a clean display query for response
```

Example:

```text
input:  "  Chicken   Breast "
display query: "Chicken Breast"
cache query: "chicken breast"
cache key: "usda:generic:chicken breast"
```

Do not:

* strip meaningful punctuation indiscriminately
* accept empty normalized queries
* accept queries shorter than `minQueryLength`
* allow client-supplied USDA filters
* allow client-supplied result limits above the internal fixed limit

## 7. DTO Contract

Define Worker-owned TypeScript types.

Suggested types:

```ts
export interface GenericFoodLookupResponse {
  query: string;
  source: "USDA";
  attribution: string;
  isApproximate: true;
  results: GenericFoodResult[];
}

export interface GenericFoodResult {
  externalId: string;
  name: string;
  description: string;
  dataType: string;
  nutritionPer100g: NutritionPer100g;
}

export interface NutritionPer100g {
  caloriesKcal: number | null;
  proteinG: number | null;
  carbohydrateG: number | null;
  fatG: number | null;
}
```

Optional internal fields must not leak into the public response unless approved.

Do not expose:

```text
foodNutrients
foodMeasures
marketCountry
publicationDate
modifiedDate
dataSource
scientificName
raw response payload
API metadata unrelated to Android
```

## 8. USDA Provider Client

Create a small provider client.

Suggested interface:

```ts
export interface UsdaSearchOptions {
  query: string;
  pageSize: number;
  signal: AbortSignal;
}

export interface UsdaProviderClient {
  searchGenericFoods(
    options: UsdaSearchOptions
  ): Promise<unknown>;
}
```

A concrete implementation may receive:

```ts
apiKey
fetch implementation
base URL
```

through dependency injection.

This enables deterministic tests without live network calls.

Recommended provider request:

```text
POST https://api.nal.usda.gov/fdc/v1/foods/search?api_key=<secret>
```

Suggested controlled body:

```json
{
  "query": "egg",
  "pageSize": 10,
  "dataType": [
    "Foundation",
    "SR Legacy",
    "Survey (FNDDS)"
  ]
}
```

The exact accepted USDA data-type labels must be verified against provider behavior before finalizing.

Do not include `Branded` in the default generic-food search unless a later product decision approves it.

Provider client responsibilities:

* build URL safely
* include API key
* use POST JSON
* accept an AbortSignal
* use fixed page size
* map HTTP status to internal provider errors
* parse JSON once
* validate minimum response shape
* return provider data only to the normalizer/service layer

Provider client must not:

* return a public `Response`
* build GymLedger API response envelopes
* write to D1
* update usage counters
* log raw response payloads
* retry repeatedly

## 9. Timeout

Use `AbortController`.

Recommended timeout:

```text
5 seconds
```

Keep timeout in a named constant.

Tests should use a shorter injected timeout or mocked fetch rather than waiting five real seconds.

Map aborts caused by the timeout to:

```text
provider_timeout
```

Do not map unrelated runtime errors to timeout.

## 10. Provider Error Mapping

Internal provider result/error categories:

```text
timeout
rate_limited
unavailable
invalid_response
unexpected_error
```

Public mapping:

```text
timeout -> provider_timeout
HTTP 429 -> provider_rate_limited
HTTP 500-599 -> provider_unavailable
invalid JSON/shape -> provider_error
other network/provider exception -> provider_error
```

Do not return provider status text or raw body.

A provider `404` is not expected for search and should not be treated as a valid empty search result automatically.

A valid `200` response with no usable normalized results maps to:

```text
not_found
```

## 11. USDA Response Parsing

Treat USDA response payload as unknown at the provider boundary.

Add narrow runtime guards.

Minimum expected search response:

```text
foods: array
```

Minimum usable food item:

```text
fdcId
description
foodNutrients
```

Skip malformed individual results when possible.

Reject the entire provider response only if the top-level shape is unusable.

Limit the normalized result count after filtering malformed entries.

## 12. Nutrient Normalization

Create a pure normalizer.

Input:

```text
unknown USDA food item
```

Output:

```text
GenericFoodResult | null
```

The normalizer must not access:

* D1
* environment variables
* fetch
* runtime config
* usage counters

Nutrient targets:

```text
Energy kcal
Protein g
Carbohydrate by difference g
Total lipid/fat g
```

Prefer stable identifiers.

Suggested internal matching priority:

1. stable nutrient number or provider nutrient ID
2. exact normalized nutrient name fallback
3. no value

Energy rules:

* Prefer kcal directly.
* If only kJ is available and conversion is implemented:

    * centralize conversion
    * test it
    * round predictably
* Do not use kJ directly as kcal.

Value rules:

* Preserve valid zero values.
* Reject NaN and non-finite numbers.
* Return `null` for unavailable values.
* Do not turn missing values into zero.
* Do not infer nutrients from unrelated nutrient fields.

Name rules:

```text
name = provider description
description = provider description
```

A later phase may refine display names.

## 13. Per-100-g Contract

The public DTO explicitly means:

```text
nutritionPer100g
```

Only include USDA results whose nutrient basis can reasonably be treated as per 100 g under the selected USDA search response contract.

Do not:

* convert serving data without a reliable weight
* assume one serving equals 100 g
* combine portion and per-100-g values
* use branded household serving values in this generic endpoint

Document any USDA data-type assumptions in code comments and README.

## 14. Attribution

Required public values:

```ts
source: "USDA"
attribution: "USDA FoodData Central"
isApproximate: true
```

Do not imply that USDA endorses GymLedger.

Do not omit attribution from cached responses.

The cached normalized JSON must retain attribution.

## 15. Cache Integration

Extend or use the existing cache helpers.

Cache key:

```text
usda:generic:<normalized-query>
```

Lookup type:

```text
generic
```

Source:

```text
usda
```

Recommended TTL:

```text
86400 seconds
```

Use runtime `cache_ttl_seconds` when valid.

Fallback safely to 86400.

Cache write data:

```text
cache_key
source
lookup_type
query
normalized_json
attribution
is_approximate
expires_at
```

The stored `normalized_json` must be exactly the Worker-owned normalized response or a versioned internal DTO.

Do not store the provider response.

Cache read must:

* parse normalized JSON safely
* validate required DTO fields
* reject malformed cached JSON
* reject expired entries
* not crash the Worker

A malformed cache entry should behave as a cache miss.

## 16. Usage and Budget Integration

Use UTC date:

```text
YYYY-MM-DD
```

Required sequence on cache miss:

1. increment `cache_misses`
2. read runtime config
3. block if disabled
4. check daily budget
5. block if exhausted
6. validate API key
7. increment `external_calls` immediately before actual fetch
8. call USDA

Blocked calls:

* safe mode: increment `blocked_calls`
* online lookup disabled: increment `blocked_calls`
* provider disabled: increment `blocked_calls`
* budget exceeded: increment `blocked_calls`

Missing API key is configuration failure.

It may increment `blocked_calls` if the project treats configuration blocks consistently, but this behavior must be selected explicitly and tested.

Do not increment:

```text
external_calls
```

when:

* cache hit
* safe mode blocks
* online lookup disabled
* provider disabled
* budget exceeded
* API key is missing before fetch
* request validation fails

## 17. Runtime Configuration

Required runtime checks:

```text
safe_mode
online_lookup_enabled
usda_provider_enabled
daily_external_call_budget
cache_enabled
cache_ttl_seconds
```

Current conservative defaults remain:

```text
safe_mode = true
online_lookup_enabled = false
usda_provider_enabled = false
daily_external_call_budget = 25
cache_enabled = true
cache_ttl_seconds = 86400
```

Testing provider success requires mocked runtime config values or an injected configuration layer.

Do not change defaults globally to enabled.

## 18. Route Orchestration

Keep `index.ts` small.

Suggested route logic:

```ts
if (pathname === "/v1/foods/generic") {
  if (method !== "GET") {
    return error("method_not_allowed");
  }

  return handleGenericFoodLookup(request, env);
}
```

Put the main use-case logic in:

```text
services/genericFoodLookup.ts
```

The service should return a typed result or throw/return known domain errors.

Avoid deeply nested logic in the route.

## 19. Response Envelope

Use the existing Worker response helpers.

Success:

```json
{
  "ok": true,
  "data": {
    "query": "egg",
    "source": "USDA",
    "attribution": "USDA FoodData Central",
    "isApproximate": true,
    "results": []
  }
}
```

Error:

```json
{
  "ok": false,
  "error": {
    "code": "provider_timeout",
    "message": "Food lookup is temporarily unavailable."
  }
}
```

Messages must remain safe and generic.

Do not include internal details.

## 20. Error Codes

Extend the existing error catalog with:

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

Use consistent HTTP statuses.

Suggested mapping:

```text
invalid_query -> 400
lookup_disabled -> 503
provider_disabled -> 503
budget_exceeded -> 429
provider_timeout -> 504
provider_rate_limited -> 429
provider_unavailable -> 503
provider_error -> 502
not_found -> 404
configuration_error -> 503
```

Do not expose whether a secret exists beyond the generic configuration error.

## 21. Tests

All tests must run without live USDA access.

### Query tests

* missing `q`
* empty `q`
* whitespace-only `q`
* query below `minQueryLength`
* valid normalized query
* repeated internal whitespace

### Cache tests

* valid cache hit returns response
* cache hit avoids provider fetch
* cache hit increments hit count
* cache hit increments daily cache hit
* expired entry acts as miss
* malformed JSON acts as miss
* cache disabled skips cache read/write if that is the selected contract
* normalized provider response is cached
* raw provider payload is never passed to cache write

### Runtime gate tests

* safe mode blocks
* online lookup disabled blocks
* USDA provider disabled blocks
* blocked gate increments `blocked_calls`
* blocked gate does not increment `external_calls`
* budget exhausted blocks
* budget exhausted returns `budget_exceeded`

### Configuration tests

* missing `USDA_API_KEY` returns `configuration_error`
* missing key does not call fetch
* missing key does not increment external calls

### Provider client tests

* request URL uses USDA endpoint
* API key is included only in provider request
* POST body is controlled
* fixed page size is used
* allowed generic data types are used
* timeout abort maps correctly
* 429 maps correctly
* 5xx maps correctly
* invalid JSON maps correctly
* invalid top-level shape maps correctly

### Normalizer tests

* valid Foundation food
* valid SR Legacy food
* valid FNDDS/Survey food if supported
* calories/protein/carbohydrate/fat mapping
* missing nutrient returns `null`
* zero nutrient remains zero
* invalid number returns `null`
* malformed food is skipped
* no raw nutrient arrays in output
* attribution and approximate flags exist

### Route tests

* GET valid query success
* POST returns method_not_allowed
* invalid query returns invalid_query
* cache hit success
* provider success
* provider timeout
* provider rate limit
* provider unavailable
* no usable results returns not_found
* `/v1/health` remains unchanged
* `/v1/config` remains unchanged

## 22. Testing Architecture

Prefer dependency injection for:

```text
fetch
clock/date
timeout
runtime config
provider client
```

Do not require a full dependency-injection framework.

Simple function parameters or small interfaces are enough.

Avoid global mutable mocks.

Avoid tests that depend on execution order.

## 23. README Updates

Document:

```text
USDA_API_KEY requirement
data.gov key responsibility
local .dev.vars setup
Cloudflare secret setup
safe-mode defaults
runtime flags needed for provider calls
generic endpoint
example curl
cache-first behavior
daily budget behavior
provider timeout behavior
USDA attribution
no raw payload storage
no Android integration yet
```

Example local file:

```text
worker/food-lookup/.dev.vars
```

Example content:

```text
USDA_API_KEY=your-local-key
```

Explicitly say:

```text
Do not commit .dev.vars.
```

Do not put a real key in README.

## 24. Manual QA Sequence

1. Apply local D1 migration.
2. Set local USDA API key.
3. Start Worker.
4. Confirm `/v1/health`.
5. Confirm `/v1/config`.
6. Confirm provider call is blocked under conservative defaults.
7. Enable required runtime flags locally.
8. Call generic endpoint with `egg`.
9. Inspect normalized DTO.
10. Repeat same query.
11. Confirm second request uses cache.
12. Inspect D1 usage counters.
13. Disable USDA provider.
14. Confirm provider-disabled error.
15. Test invalid query.
16. Do not deploy.

## 25. Quality Gates

From repo root:

```bash
git status --short --untracked-files=all
git diff --name-status
git diff --stat
git diff --check
```

Android no-touch:

```bash
git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

Worker validation:

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

Open Food Facts no-scope gate:

```bash
grep -R -nE "openfoodfacts|world\.openfoodfacts\.org" src || true
```

Barcode no-scope gate:

```bash
grep -R -nE "barcode|foods/barcode" src || true
```

Raw provider payload gate:

```bash
grep -R -nE "rawPayload|raw_json|providerPayload|usdaResponse" src || true
```

Secret no-commit gate:

```bash
git grep -nE "USDA_API_KEY\s*=|api_key=[A-Za-z0-9_-]{10,}" -- \
  ':!worker/food-lookup/README.md' \
  ':!docs/CURRENT_PHASE.md' \
  ':!docs/IMPLEMENTATION_PLAN.md' || true
```

## 26. Explicitly Out of Scope

```text
Open Food Facts
barcode scanning
packaged-food search
Android networking
Android DTOs
Retrofit
OkHttp
Android settings UI
remote Worker deployment
custom domain
user accounts
personal cloud sync
provider result persistence in Room
food selection UI
meal logging integration
```

## 27. Stop Conditions

Stop before editing or continuing when:

* the existing Phase 17E.1 code differs materially from this plan
* current cache helpers cannot support expiration safely
* the USDA search response cannot reliably support per-100-g values
* required nutrient identifiers are ambiguous
* live network access would be required for unit tests
* the implementation requires Android changes
* the implementation requires an additional D1 migration
* a remote migration or deploy is proposed
* a real API key is found in tracked files
* the implementation expands to Open Food Facts or barcode lookup
* validation fails twice after focused corrections

## 28. Final Builder Report

The builder must stop after implementation and report:

1. Files created.
2. Files modified.
3. Query and cache flow implemented.
4. Runtime and budget gates implemented.
5. USDA request strategy.
6. Nutrient identifiers used.
7. DTO shape.
8. Error mapping.
9. Test count and results.
10. Typecheck result.
11. D1 local migration result.
12. Manual QA performed or not performed.
13. Any behavior requiring user approval.
14. Confirmation that Android was untouched.
15. Confirmation that no key or raw provider payload was committed.
