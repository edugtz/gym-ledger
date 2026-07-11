# Phase 17E.3 Implementation Plan — Open Food Facts Barcode Lookup Provider

## 1. Goal

Implement packaged-food barcode lookup through Open Food Facts.

Required endpoint:

```text
GET /v1/foods/barcode/:barcode
```

The endpoint will:

1. parse and validate the barcode path segment
2. build a stable cache key
3. check D1 cache
4. return a valid cache hit immediately
5. evaluate runtime, feature, and budget gates on cache miss
6. validate Open Food Facts provider configuration
7. call Open Food Facts once with an explicit timeout
8. normalize the product into a Worker-owned DTO
9. cache only the normalized DTO
10. return stable errors without exposing provider internals

No Android work is included.

No full-text product search is included.

No provider writes are included.

## 2. Source of Truth

Read before editing:

```text
AGENTS.md
AI_WORKFLOW.md
docs/CURRENT_PHASE.md
docs/IMPLEMENTATION_PLAN.md
docs/BACKEND_PHASE_GUIDE.md
worker/food-lookup/README.md
```

Inspect all existing Worker source and tests before choosing exact files.

Phase 17E.1 and 17E.2 foundations must be reused.

## 3. Existing Architecture

Current Worker patterns include:

```text
src/index.ts
src/errors.ts
src/response.ts
src/config.ts
src/db.ts
src/auth.ts
src/cache.ts
src/usage.ts
src/runtimeConfig.ts
src/types/foodLookup.ts
src/providers/usda.ts
src/normalizers/usdaFood.ts
src/services/genericFoodLookup.ts
```

Existing behavior that must remain stable:

```text
GET /v1/health
GET /v1/config
GET /v1/foods/generic?q=<query>
```

Do not regress USDA behavior.

## 4. Proposed Structure

Likely files to create:

```text
src/barcode.ts
src/barcode.test.ts
src/types/packagedFoodLookup.ts
src/types/packagedFoodLookup.test.ts
src/providers/openFoodFacts.ts
src/providers/openFoodFacts.test.ts
src/normalizers/openFoodFactsProduct.ts
src/normalizers/openFoodFactsProduct.test.ts
src/services/barcodeFoodLookup.ts
src/services/barcodeFoodLookup.test.ts
```

Likely files to modify:

```text
src/index.ts
src/index.test.ts
src/errors.ts
src/runtimeConfig.ts
src/runtimeConfig.test.ts
README.md
```

Possible reuse-only files:

```text
src/cache.ts
src/cache.test.ts
src/usage.ts
src/usage.test.ts
```

Do not create all proposed files blindly.

Preflight must confirm the smallest coherent structure.

## 5. Request Flow

Required flow:

```text
incoming request
  -> match /v1/foods/barcode/:barcode
  -> reject unsupported method
  -> decode path segment
  -> validate barcode
  -> build cache key
  -> read cache when enabled
  -> valid cache hit?
       yes:
         increment cache entry hit
         increment daily cache_hits
         return cached DTO
       no:
         increment daily cache_misses
         read runtime configuration
         safe mode enabled?
           block
         online lookup disabled?
           block
         OFF provider disabled?
           block
         barcode lookup disabled?
           block
         daily budget exhausted?
           block
         missing User-Agent?
           configuration error
         increment external_calls
         call OFF once with timeout
         validate provider response
         normalize product
         cache normalized DTO
         return response
```

Validation failures occur before cache and usage counters.

## 6. Barcode Utility

Create a pure barcode utility.

Suggested interface:

```ts
export interface BarcodeValidationSuccess {
  ok: true;
  barcode: string;
}

export interface BarcodeValidationFailure {
  ok: false;
}

export function normalizeAndValidateBarcode(
  raw: string
): BarcodeValidationSuccess | BarcodeValidationFailure;
```

Required behavior:

```text
trim
preserve leading zeroes
digits only
allowed lengths: 8, 12, 13, 14
return string
```

Do not use:

```ts
Number(barcode)
parseInt(barcode)
BigInt(barcode)
```

The DTO, cache key, route, and provider URL all use a string.

Do not silently pad or truncate.

Do not calculate or reject by checksum unless explicitly approved after discovery.

## 7. Environment Contract

Extend the Worker environment:

```ts
OPEN_FOOD_FACTS_USER_AGENT?: string;
```

No Open Food Facts API key is needed for product reads.

Rules:

* missing User-Agent returns `configuration_error` only after cache miss and provider gates pass
* do not add personal contact information to source
* do not place a real contact email in README examples
* use `.dev.vars` locally
* do not commit `.dev.vars`
* future deployment may use `wrangler secret put` or approved environment configuration

Existing environment values remain intact:

```ts
DB: D1Database;
GYMLEDGER_API_KEY?: string;
USDA_API_KEY?: string;
```

## 8. Provider Client

Create a dedicated Open Food Facts provider client.

Suggested types:

```ts
export interface OpenFoodFactsProviderConfig {
  baseUrl: string;
  userAgent: string;
  timeoutMs: number;
}

export type OpenFoodFactsProviderResult =
  | { kind: "success"; payload: unknown }
  | { kind: "not_found" }
  | { kind: "timeout" }
  | { kind: "rate_limited" }
  | { kind: "unavailable" }
  | { kind: "error" }
  | { kind: "unexpected"; detail: string };
```

Prefer returning validated minimal provider data or an unknown payload passed directly to a pure normalizer.

The provider must not:

* build the public response envelope
* write to D1
* update counters
* log payloads
* retry repeatedly
* download images
* perform search requests

## 9. Provider URL

Production base:

```text
https://world.openfoodfacts.org
```

Current endpoint:

```text
/api/v3.6/product/<barcode>.json
```

Build the URL with `URL`.

Encode the barcode path safely.

Because validated barcodes are digits only, path encoding should be straightforward, but still avoid manual untrusted URL concatenation where practical.

Potential field selection may include only values needed for:

```text
code
product_name
generic_name
brands
brands_tags
quantity
serving_size
nutriments
```

The exact current v3 field-selection syntax must be verified during preflight.

Do not assume v2 query parameters work unchanged in v3.

If reliable v3 field selection is uncertain, fetch the single product response without broadening to additional requests, then normalize and discard unused fields.

## 10. Required Headers

Provider request must include:

```http
User-Agent: <configured value>
Accept: application/json
```

No authentication header is required for read-only product lookup.

Do not include:

```text
Authorization
Api-Key
cookies
user credentials
```

Tests must verify:

* custom User-Agent is present
* Accept header is present
* no write credentials exist
* request method is GET
* body is absent

## 11. Timeout

Use `AbortController`.

Default:

```text
5000 ms
```

Make timeout injectable through provider config.

Clear timeout after both success and failure.

Map only an abort caused by timeout to:

```text
timeout
```

Other fetch failures map to:

```text
error
```

## 12. HTTP Mapping

Suggested provider mapping:

```text
200 -> parse and validate response
404 -> not_found
429 -> rate_limited
500–599 -> unavailable
other non-2xx -> error
invalid JSON -> unexpected
invalid top-level shape -> unexpected
```

Open Food Facts may return a successful HTTP response containing a status field indicating product absence.

The implementation must inspect current response semantics.

Required distinction:

```text
valid provider not-found response -> not_found
invalid provider response -> unexpected/provider_error
```

Do not treat malformed payload as an unknown barcode.

## 13. Provider Shape Validation

Treat response JSON as `unknown`.

Validate at minimum:

* top-level non-null object
* not an array
* recognized status/result semantics
* product field is an object for success
* product code/barcode is compatible with the request where supplied

A valid not-found payload may omit `product`.

A successful payload without a usable product object is `unexpected`, not `not_found`, unless current official semantics explicitly identify it as not found.

Provider detail strings must not be returned publicly.

## 14. Worker DTO

Create Worker-owned types.

Suggested contract:

```ts
export interface PackagedFoodLookupResponse {
  barcode: string;
  source: "OPEN_FOOD_FACTS";
  attribution: "Open Food Facts — ODbL";
  isApproximate: true;
  product: PackagedFoodProduct;
}

export interface PackagedFoodProduct {
  externalId: string;
  name: string | null;
  genericName: string | null;
  brands: string[];
  quantity: string | null;
  servingSize: string | null;
  nutritionPer100g: NutritionValues;
  nutritionPerServing: NutritionValues;
}

export interface NutritionValues {
  caloriesKcal: number | null;
  proteinG: number | null;
  carbohydrateG: number | null;
  fatG: number | null;
}
```

Reuse `NutritionPer100g` from the USDA DTO only if it remains semantically clean.

Do not couple packaged-food DTO naming to USDA-specific types if that makes future Android contract confusing.

## 15. Cached DTO Guard

Add:

```ts
isPackagedFoodLookupResponse(
  value: unknown
): value is PackagedFoodLookupResponse
```

Validate:

* root object, not array
* barcode string matching supported barcode format
* source exactly `OPEN_FOOD_FACTS`
* attribution non-empty string
* `isApproximate === true`
* product object
* `externalId` string
* nullable name/genericName/quantity/servingSize
* brands array of strings
* nutrition objects
* every nutrient is finite number or null

Reject:

* NaN
* Infinity
* missing required keys
* wrong source
* malformed brands
* malformed nutrition
* arrays as root/product/nutrition

Cached invalid data behaves as a cache miss.

## 16. Product Normalizer

Create a pure normalizer.

Input:

```text
unknown Open Food Facts product
```

Output:

```text
PackagedFoodProduct | null
```

No access to:

* environment
* fetch
* D1
* counters
* runtime config
* response helpers

Normalize only approved fields.

## 17. Product Identity

Required:

```text
externalId = normalized barcode string
```

Prefer provider `code` only when it matches or normalizes consistently with the requested barcode.

Do not replace the requested barcode with a numeric form.

If provider code conflicts materially with requested barcode:

* stop and classify as unexpected provider response
* do not silently return a different product

## 18. Name Normalization

Suggested provider field priority:

```text
localized product name
product_name
generic_name
null
```

Exact localization field behavior must be discovered.

Avoid broad language logic in this phase.

Do not:

* invent a name
* use barcode as name
* concatenate brand into name automatically
* expose an empty string as a meaningful name

Trim strings.

Convert empty strings to `null`.

## 19. Brand Normalization

Output:

```ts
string[]
```

Priority:

1. readable structured provider values
2. provider brand text fallback

Rules:

* trim
* remove empties
* deduplicate
* preserve readable casing
* avoid taxonomy prefixes
* do not split on ambiguous punctuation beyond comma unless documented

## 20. Quantity and Serving Size

Normalize strings:

```text
quantity
serving_size
```

Rules:

* trim
* empty becomes null
* no unit conversion in this phase
* no package-size parsing
* no serving-weight derivation from arbitrary text

## 21. Nutrition Normalization

Per 100 g:

```text
energy-kcal_100g
proteins_100g
carbohydrates_100g
fat_100g
```

Per serving:

```text
energy-kcal_serving
proteins_serving
carbohydrates_serving
fat_serving
```

Output names:

```text
caloriesKcal
proteinG
carbohydrateG
fatG
```

Value rules:

* finite number -> preserve
* zero -> preserve
* numeric string -> only parse if official provider schema/current payload requires it and tests cover it
* missing -> null
* NaN/infinite -> null
* negative nutrients -> preferably null unless the provider contract identifies a valid reason
* do not invent missing values

Energy fallback:

```text
energy-kcal_* preferred
```

Do not use `energy_*` kJ as kcal.

A kJ-to-kcal conversion is optional only when:

* kcal is absent
* unit is proven to be kJ
* conversion is centralized
* conversion is tested

## 22. Attribution

Required response values:

```ts
source: "OPEN_FOOD_FACTS"
attribution: "Open Food Facts — ODbL"
isApproximate: true
```

Cached data must retain attribution.

Do not imply Open Food Facts endorsement.

README must document:

* community-maintained data
* possible incompleteness
* Open Database License attribution
* user review/editability in future Android phase

## 23. Service Orchestration

Create:

```text
src/services/barcodeFoodLookup.ts
```

Suggested inputs:

```ts
export interface BarcodeFoodLookupDeps {
  env: WorkerEnv;
  barcode: string;
  today: string;
}
```

Prefer dependency injection or small internal interfaces for:

* provider client
* clock where necessary
* timeout configuration

Do not introduce a DI framework.

## 24. Cache Integration

Cache key:

```text
open_food_facts:barcode:<barcode>
```

Store:

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

Recommended TTL:

```text
runtime cache_ttl_seconds
fallback 86400
```

Store exactly the normalized response.

Do not store:

* provider payload
* headers
* User-Agent
* raw nutriments
* image metadata
* ingredients
* debug status

## 25. Runtime Sequence

On valid cache miss:

1. increment `cache_misses`
2. read `safe_mode`
3. read `online_lookup_enabled`
4. read `open_food_facts_provider_enabled`
5. read `barcode_lookup_enabled`
6. read daily budget
7. validate `OPEN_FOOD_FACTS_USER_AGENT`
8. increment `external_calls`
9. call provider once

Blocked gates increment `blocked_calls`.

Missing User-Agent should also increment `blocked_calls` if configuration failures are treated consistently with the USDA missing-key implementation.

This behavior must be documented and tested.

## 26. Errors

Extend the catalog only where necessary.

Add:

```text
invalid_barcode
feature_disabled
```

Preserve existing:

```text
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

Mapping:

```text
invalid_barcode -> 400
feature_disabled -> 503
```

Do not create Open Food Facts-specific public error codes.

Provider identity is an internal implementation detail.

## 27. Routing

Keep `index.ts` small.

Suggested path matching:

```ts
const barcodePrefix = "/v1/foods/barcode/";

if (pathname.startsWith(barcodePrefix)) {
  if (method !== "GET") {
    return error("method_not_allowed");
  }

  const rawBarcode = pathname.slice(barcodePrefix.length);
  // Validate exactly one segment.
}
```

Required route behavior:

* `/v1/foods/barcode/3017620422003` -> lookup
* `/v1/foods/barcode/` -> invalid barcode or route handling selected consistently
* `/v1/foods/barcode/123/extra` -> reject
* encoded slash/path traversal -> reject
* POST -> method_not_allowed
* unrelated paths remain not_found

Do not use loose matching that accepts nested paths.

## 28. Tests

All unit tests run without live Open Food Facts access.

### Barcode tests

* valid 8 digits
* valid 12 digits
* valid 13 digits
* valid 14 digits
* leading zeroes preserved
* empty
* whitespace-only
* letters
* punctuation
* too short
* unsupported length
* embedded spaces
* very long input

### Provider transport tests

* correct base URL
* correct API version and product path
* barcode encoded safely
* GET
* no request body
* custom User-Agent header
* Accept JSON header
* timeout signal present
* no credentials or API key headers
* only one request

### Provider response tests

* successful product
* official valid not-found payload
* HTTP 404 if applicable
* 429
* 503
* other 5xx
* other non-success
* invalid JSON
* top-level null
* top-level array
* missing status/result fields
* success without product
* malformed product

### Normalizer tests

* complete product
* missing product name
* generic-name fallback
* brands normalization
* duplicate brands
* quantity
* serving size
* four nutrients per 100 g
* four nutrients per serving
* missing values
* valid zero values
* invalid strings
* NaN
* Infinity
* no kJ-as-kcal
* barcode remains string
* leading zeroes preserved

### DTO guard tests

* valid DTO
* empty object
* root array
* wrong source
* false approximate flag
* numeric barcode
* malformed product
* malformed brands
* missing nutrition fields
* invalid nutrient type
* NaN
* Infinity

### Service tests

* valid cache hit
* cache hit avoids provider
* cache hit counters
* expired cache miss
* malformed cache miss
* safe mode block
* online lookup block
* provider block
* feature block
* budget block
* blocked counters
* missing User-Agent
* successful provider call
* external call counter
* normalized response cached
* raw payload not cached
* not found
* rate limited
* timeout
* unavailable
* invalid provider response

### Route tests

* valid barcode GET
* invalid barcode
* missing barcode
* unsupported method
* nested trailing path rejected
* existing USDA route unchanged
* health unchanged
* config unchanged
* unknown route unchanged

## 29. Test Architecture

Use dependency injection for provider `fetch`.

Avoid:

* live provider calls
* global network mocks
* tests depending on current Open Food Facts product availability
* real email/User-Agent values
* real credentials
* execution-order dependencies

Use fixture objects containing only minimal provider fields.

## 30. README Updates

Document:

```text
barcode endpoint
supported barcode lengths
Open Food Facts API v3 product read
custom User-Agent requirement
.dev.vars setup
runtime flags
local D1 enable commands
cache-first behavior
daily budget
rate-limit-friendly design
timeout behavior
attribution and ODbL
community data caveat
known barcode curl
invalid barcode curl
restoring conservative defaults
no text search
no Android integration yet
```

Example:

```text
OPEN_FOOD_FACTS_USER_AGENT=GymLedger/0.1 (contact@example.invalid)
```

Use a clearly non-real example domain.

Do not commit a real address.

## 31. Manual QA

Use one known barcode and one intentionally unknown valid-format barcode.

Required checks:

* conservative defaults block external lookup
* valid enabled lookup returns normalized response
* repeated barcode uses cache
* unknown barcode returns not_found
* invalid barcode returns invalid_barcode
* provider feature flag blocks
* no raw OFF fields
* no User-Agent/contact leak
* D1 counters behave correctly

Manual QA is optional before code review if it would require provider access not already configured.

It becomes mandatory before Phase 17E.4 deployment completion.

## 32. Quality Gates

From repo root:

```bash
git status --short --untracked-files=all
git diff --name-status
git diff --stat
git diff --check
git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

Worker:

```bash
cd worker/food-lookup
npm run typecheck
npm test
```

D1:

```bash
npx wrangler d1 migrations list gymledger-food-lookup --local
npx wrangler d1 migrations apply gymledger-food-lookup --local
```

No search:

```bash
grep -R -nE "cgi/search\.pl|/search\?|search-as-you-type" src || true
```

No write/auth credentials:

```bash
grep -R -nE "user_id|password|Authorization|session cookie" src || true
```

No raw storage:

```bash
grep -R -nE "rawPayload|raw_json|providerPayload|offResponse|openFoodFactsResponse" src || true
```

No images:

```bash
grep -R -nE "image_url|selected_images|images\.openfoodfacts" src || true
```

No Android:

```bash
git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

## 33. Explicitly Out of Scope

```text
Android networking
Android barcode scanner
camera integration
ML Kit
ZXing
camera permission
food selection UI
meal logging integration
product text search
search suggestions
search-as-you-type
Open Food Facts writes
product contribution
image upload
image download
ingredients
allergens
additives
Nutri-Score
NOVA
Green-Score
Eco-Score
cloud personal data
remote deployment
custom domain
```

## 34. Stop Conditions

Stop before editing or continuing if:

* current Open Food Facts v3 product endpoint differs materially
* User-Agent requirements cannot be satisfied without committing personal data
* official not-found semantics remain ambiguous after inspection
* the implementation requires provider search
* the implementation requires multiple provider requests
* the implementation requires a schema migration
* Android changes are proposed
* tests require live internet
* product image support enters scope
* write authentication enters scope
* validation fails twice after focused fixes

## 35. Final Builder Report

The builder must stop after implementation and report:

1. Files created.
2. Files modified.
3. Barcode validation contract.
4. Provider endpoint and API version used.
5. User-Agent handling.
6. Provider not-found semantics.
7. DTO shape.
8. Nutrition fields normalized.
9. Cache behavior.
10. Runtime and budget gates.
11. Error mapping.
12. Test count and results.
13. Typecheck result.
14. D1 local result.
15. Manual QA performed or not performed.
16. Any provider behavior requiring approval.
17. Confirmation Android was untouched.
18. Confirmation no personal contact, credentials, or raw payload was committed.
