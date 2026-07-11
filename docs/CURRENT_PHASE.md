# Phase 17E.3 — Open Food Facts Barcode Lookup Provider

## Objective

Add packaged-food barcode lookup through Open Food Facts to the GymLedger Food Lookup Worker.

This phase builds on:

* Phase 17E.1 — D1 Cache and Budget Foundation
* Phase 17E.2 — USDA Generic Food Lookup Provider

The Worker must:

* accept a barcode through a dedicated endpoint
* normalize and validate the barcode
* query Open Food Facts using its current product API
* normalize the packaged-food result into a stable GymLedger DTO
* return nutrition per 100 g and serving information when reliably available
* include source attribution
* mark externally sourced nutrition as approximate
* use D1 cache before calling Open Food Facts
* enforce safe mode, online lookup, provider, feature, and daily-budget gates
* use a custom Open Food Facts User-Agent
* handle provider failures with stable public errors
* keep raw Open Food Facts payloads private
* keep Android unchanged

This phase does not implement generic product text search.

This phase does not implement search-as-you-type.

This phase does not modify Android.

This phase does not deploy the Worker.

## Product Quality Goal

GymLedger should be able to resolve a known packaged-food barcode while remaining:

* offline-first for saved food data
* conservative with external calls
* resilient to incomplete community data
* explicit about attribution
* safe when providers are unavailable
* independent from the provider’s raw schema

Cloud helps discover data.

Room owns saved data.

Open Food Facts data is community-maintained and may be incomplete or inaccurate.

The Worker must normalize it but must not present it as guaranteed truth.

## Current Phase

```text
Phase 17E.3
Backend Phase B4
Open Food Facts Barcode Lookup Provider
```

## Recommended AI Route

* Planning: ChatGPT
* Optional planning review: Qwen3.6 35B A3B local
* Preferred builder: OpenCode Go Qwen3.7 Plus
* Local builder alternative: Qwen Coder 30B A3B 5bit
* Focused local patches: Devstral Small 2 24B 6bit
* Heavy local review: Qwen3.6 27B 8bit
* Debug escalation: OpenCode Go DeepSeek V4 Pro
* Provider/API edge-case escalation: Codex
* Gemini: not needed because this phase does not touch Android

## Scope

Implement only Open Food Facts barcode lookup.

Required endpoint:

```text
GET /v1/foods/barcode/:barcode
```

Example:

```text
GET /v1/foods/barcode/3017620422003
```

Use Open Food Facts product read API.

Recommended current provider endpoint:

```text
GET https://world.openfoodfacts.org/api/v3.6/product/<barcode>.json
```

Request only the fields required for the GymLedger DTO when supported by the provider.

Do not implement Open Food Facts full-text search.

Do not implement structured product search.

Do not use legacy `/cgi/search.pl`.

## Existing Foundation to Reuse

Reuse the current Worker architecture:

```text
worker/food-lookup/src/index.ts
worker/food-lookup/src/errors.ts
worker/food-lookup/src/response.ts
worker/food-lookup/src/cache.ts
worker/food-lookup/src/usage.ts
worker/food-lookup/src/runtimeConfig.ts
worker/food-lookup/src/types/foodLookup.ts
worker/food-lookup/src/services/genericFoodLookup.ts
worker/food-lookup/src/providers/usda.ts
```

Reuse:

* D1 cache
* cache expiration
* safe normalized JSON parsing
* runtime configuration
* daily usage counters
* external call budget
* stable response envelope
* stable provider error patterns
* query-independent UTC budget tracking
* dependency-injected provider tests

Do not create duplicate cache, usage, config, or error systems.

## Runtime Gates

Before making an Open Food Facts request, enforce:

1. `safe_mode`
2. `online_lookup_enabled`
3. `open_food_facts_provider_enabled`
4. `barcode_lookup_enabled`
5. `daily_external_call_budget`

Required behavior:

* If `safe_mode = true`, return `lookup_disabled`.
* If `online_lookup_enabled = false`, return `lookup_disabled`.
* If `open_food_facts_provider_enabled = false`, return `provider_disabled`.
* If `barcode_lookup_enabled = false`, return `feature_disabled`.
* If the daily budget is exhausted, return `budget_exceeded`.
* Runtime and budget blocks increment `blocked_calls`.
* Cache hits do not consume external-call budget.
* `external_calls` increments only immediately before an actual provider request.
* Cache misses increment `cache_misses`.
* Cache hits increment cache entry and daily hit counters.

Conservative defaults remain unchanged:

```text
safe_mode = true
online_lookup_enabled = false
open_food_facts_provider_enabled = false
barcode_lookup_enabled = false
daily_external_call_budget = 25
cache_enabled = true
cache_ttl_seconds = 86400
```

Do not enable provider defaults globally.

## Barcode Rules

The route path contains the barcode:

```text
/v1/foods/barcode/:barcode
```

Required validation:

* trim surrounding whitespace
* decode the URL segment safely
* digits only
* reject empty values
* reject non-numeric values
* accept common GTIN lengths:

    * 8
    * 12
    * 13
    * 14
* preserve leading zeroes
* do not parse the barcode as a number
* store and return it as a string
* do not silently remove internal characters
* do not guess or pad invalid barcodes
* do not require a check-digit implementation unless preflight proves it is needed

Examples:

```text
3017620422003 -> valid
012345678905 -> valid
12345670 -> valid
00012345600012 -> valid
1234 -> invalid
ABC123 -> invalid
```

Barcode normalization must be centralized and tested.

## Cache Behavior

Cache key pattern:

```text
open_food_facts:barcode:<barcode>
```

Recommended values:

```text
source = open_food_facts
lookup_type = barcode
query = normalized barcode
```

Cache lookup happens before provider gates.

A valid, non-expired cache hit must:

* return the normalized DTO
* increment the cache entry hit count
* increment daily `cache_hits`
* avoid an Open Food Facts request
* avoid incrementing `external_calls`

A cache miss must:

* increment daily `cache_misses`
* evaluate runtime/provider/feature/budget gates
* call Open Food Facts only when allowed
* normalize the response
* cache only the normalized Worker-owned DTO

Expired, malformed, or structurally invalid cached data behaves as a cache miss.

Never cache raw Open Food Facts payloads.

## Provider Contract

Use production base URL:

```text
https://world.openfoodfacts.org
```

Current product endpoint:

```text
/api/v3.6/product/<barcode>.json
```

Use `GET`.

No Open Food Facts API key is required for read-only product lookup.

The request must include a custom User-Agent.

Required environment value:

```text
OPEN_FOOD_FACTS_USER_AGENT
```

Recommended local value format:

```text
GymLedger/0.1 (contact@example.com)
```

Rules:

* do not hardcode the user’s personal email
* do not commit a real contact email unless explicitly approved
* local `.dev.vars` may provide the User-Agent
* production deployment will use a Worker secret or environment variable
* missing User-Agent when provider execution is otherwise allowed returns `configuration_error`
* do not expose the User-Agent in public responses
* do not log request headers unnecessarily

The provider request should ask only for required fields when the current API supports field selection.

## Provider Traffic Rules

Open Food Facts product reads are rate-limited.

The implementation must:

* use cache first
* make at most one provider request per incoming lookup
* avoid retries in the same request
* avoid N+1 requests
* avoid search endpoints
* avoid search-as-you-type
* use an explicit timeout
* respect HTTP 429
* treat provider-wide HTTP 503 as unavailable or rate-limited infrastructure
* not download images
* not proxy image bytes
* not prefetch adjacent products
* not crawl products

No live-provider tests may run as part of unit tests.

## Timeout

Use `AbortController`.

Recommended timeout:

```text
5 seconds
```

Keep timeout as a named constant or injectable provider option.

Map timeout aborts to:

```text
provider_timeout
```

Do not map unrelated network failures to timeout.

## Public DTO

Add a stable packaged-food DTO owned by GymLedger.

Suggested response:

```json
{
  "barcode": "3017620422003",
  "source": "OPEN_FOOD_FACTS",
  "attribution": "Open Food Facts — ODbL",
  "isApproximate": true,
  "product": {
    "externalId": "3017620422003",
    "name": "Product name",
    "genericName": null,
    "brands": ["Brand"],
    "quantity": "400 g",
    "servingSize": "30 g",
    "nutritionPer100g": {
      "caloriesKcal": 539,
      "proteinG": 6.3,
      "carbohydrateG": 57.5,
      "fatG": 30.9
    },
    "nutritionPerServing": {
      "caloriesKcal": 162,
      "proteinG": 1.9,
      "carbohydrateG": 17.3,
      "fatG": 9.3
    }
  }
}
```

Required top-level fields:

* `barcode`
* `source`
* `attribution`
* `isApproximate`
* `product`

Required product fields:

* `externalId`
* `name`
* `genericName`
* `brands`
* `quantity`
* `servingSize`
* `nutritionPer100g`
* `nutritionPerServing`

Required nutrition fields:

* `caloriesKcal`
* `proteinG`
* `carbohydrateG`
* `fatG`

Rules:

* `source` must be exactly `OPEN_FOOD_FACTS`.
* `attribution` must identify Open Food Facts and ODbL.
* `isApproximate` must be `true`.
* Missing scalar fields use `null`.
* Missing brands use an empty array.
* Missing nutrition values use `null`.
* Do not invent nutrient values.
* Do not convert missing nutrients to zero.
* Preserve valid numeric zero values.
* Keep barcode values as strings.
* Do not expose raw provider fields.
* Do not expose image URLs in this phase.
* Do not expose ingredients, allergens, Nutri-Score, NOVA, Eco-Score, or additives in this phase unless explicitly approved during preflight.
* Do not expose provider debug metadata.

## Product Name Rules

Choose the product name using a centralized priority.

Suggested priority:

1. localized product name appropriate to provider response
2. `product_name`
3. `generic_name`
4. safe fallback only when a meaningful provider name exists

If no usable product name exists:

* the product may still be returned if barcode and nutrition are usable, but name must not be fabricated
* use an explicit safe fallback only if approved by the implementation plan
* otherwise map unusable product data to `not_found` or `provider_error` according to response semantics

Do not use the barcode itself as the product name.

## Brand Rules

Normalize brands into:

```ts
string[]
```

Preferred sources:

* structured brand tags when available
* comma-separated brand text only as fallback

Required behavior:

* trim values
* remove empty values
* avoid duplicates
* preserve readable provider casing
* do not expose taxonomy prefixes such as `en:` where not user-friendly

## Nutrition Normalization

Normalize per-100-g provider values from the provider’s nutriments structure.

Target fields:

```text
caloriesKcal
proteinG
carbohydrateG
fatG
```

Suggested Open Food Facts field priority:

```text
energy-kcal_100g
proteins_100g
carbohydrates_100g
fat_100g
```

Energy rules:

* prefer explicit kcal value
* do not treat kJ as kcal
* if kJ conversion is implemented:

    * centralize it
    * test it
    * only use it when kcal is absent
* reject non-finite values
* preserve valid zeroes

Per-serving values may use:

```text
energy-kcal_serving
proteins_serving
carbohydrates_serving
fat_serving
```

Only return per-serving values when provided reliably.

Do not derive per-serving values from serving text unless a tested, reliable numeric serving weight is available and such derivation is explicitly implemented.

## Serving Information

Return:

```text
quantity
servingSize
nutritionPerServing
```

Use provider values only.

Do not:

* parse arbitrary household measures
* assume one package equals one serving
* assume serving size equals 100 g
* fabricate serving nutrition
* convert units without a centralized tested implementation

## Provider Response Semantics

Treat the provider payload as `unknown`.

Validate the top-level response shape before normalization.

Distinguish:

```text
valid product found
valid product not found
invalid provider response
provider unavailable
provider rate limited
provider timeout
```

Expected not-found semantics may be represented by:

* provider status indicating no product
* missing product with a valid not-found status
* provider HTTP 404, depending on current endpoint behavior

The builder must inspect the actual current API contract and encode one stable internal mapping.

A malformed response must not be treated as `not_found`.

## Public Errors

Use the existing stable error response envelope.

Required error codes:

```text
invalid_barcode
lookup_disabled
provider_disabled
feature_disabled
budget_exceeded
provider_timeout
provider_rate_limited
provider_unavailable
provider_error
not_found
configuration_error
```

Suggested HTTP mapping:

```text
invalid_barcode -> 400
lookup_disabled -> 503
provider_disabled -> 503
feature_disabled -> 503
budget_exceeded -> 429
provider_timeout -> 504
provider_rate_limited -> 429
provider_unavailable -> 503
provider_error -> 502
not_found -> 404
configuration_error -> 503
```

Do not expose:

* raw provider payloads
* raw provider response bodies
* stack traces
* internal exception messages
* D1 errors
* request headers
* contact details from the User-Agent
* internal provider URLs

## Files Expected

Likely files to create:

```text
worker/food-lookup/src/types/packagedFoodLookup.ts
worker/food-lookup/src/types/packagedFoodLookup.test.ts
worker/food-lookup/src/providers/openFoodFacts.ts
worker/food-lookup/src/providers/openFoodFacts.test.ts
worker/food-lookup/src/normalizers/openFoodFactsProduct.ts
worker/food-lookup/src/normalizers/openFoodFactsProduct.test.ts
worker/food-lookup/src/services/barcodeFoodLookup.ts
worker/food-lookup/src/services/barcodeFoodLookup.test.ts
worker/food-lookup/src/barcode.ts
worker/food-lookup/src/barcode.test.ts
```

Likely files to modify:

```text
worker/food-lookup/src/index.ts
worker/food-lookup/src/index.test.ts
worker/food-lookup/src/errors.ts
worker/food-lookup/src/runtimeConfig.ts
worker/food-lookup/src/runtimeConfig.test.ts
worker/food-lookup/README.md
```

Existing cache and usage files should be reused.

Modify them only when a genuinely reusable improvement is required.

## Do Not Do

* Do not modify Android.
* Do not modify Gradle files.
* Do not add Retrofit.
* Do not add OkHttp.
* Do not add Android barcode scanning.
* Do not add camera permission.
* Do not add ML Kit.
* Do not add product text search.
* Do not add search-as-you-type.
* Do not use `/cgi/search.pl`.
* Do not add Open Food Facts write operations.
* Do not add provider authentication accounts or passwords.
* Do not upload images.
* Do not download or proxy product images.
* Do not expose raw Open Food Facts payloads.
* Do not store raw provider payloads.
* Do not store personal data.
* Do not add user accounts.
* Do not add cloud meal storage.
* Do not deploy.
* Do not apply remote D1 changes.
* Do not add a D1 migration unless a true blocker is discovered and reported.
* Do not change safe defaults to enabled.
* Do not implement Phase 17E.4 or Phase 17F.
* Do not commit; the user commits manually.

## Acceptance Criteria

* `GET /v1/foods/barcode/:barcode` exists.
* Valid GTIN-8, UPC-A, EAN-13, and GTIN-14 strings are accepted.
* Invalid barcode values return `invalid_barcode`.
* Leading zeroes are preserved.
* Cache lookup happens before provider gates.
* Valid cache hit avoids Open Food Facts.
* Cache hit increments cache hit counters.
* Cache miss increments cache miss counters.
* Safe mode blocks external requests.
* Disabled online lookup blocks external requests.
* Disabled Open Food Facts provider blocks external requests.
* Disabled barcode feature blocks external requests.
* Exhausted budget blocks external requests.
* Blocked attempts increment `blocked_calls`.
* Actual provider attempts increment `external_calls`.
* Provider call uses an explicit timeout.
* Provider call includes custom User-Agent.
* Missing User-Agent returns `configuration_error` when provider execution is otherwise allowed.
* Provider HTTP 429 maps to `provider_rate_limited`.
* Provider temporary failure maps to `provider_unavailable`.
* Provider timeout maps to `provider_timeout`.
* Invalid provider responses map to `provider_error`.
* Unknown barcode maps to `not_found`.
* Valid product returns a Worker-owned DTO.
* Barcode remains a string.
* Nutrition is normalized per 100 g.
* Serving nutrition is returned only when reliably available.
* Missing nutrients return `null`.
* Results include Open Food Facts attribution.
* Results are marked approximate.
* Only normalized DTOs are cached.
* No raw provider payload is stored or returned.
* Existing USDA generic lookup remains stable.
* Existing `/v1/health` remains stable.
* Existing `/v1/config` remains stable.
* No Android files are modified.
* No personal data or secrets are committed.
* README documents local provider setup and validation.
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

No text-search scope gate:

```bash
grep -R -nE "cgi/search\.pl|/api/v[0-9.]+/search|search-as-you-type" src || true
```

No write-operation gate:

```bash
grep -R -nE "POST.*openfoodfacts|PUT.*openfoodfacts|user_id|password" src || true
```

Raw provider payload gate:

```bash
grep -R -nE "rawPayload|raw_json|providerPayload|openFoodFactsResponse|offResponse" src || true
```

Android package safety:

```bash
grep -R -n "com\.gymledger" app/src || true
```

## Manual QA

Manual provider QA requires:

* local D1 migrations
* `.dev.vars`
* a valid custom User-Agent
* runtime flags enabled locally
* no deployment

Example `.dev.vars`:

```text
OPEN_FOOD_FACTS_USER_AGENT=GymLedger/0.1 (your-contact-email)
```

Do not commit `.dev.vars`.

Enable local flags:

```bash
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('safe_mode', 'false', datetime('now'));"

npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('online_lookup_enabled', 'true', datetime('now'));"

npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('open_food_facts_provider_enabled', 'true', datetime('now'));"

npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('barcode_lookup_enabled', 'true', datetime('now'));"
```

Start:

```bash
cd worker/food-lookup
npm run dev
```

Test:

```bash
curl -i http://localhost:8787/v1/health
curl -i http://localhost:8787/v1/config
curl -i http://localhost:8787/v1/foods/barcode/3017620422003
curl -i http://localhost:8787/v1/foods/barcode/1234
curl -i http://localhost:8787/v1/foods/barcode/ABC123
curl -i -X POST http://localhost:8787/v1/foods/barcode/3017620422003
```

Repeat the valid barcode to verify cache behavior.

Manual QA must confirm:

* first allowed lookup may call Open Food Facts
* second identical lookup is served from cache
* cache hit does not increment external calls
* barcode remains a string
* leading zeroes are preserved
* attribution is present
* nutrition is normalized
* no raw Open Food Facts fields appear
* no User-Agent value appears in response
* invalid barcode does not consume budget
* disabling feature/provider blocks the external request

Restore safe defaults after QA.

## Stop Conditions

Stop implementation and report before continuing if:

* the current Open Food Facts product API contract differs materially from this plan
* v3 product read cannot provide required nutrition fields
* provider not-found semantics are ambiguous
* barcode normalization requires a product decision
* current cache helpers cannot safely support the DTO
* a D1 schema change appears necessary
* Android changes appear necessary
* a real contact email or credential appears in tracked files
* tests require live network access
* implementation expands into search, write operations, images, or barcode scanning
* remote deploy or migration appears necessary
* the first real validation error cannot be fixed in two focused attempts

## Suggested Commit

```text
feat: add Open Food Facts barcode lookup provider
```
