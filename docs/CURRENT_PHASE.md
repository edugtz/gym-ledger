# Phase 17E.4 — Worker Deploy and Production Smoke Tests

## Objective

Deploy the GymLedger Food Lookup Worker safely to Cloudflare Workers and verify its real production behavior.

This phase closes Phase 17E.

It builds on:

- Phase 17E.1 — D1 Cache and Budget Foundation
- Phase 17E.2 — USDA Generic Food Lookup Provider
- Phase 17E.3 — Open Food Facts Barcode Lookup Provider

The phase must:

- audit the deployment configuration
- confirm production authentication behavior
- configure required Cloudflare secrets
- verify the production D1 binding
- apply pending D1 migrations remotely
- deploy to `workers.dev`
- run production health and configuration smoke tests
- run real USDA generic-food lookup
- run real Open Food Facts barcode lookup
- verify cache behavior
- verify runtime kill switches
- verify daily-budget behavior safely
- verify authentication behavior
- document the deployed endpoint for Phase 17F
- preserve safe defaults when testing is complete

This phase does not modify Android.

This phase does not add new provider features.

This phase does not freeze the final Android integration contract. That belongs to Phase 17F / Backend B6.

## Current Phase

```text
Phase 17E.4
Backend Phase B5
Worker Deploy and Production Smoke Tests
```

## Product Principle

```text
Cloud helps discover data.
Room owns saved data.
```

The deployed Worker remains a low-cost lookup, normalization, cache, and budget service.

It must not store:

- workouts
- meals
- body measurements
- photos
- user accounts
- device identifiers
- personal history
- provider secrets in D1
- API keys in source control

## Recommended AI Route

- Planning: ChatGPT
- Preflight: Qwen Coder 30B A3B 5bit local
- Preferred code-preparation builder: OpenCode Go + Qwen3.7 Plus
- Cloudflare deployment guidance: Codex
- Wrangler/D1 troubleshooting: DeepSeek V4 Pro
- Focused documentation patches: Devstral Small 2 24B 6bit
- Final repository review: ChatGPT through GitHub
- Gemini: not needed because Android is out of scope

Deployment commands and secret entry must remain user-controlled.

No model may invent, print, store, or commit real secrets.

## Scope

Implement and execute only Backend Phase B5:

```text
Worker Deploy and Production Smoke Tests
```

Required production endpoints:

```text
GET /v1/health
GET /v1/config
GET /v1/foods/generic?q=<query>
GET /v1/foods/barcode/:barcode
```

Expected deployment domain:

```text
https://gymledger-food-lookup.<cloudflare-subdomain>.workers.dev
```

The exact URL must come from Wrangler deployment output.

Do not guess the Cloudflare subdomain.

## Existing Deployment Configuration

Current Worker configuration includes:

```text
name = gymledger-food-lookup
main = src/index.ts
workers_dev = true
D1 binding = DB
database_name = gymledger-food-lookup
```

The configured `database_id` must be verified against the intended Cloudflare account before deployment.

Do not replace it blindly.

## Required Pre-Deploy Audit

Before any remote write or deployment, inspect:

```text
worker/food-lookup/wrangler.toml
worker/food-lookup/package.json
worker/food-lookup/src/index.ts
worker/food-lookup/src/auth.ts
worker/food-lookup/src/config.ts
worker/food-lookup/src/runtimeConfig.ts
worker/food-lookup/src/errors.ts
worker/food-lookup/migrations/
worker/food-lookup/README.md
```

Confirm:

- Wrangler is authenticated to the intended Cloudflare account.
- Worker name is correct.
- D1 database exists in that account.
- D1 database ID matches `wrangler.toml`.
- No placeholder database ID remains.
- No secret is stored in `wrangler.toml`.
- `.dev.vars` remains ignored.
- All unit tests pass.
- Both provider implementations are present.
- Runtime defaults remain conservative.
- Existing endpoint contracts remain stable.

## Authentication Contract

The Worker already contains an `X-GymLedger-Key` validation helper.

This phase must make the production authentication contract explicit and tested.

### Public endpoints

```text
GET /v1/health
GET /v1/config
```

These remain publicly accessible and must not reveal secrets.

### Protected endpoints

```text
GET /v1/foods/generic
GET /v1/foods/barcode/:barcode
```

These require:

```http
X-GymLedger-Key: <configured secret>
```

when `GYMLEDGER_API_KEY` is configured.

Production must configure `GYMLEDGER_API_KEY`.

Required behavior:

- missing header returns `unauthorized`
- incorrect header returns `unauthorized`
- correct header allows request processing
- API key must not appear in responses
- API key must not appear in logs
- API key must not be placed in query parameters
- health and config remain public
- provider secrets remain server-side
- unauthorized requests must not invoke provider services
- unauthorized requests must not increment external-call counters

If current route code does not invoke the existing authentication helper, wiring it into lookup routes is required before deployment.

Do not introduce:

- user accounts
- OAuth
- JWT
- sessions
- per-user API keys
- refresh tokens

## Required Secrets and Environment Values

Configure in Cloudflare:

```text
GYMLEDGER_API_KEY
USDA_API_KEY
OPEN_FOOD_FACTS_USER_AGENT
```

Rules:

- `GYMLEDGER_API_KEY` must be a generated high-entropy secret.
- `USDA_API_KEY` must be the real USDA key.
- `OPEN_FOOD_FACTS_USER_AGENT` must identify GymLedger and provide an approved contact address.
- Do not commit any secret value.
- Do not paste secret values into documentation.
- Do not put secret values in `wrangler.toml`.
- Do not store secret values in D1.
- Do not echo secret values in shared terminal transcripts.
- Use interactive Wrangler secret entry.

Commands:

```bash
npx wrangler secret put GYMLEDGER_API_KEY
npx wrangler secret put USDA_API_KEY
npx wrangler secret put OPEN_FOOD_FACTS_USER_AGENT
```

Do not pass secret values directly in shell command arguments.

## Remote D1 Migration

Phase 17E.1 created the required D1 migration.

Before applying remotely:

```bash
npx wrangler d1 migrations list gymledger-food-lookup --remote
```

Review the output.

Only apply migrations when:

- the intended Cloudflare account is active
- the database name is correct
- the database ID is correct
- the migration list is expected
- no unexpected destructive migration appears

Then:

```bash
npx wrangler d1 migrations apply gymledger-food-lookup --remote
```

This is a remote operation and must be executed manually by the user.

Afterward verify that these tables exist:

```text
food_lookup_cache
usage_daily
runtime_config
```

Do not print secrets or unnecessary table contents.

## Production Runtime Configuration

Production must begin and end in a conservative state:

```text
safe_mode = true
online_lookup_enabled = false
usda_provider_enabled = false
open_food_facts_provider_enabled = false
generic_food_search_enabled = false
barcode_lookup_enabled = false
daily_external_call_budget = 25
cache_enabled = true
cache_ttl_seconds = 86400
```

For smoke testing only, temporarily enable:

```text
safe_mode = false
online_lookup_enabled = true
usda_provider_enabled = true
open_food_facts_provider_enabled = true
generic_food_search_enabled = true
barcode_lookup_enabled = true
daily_external_call_budget = 25
cache_enabled = true
cache_ttl_seconds = 86400
```

After smoke testing, restore:

```text
safe_mode = true
online_lookup_enabled = false
usda_provider_enabled = false
open_food_facts_provider_enabled = false
generic_food_search_enabled = false
barcode_lookup_enabled = false
daily_external_call_budget = 25
cache_enabled = true
cache_ttl_seconds = 86400
```

Do not leave providers enabled unintentionally.

## Deployment Preparation

Run from `worker/food-lookup`:

```bash
npm ci
npm run typecheck
npm test
npx wrangler deploy --dry-run
```

Only continue when all commands pass.

Then deploy:

```bash
npx wrangler deploy
```

Record:

- Worker name
- deployment identifier or version
- deployment UTC timestamp
- deployed Git commit SHA
- resulting `workers.dev` URL
- Wrangler version

Do not record secrets or Cloudflare tokens.

## Production Smoke-Test Sequence

Set the deployed URL locally:

```bash
export GYMLEDGER_WORKER_URL="https://actual-worker-url.workers.dev"
```

Set the API key in the current shell only:

```bash
export GYMLEDGER_API_KEY="<local-shell-only-value>"
```

Do not commit or print the value.

### 1. Health endpoint

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/health"
```

Expected:

- HTTP 200
- stable success envelope
- no secret fields

### 2. Public configuration endpoint

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/config"
```

Expected:

- HTTP 200
- only safe public configuration
- no D1 database ID
- no API keys
- no Open Food Facts contact value
- no secret runtime values

### 3. Generic lookup without authentication

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/foods/generic?q=egg"
```

Expected:

- HTTP 401
- error code `unauthorized`
- no provider call
- no external-call increment

### 4. Barcode lookup without authentication

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/foods/barcode/3017620422003"
```

Expected:

- HTTP 401
- error code `unauthorized`
- no provider call
- no external-call increment

### 5. Incorrect authentication key

Send a deliberately incorrect value.

Expected:

- HTTP 401
- error code `unauthorized`
- no provider call
- no external-call increment

### 6. Safe-mode behavior

With conservative defaults active, call a valid lookup using the correct API key.

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/generic?q=egg"
```

Expected:

- HTTP 503
- error code `lookup_disabled`
- provider is not called
- `blocked_calls` increments
- `external_calls` does not increment

### 7. USDA production lookup

Temporarily enable the required runtime flags.

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/generic?q=egg"
```

Expected:

- HTTP 200
- source `USDA`
- attribution `USDA FoodData Central`
- `isApproximate = true`
- normalized nutrient values
- no raw USDA fields
- no USDA key

### 8. USDA cache verification

Repeat the identical USDA request.

Expected:

- same normalized result
- cache hit increments
- no new external provider call
- cache-entry hit count increments

### 9. Open Food Facts production lookup

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/barcode/3017620422003"
```

Expected:

- HTTP 200
- source `OPEN_FOOD_FACTS`
- attribution includes Open Food Facts and ODbL
- barcode remains a string
- `product.externalId` matches the barcode
- normalized product fields
- no raw provider fields
- no User-Agent value
- no image fields

### 10. Open Food Facts cache verification

Repeat the identical barcode request.

Expected:

- cache hit
- no additional provider call
- stable DTO identity
- cache-entry hit count increments

### 11. Unknown valid barcode

Use an intentionally unknown but structurally valid barcode.

Expected:

- HTTP 404
- error code `not_found`
- no raw provider response

### 12. Invalid barcode

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/barcode/1234"
```

Expected:

- HTTP 400
- error code `invalid_barcode`
- no provider call
- no external-call budget consumed

### 13. Runtime switch verification

Verify each switch separately:

```text
safe_mode = true
    -> lookup_disabled

online_lookup_enabled = false
    -> lookup_disabled

usda_provider_enabled = false
    -> provider_disabled

open_food_facts_provider_enabled = false
    -> provider_disabled

barcode_lookup_enabled = false
    -> feature_disabled
```

Restore the value after each focused test.

## Budget Verification

Do not generate 25 real provider calls to exhaust the budget.

Preferred approach:

1. Read the current UTC date.
2. Read the current `external_calls` value for that date.
3. Temporarily set `daily_external_call_budget` to `current_external_calls + 1`.
4. Perform one fresh uncached lookup.
5. Perform another different uncached lookup.
6. Confirm the second request returns `budget_exceeded`.
7. Restore `daily_external_call_budget = 25`.

Expected blocked request behavior:

- HTTP 429
- error code `budget_exceeded`
- `blocked_calls` increments
- provider is not called
- `external_calls` does not increment for the blocked request

Do not delete or reset production usage rows unless absolutely necessary and explicitly documented.

## D1 Verification

Inspect only operational metadata.

Verify:

- a USDA cache entry exists
- an Open Food Facts cache entry exists
- cache keys use the expected format
- source and lookup type are correct
- cache hit counts increase
- usage counters change by expected deltas
- runtime configuration is restored safely

Expected cache-key patterns:

```text
usda:generic:<normalized-query>
open_food_facts:barcode:<barcode>
```

Do not dump secrets or unnecessary full product payloads.

## Safe-State Restoration

Before phase completion, restore:

```text
safe_mode = true
online_lookup_enabled = false
usda_provider_enabled = false
open_food_facts_provider_enabled = false
generic_food_search_enabled = false
barcode_lookup_enabled = false
daily_external_call_budget = 25
cache_enabled = true
cache_ttl_seconds = 86400
```

Then make one authenticated lookup.

Expected:

```text
lookup_disabled
```

The phase is not complete until this safe state is verified.

## Documentation Required

Update:

```text
docs/CURRENT_PHASE.md
docs/IMPLEMENTATION_PLAN.md
worker/food-lookup/README.md
```

Create:

```text
docs/FOOD_LOOKUP_DEPLOYMENT.md
```

The deployment document may contain:

- Worker name
- public `workers.dev` base URL
- deployment date
- deployed commit SHA
- Wrangler version
- endpoint list
- authentication header name
- safe runtime defaults
- smoke-test results
- known limitations
- rollback instructions

It must not contain:

- `GYMLEDGER_API_KEY`
- USDA API key
- Cloudflare API token
- Cloudflare account ID
- personal Open Food Facts contact value
- secret values
- complete private D1 dumps

## Rollback Plan

Before deployment, identify the currently deployed Worker version, if any.

Required immediate safety controls:

```text
safe_mode = true
online_lookup_enabled = false
usda_provider_enabled = false
open_food_facts_provider_enabled = false
generic_food_search_enabled = false
barcode_lookup_enabled = false
```

Rollback options:

- redeploy the previously approved Git commit
- use Cloudflare deployment/version rollback when available
- restore the previous Worker deployment
- disable all external lookup switches

A production provider failure does not justify bypassing safety controls.

## Files Expected

Likely file to create:

```text
docs/FOOD_LOOKUP_DEPLOYMENT.md
```

Likely files to modify:

```text
docs/CURRENT_PHASE.md
docs/IMPLEMENTATION_PLAN.md
worker/food-lookup/README.md
worker/food-lookup/src/index.ts
worker/food-lookup/src/index.test.ts
```

`index.ts` and its tests should change only if authentication is not currently wired into protected routes.

Possible file to create:

```text
worker/food-lookup/src/auth.test.ts
```

Possible files to modify only when required by a verified blocker:

```text
worker/food-lookup/src/auth.ts
worker/food-lookup/wrangler.toml
```

Do not modify providers, normalizers, cache, usage, or DTO contracts unless a real production defect is reproduced and reported.

## Do Not Do

- Do not modify Android.
- Do not add Retrofit.
- Do not add OkHttp.
- Do not add Android settings.
- Do not begin Phase 17F.
- Do not freeze the final Android contract.
- Do not add provider search.
- Do not add images.
- Do not add user accounts.
- Do not add personal cloud storage.
- Do not expose any key.
- Do not commit `.dev.vars`.
- Do not commit Cloudflare tokens.
- Do not hardcode production secrets.
- Do not put USDA credentials in `wrangler.toml`.
- Do not store provider secrets in D1.
- Do not leave providers enabled after smoke testing.
- Do not intentionally trigger provider rate limits.
- Do not create a custom domain.
- Do not modify the D1 schema unless a real blocker is discovered.
- Do not commit automatically. The user commits manually.

## Acceptance Criteria

- Pre-deploy audit passes.
- Phase 17E.3 is merged into `dev`.
- `npm ci` passes.
- `npm run typecheck` passes.
- `npm test` passes.
- Wrangler dry run passes.
- Cloudflare account is verified.
- D1 database identity is verified.
- Required secret names are configured.
- Remote migration applies successfully.
- Worker deployment succeeds.
- Production URL is documented.
- Health endpoint succeeds publicly.
- Config endpoint succeeds publicly.
- Lookup endpoints require `X-GymLedger-Key`.
- Missing or incorrect key returns `unauthorized`.
- Unauthorized requests do not invoke providers.
- Conservative runtime defaults block lookups.
- USDA lookup succeeds during the controlled smoke window.
- USDA repeat lookup uses cache.
- Open Food Facts lookup succeeds during the controlled smoke window.
- Open Food Facts repeat lookup uses cache.
- Invalid barcode does not consume provider budget.
- Unknown valid barcode returns `not_found`.
- Feature and provider switches work.
- Budget blocking is verified without abusive traffic.
- Runtime defaults are restored.
- No secret appears in Git, responses, or shared logs.
- Android remains untouched.
- Deployment and rollback documentation is complete.

## Validation Commands

From repository root:

```bash
git status --short --untracked-files=all
git diff --name-status
git diff --stat
git diff --check
git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

From Worker:

```bash
cd worker/food-lookup

npm ci
npm run typecheck
npm test
npx wrangler deploy --dry-run
```

Cloudflare identity and resources:

```bash
npx wrangler whoami
npx wrangler d1 list
npx wrangler d1 migrations list gymledger-food-lookup --remote
npx wrangler secret list
```

Remote migration and deploy:

```bash
npx wrangler d1 migrations apply gymledger-food-lookup --remote
npx wrangler deploy
```

Secret safety:

```bash
git status --short --untracked-files=all

git grep -nE \
  "GYMLEDGER_API_KEY=|USDA_API_KEY=|OPEN_FOOD_FACTS_USER_AGENT=.*@" \
  -- ':!docs/CURRENT_PHASE.md' \
     ':!docs/IMPLEMENTATION_PLAN.md' \
     ':!worker/food-lookup/README.md' || true
```

Review every match manually.

Regex checks do not replace manual inspection.

## Stop Conditions

Stop before deployment if:

- Phase 17E.3 is not merged into `dev`
- tests fail
- typecheck fails
- Wrangler dry run fails
- authentication can be bypassed
- lookup authentication is not wired
- the wrong Cloudflare account is active
- the configured D1 database does not match
- an unexpected migration appears
- a required secret is missing
- a real secret is tracked
- `/v1/config` exposes sensitive information
- Android files changed
- Cloudflare unexpectedly requires billing
- provider code changed without review

Stop after deployment and report before patching provider code if:

- production payload differs materially from tested contracts
- provider responses expose unexpected fields
- cache behavior is incorrect
- usage counters are incorrect
- authentication fails
- safe mode fails
- runtime switches cannot be restored
- rollback becomes necessary

## Suggested Commits

Planning:

```text
docs: plan phase 17e4 worker deployment
```

Authentication preparation, when required:

```text
fix: enforce food lookup API authentication
```

Deployment record:

```text
docs: record food lookup worker deployment
```

## Code Preparation Status

Authentication wiring complete. Protected lookup routes now require `X-GymLedger-Key` header.

Pending: deployment, remote D1 migration, secret configuration, production smoke tests.

See [docs/FOOD_LOOKUP_DEPLOYMENT.md](FOOD_LOOKUP_DEPLOYMENT.md) for deployment template.