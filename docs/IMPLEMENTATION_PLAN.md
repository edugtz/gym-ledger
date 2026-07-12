# Phase 17E.4 Implementation Plan — Worker Deploy and Production Smoke Tests

## 1. Goal

Safely deploy the GymLedger Food Lookup Worker to Cloudflare Workers and verify production behavior.

This phase must leave the deployed system in a conservative disabled state after smoke testing.

No Android work is included.

## 2. Source of Truth

Read before editing or deploying:

```text
AGENTS.md
AI_WORKFLOW.md
docs/CURRENT_PHASE.md
docs/IMPLEMENTATION_PLAN.md
docs/BACKEND_CLOUD_PHASES.md
worker/food-lookup/README.md
worker/food-lookup/wrangler.toml
worker/food-lookup/package.json
worker/food-lookup/src/index.ts
worker/food-lookup/src/auth.ts
worker/food-lookup/src/config.ts
worker/food-lookup/src/runtimeConfig.ts
worker/food-lookup/src/usage.ts
worker/food-lookup/src/cache.ts
worker/food-lookup/src/errors.ts
worker/food-lookup/migrations/
```

Inspect the repository before selecting exact files to modify.

## 3. Phase Boundaries

Included:

- repository and deployment audit
- lookup-route authentication preparation
- Cloudflare identity verification
- secret-name configuration
- remote D1 migration
- Worker deployment
- production smoke tests
- cache verification
- runtime-switch verification
- budget verification
- safe-state restoration
- deployment documentation
- rollback documentation

Excluded:

- Android integration
- Android API client
- Android settings
- DTO contract freeze
- custom domain
- analytics platform
- monitoring platform
- provider feature additions
- provider text search
- product images
- personal cloud data
- user accounts
- D1 schema redesign

## 4. Controlled Execution Stages

Execute the phase in five stages.

### Stage A — Repository and configuration audit

No remote writes.

### Stage B — Authentication and deployment-readiness patch

Local code changes only.

No deployment.

### Stage C — Cloudflare resource and secret setup

Remote configuration performed manually by the user.

### Stage D — Deployment and production smoke testing

Controlled external traffic.

### Stage E — Restore safe state and document deployment

No providers left enabled unintentionally.

Do not skip directly to deployment.

## 5. Stage A — Repository Audit

Start from updated `dev`:

```bash
cd ~/AndroidStudioProjects/GymLedger

git checkout dev
git pull --ff-only origin dev
git log --oneline --decorate -5
git status --short
```

Confirm Phase 17E.3 is merged.

Create the planning branch:

```bash
git checkout -b 17e4-worker-deploy-smoke-plan
```

Run baseline Worker validation:

```bash
cd worker/food-lookup

npm ci
npm run typecheck
npm test
npx wrangler deploy --dry-run
```

Confirm Android is untouched:

```bash
cd ../..

git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

Inspect tracked files for potential secrets:

```bash
git status --short --untracked-files=all

git grep -nE \
  "GYMLEDGER_API_KEY=|USDA_API_KEY=|OPEN_FOOD_FACTS_USER_AGENT=.*@" \
  -- ':!docs/CURRENT_PHASE.md' \
     ':!docs/IMPLEMENTATION_PLAN.md' \
     ':!worker/food-lookup/README.md' || true
```

Review matches manually.

Placeholder examples are not necessarily secrets, but every result must be inspected.

## 6. Authentication Audit

Intended endpoint policy:

```text
/v1/health                         public
/v1/config                         public
/v1/foods/generic                  protected
/v1/foods/barcode/:barcode         protected
```

Inspect whether `validateApiKey()` is called by both lookup routes before invoking their services.

Required request flow for generic lookup:

```text
route match
-> method validation
-> query validation
-> API-key validation
-> service invocation
```

Required request flow for barcode lookup:

```text
route match
-> method validation
-> strict path and barcode validation
-> API-key validation
-> service invocation
```

This preserves existing input errors while preventing provider calls without authentication.

Required behavior:

- malformed generic query returns `invalid_query`
- malformed barcode route returns `invalid_barcode`
- valid lookup without configured header returns `unauthorized`
- valid lookup with wrong header returns `unauthorized`
- valid lookup with correct header proceeds
- health remains public
- config remains public
- unauthorized requests do not call lookup services
- unauthorized requests do not update provider counters

## 7. Existing Authentication Helper

Current helper behavior must be inspected.

Expected contract:

```ts
validateApiKey(request, env)
```

When `GYMLEDGER_API_KEY` is configured:

- missing `X-GymLedger-Key` returns false
- wrong `X-GymLedger-Key` returns false
- correct `X-GymLedger-Key` returns true

When no key is configured:

- local-development behavior may continue allowing requests
- production must not rely on this fallback
- production safety requires configuring the secret

Do not introduce accounts, JWTs, sessions, or OAuth.

## 8. Authentication Code Changes

Likely modify:

```text
worker/food-lookup/src/index.ts
worker/food-lookup/src/index.test.ts
```

Potentially create:

```text
worker/food-lookup/src/auth.test.ts
```

Modify `auth.ts` only when an actual defect is discovered.

Suggested route-level pattern:

```ts
if (!validateApiKey(request, env)) {
  return error("unauthorized");
}
```

Apply only to the lookup endpoints.

Do not protect `/v1/health` or `/v1/config`.

## 9. Authentication Test Requirements

Add tests for:

### Helper tests

- configured key with missing header
- configured key with incorrect header
- configured key with correct header
- no configured key preserves documented local behavior

### Generic route tests

- valid query without key returns unauthorized
- valid query with wrong key returns unauthorized
- valid query with correct key reaches service behavior
- invalid query remains invalid_query
- unauthorized request does not invoke provider path

### Barcode route tests

- valid barcode without key returns unauthorized
- valid barcode with wrong key returns unauthorized
- valid barcode with correct key reaches service behavior
- invalid barcode remains invalid_barcode
- nested path remains invalid_barcode
- unauthorized request does not invoke provider path

### Public route tests

- health remains public
- config remains public

Keep all existing tests green.

## 10. Wrangler Configuration Audit

Inspect:

```text
worker/food-lookup/wrangler.toml
```

Verify:

```toml
name = "gymledger-food-lookup"
main = "src/index.ts"
workers_dev = true
```

Verify:

- `compatibility_date` is accepted by the installed Wrangler version
- D1 binding name is `DB`
- database name is `gymledger-food-lookup`
- database ID belongs to the intended account
- `[vars]` contains no secrets
- no provider key is committed
- no API key is committed

Only modify `wrangler.toml` for a verified deployment blocker.

## 11. Cloudflare Identity Verification

Run manually:

```bash
cd worker/food-lookup

npx wrangler whoami
npx wrangler d1 list
```

Confirm:

- authenticated account is the intended account
- D1 database `gymledger-food-lookup` exists
- listed database ID matches `wrangler.toml`

Do not commit Cloudflare account identifiers.

Stop if the account or database does not match.

## 12. Required Production Secret Names

Required:

```text
GYMLEDGER_API_KEY
USDA_API_KEY
OPEN_FOOD_FACTS_USER_AGENT
```

List configured secret names:

```bash
npx wrangler secret list
```

Set missing values interactively:

```bash
npx wrangler secret put GYMLEDGER_API_KEY
npx wrangler secret put USDA_API_KEY
npx wrangler secret put OPEN_FOOD_FACTS_USER_AGENT
```

The user enters values manually.

Do not include values in:

- source code
- Markdown files
- Wrangler configuration
- shell history
- Git commits
- D1
- model prompts
- shared logs

## 13. Remote Migration Audit

List pending migrations:

```bash
npx wrangler d1 migrations list gymledger-food-lookup --remote
```

Expected migration:

```text
0001_cache_budget_foundation.sql
```

Inspect the migration before applying.

Apply only when the correct account and database are verified:

```bash
npx wrangler d1 migrations apply gymledger-food-lookup --remote
```

After applying, verify schema names only:

```text
food_lookup_cache
usage_daily
runtime_config
```

Do not dump secret values or unnecessary normalized payloads.

## 14. Production Runtime Configuration States

### Required safe state

```text
safe_mode=true
online_lookup_enabled=false
usda_provider_enabled=false
open_food_facts_provider_enabled=false
generic_food_search_enabled=false
barcode_lookup_enabled=false
daily_external_call_budget=25
cache_enabled=true
cache_ttl_seconds=86400
```

### Temporary smoke-test state

```text
safe_mode=false
online_lookup_enabled=true
usda_provider_enabled=true
open_food_facts_provider_enabled=true
generic_food_search_enabled=true
barcode_lookup_enabled=true
daily_external_call_budget=25
cache_enabled=true
cache_ttl_seconds=86400
```

Every remote D1 command must include:

```text
--remote
```

Review every SQL statement before executing it.

Prefer one reviewed multi-statement transaction when supported.

## 15. Safe-State SQL Template

Use reviewed commands similar to:

```bash
npx wrangler d1 execute gymledger-food-lookup --remote --command="
INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('safe_mode', 'true', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('online_lookup_enabled', 'false', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('usda_provider_enabled', 'false', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('open_food_facts_provider_enabled', 'false', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('generic_food_search_enabled', 'false', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('barcode_lookup_enabled', 'false', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('daily_external_call_budget', '25', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('cache_enabled', 'true', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('cache_ttl_seconds', '86400', datetime('now'));
"
```

Confirm quoting and Wrangler compatibility during preflight.

Do not execute before database identity is verified.

## 16. Smoke-State SQL Template

Use reviewed commands similar to:

```bash
npx wrangler d1 execute gymledger-food-lookup --remote --command="
INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('safe_mode', 'false', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('online_lookup_enabled', 'true', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('usda_provider_enabled', 'true', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('open_food_facts_provider_enabled', 'true', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('generic_food_search_enabled', 'true', datetime('now'));

INSERT OR REPLACE INTO runtime_config (key, value, updated_at)
VALUES ('barcode_lookup_enabled', 'true', datetime('now'));
"
```

This state is temporary.

Safe-state restoration is mandatory.

## 17. Deployment Dry Run

Run:

```bash
cd worker/food-lookup

npm ci
npm run typecheck
npm test
npx wrangler deploy --dry-run
```

Stop on the first failure.

Do not deploy with failing tests.

## 18. Deployment

Deploy manually:

```bash
npx wrangler deploy
```

Record:

```text
Worker name
workers.dev base URL
UTC deployment timestamp
deployed Git commit SHA
deployment identifier/version
Wrangler version
```

Do not record secret values.

## 19. Smoke-Test Environment Variables

Use local shell variables:

```bash
export GYMLEDGER_WORKER_URL="https://actual-worker-url.workers.dev"
export GYMLEDGER_API_KEY="<local-shell-only-value>"
```

Do not commit or echo the key.

## 20. Authentication Smoke Tests

Before enabling providers:

### Health

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/health"
```

Expected:

```text
HTTP 200
```

### Config

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/config"
```

Expected:

```text
HTTP 200
```

### Generic lookup without key

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/foods/generic?q=egg"
```

Expected:

```text
HTTP 401
unauthorized
```

### Barcode lookup without key

```bash
curl -i "$GYMLEDGER_WORKER_URL/v1/foods/barcode/3017620422003"
```

Expected:

```text
HTTP 401
unauthorized
```

### Wrong key

Use an explicitly incorrect local value.

Expected:

```text
HTTP 401
unauthorized
```

### Correct key with safe mode active

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/generic?q=egg"
```

Expected:

```text
HTTP 503
lookup_disabled
```

Compare usage-counter deltas to confirm unauthorized calls did not consume external-call budget.

## 21. USDA Production Smoke Test

Temporarily enable the smoke state.

Call:

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/generic?q=egg"
```

Verify:

- HTTP 200
- stable success envelope
- query is present
- source is `USDA`
- attribution is present
- `isApproximate` is true
- results array exists
- nutrient fields are normalized
- no provider key appears
- no raw USDA response fields appear

Repeat the exact request.

Verify using D1 counter deltas:

- first request may increment `external_calls`
- second request increments `cache_hits`
- second request does not increment `external_calls`
- cache-entry hit count increments

Do not assume counters begin at zero.

## 22. Open Food Facts Production Smoke Test

Use:

```text
3017620422003
```

Call:

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/barcode/3017620422003"
```

Verify:

- HTTP 200
- barcode remains a string
- source equals `OPEN_FOOD_FACTS`
- attribution includes Open Food Facts and ODbL
- `isApproximate` is true
- product object exists
- externalId matches requested barcode
- normalized nutrition is present when available
- no raw provider status/result fields appear
- no image fields appear
- no User-Agent value appears

Repeat the exact request.

Verify:

- cache hit increments
- external-call count does not increment
- cached barcode identity remains correct

## 23. Negative Production Tests

### Invalid barcode

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/barcode/1234"
```

Expected:

```text
HTTP 400
invalid_barcode
```

No provider call.

### Unknown valid barcode

Use one valid-format barcode known not to exist.

Expected:

```text
HTTP 404
not_found
```

### Disabled USDA provider

Expected:

```text
provider_disabled
```

### Disabled Open Food Facts provider

Expected:

```text
provider_disabled
```

### Disabled barcode feature

Expected:

```text
feature_disabled
```

### Disabled online lookup

Expected:

```text
lookup_disabled
```

### Safe mode

Expected:

```text
lookup_disabled
```

Do not intentionally trigger provider rate limits.

## 24. Budget-Gate Test

Preferred sequence:

1. Get current UTC date.
2. Read current `external_calls` value.
3. Temporarily set the daily budget to `external_calls + 1`.
4. Make one fresh uncached provider request.
5. Make another different uncached provider request.
6. Confirm the second request returns `budget_exceeded`.
7. Confirm the second request does not increment `external_calls`.
8. Confirm `blocked_calls` increments.
9. Restore budget to `25`.

Do not delete usage data.

Do not run high-volume calls.

## 25. D1 Cache Verification

Inspect selected fields only:

```text
cache_key
source
lookup_type
query
attribution
is_approximate
expires_at
hit_count
last_hit_at
```

Expected entries include:

```text
usda:generic:egg
open_food_facts:barcode:3017620422003
```

Inspect `normalized_json` only when required.

Do not share unnecessary provider-derived product details in logs.

Verify no raw provider payload was stored.

## 26. Runtime Restoration

Mandatory final state:

```text
safe_mode=true
online_lookup_enabled=false
usda_provider_enabled=false
open_food_facts_provider_enabled=false
generic_food_search_enabled=false
barcode_lookup_enabled=false
daily_external_call_budget=25
cache_enabled=true
cache_ttl_seconds=86400
```

Apply the reviewed safe-state command.

Then verify:

```bash
curl -i \
  -H "X-GymLedger-Key: $GYMLEDGER_API_KEY" \
  "$GYMLEDGER_WORKER_URL/v1/foods/generic?q=egg"
```

Expected:

```text
HTTP 503
lookup_disabled
```

The phase cannot pass without this confirmation.

## 27. Deployment Documentation

Create:

```text
docs/FOOD_LOOKUP_DEPLOYMENT.md
```

Required structure:

```markdown
# Food Lookup Worker Deployment

## Deployment

- Worker: `gymledger-food-lookup`
- Base URL: `https://...workers.dev`
- Environment: production
- Git commit: `<sha>`
- Deployment date: `<UTC timestamp>`
- Wrangler version: `<version>`

## Public Endpoints

- `GET /v1/health`
- `GET /v1/config`

## Protected Endpoints

- `GET /v1/foods/generic?q=<query>`
- `GET /v1/foods/barcode/:barcode`

Authentication header:

`X-GymLedger-Key`

## Runtime State

Safe defaults restored after smoke testing.

## Smoke Tests

- health: PASS
- config: PASS
- authentication: PASS
- safe mode: PASS
- USDA lookup: PASS
- USDA cache: PASS
- Open Food Facts lookup: PASS
- Open Food Facts cache: PASS
- invalid barcode: PASS
- unknown barcode: PASS
- runtime switches: PASS
- budget gate: PASS

## Known Limitations

- Open Food Facts per-serving nutrition may be unavailable.
- Open Food Facts products using per-100-ml nutrition are not converted into per-100-g values.
- Provider-derived nutrition remains approximate.
- Android integration is not implemented yet.

## Rollback

- enable `safe_mode`
- disable `online_lookup_enabled`
- disable provider flags
- disable lookup feature flags
- redeploy the previous approved commit
```

Never include secret values.

## 28. README Update

Update `worker/food-lookup/README.md` with:

- deployed endpoint reference
- protected endpoint authentication header
- production secret setup commands without values
- remote migration commands
- deployment commands
- safe runtime defaults
- smoke-test overview
- rollback and kill-switch procedure
- link to `docs/FOOD_LOOKUP_DEPLOYMENT.md`

Do not duplicate private account metadata.

## 29. Files Expected

Create:

```text
docs/FOOD_LOOKUP_DEPLOYMENT.md
```

Modify:

```text
docs/CURRENT_PHASE.md
docs/IMPLEMENTATION_PLAN.md
worker/food-lookup/README.md
worker/food-lookup/src/index.ts
worker/food-lookup/src/index.test.ts
```

Potentially create:

```text
worker/food-lookup/src/auth.test.ts
```

Modify only when a verified blocker requires it:

```text
worker/food-lookup/src/auth.ts
worker/food-lookup/wrangler.toml
```

Do not modify:

```text
worker/food-lookup/src/providers/usda.ts
worker/food-lookup/src/providers/openFoodFacts.ts
worker/food-lookup/src/normalizers/
worker/food-lookup/src/services/
worker/food-lookup/src/cache.ts
worker/food-lookup/src/usage.ts
worker/food-lookup/migrations/
app/
build.gradle.kts
settings.gradle.kts
gradle/libs.versions.toml
```

unless a real deployment defect is reproduced and reported before editing.

## 30. Test Requirements

All existing tests must remain green.

Add authentication coverage for:

- public health
- public config
- generic missing key
- generic wrong key
- generic correct key
- barcode missing key
- barcode wrong key
- barcode correct key
- invalid query remains invalid_query
- invalid barcode remains invalid_barcode
- unauthorized provider invocation is prevented

Expected test count:

```text
228 or more
```

No live provider calls in unit tests.

## 31. Quality Gates

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

Cloudflare verification:

```bash
npx wrangler whoami
npx wrangler d1 list
npx wrangler d1 migrations list gymledger-food-lookup --remote
npx wrangler secret list
```

Secret gate:

```bash
cd ../..

git grep -nE \
  "GYMLEDGER_API_KEY=|USDA_API_KEY=|OPEN_FOOD_FACTS_USER_AGENT=.*@" \
  -- ':!docs/CURRENT_PHASE.md' \
     ':!docs/IMPLEMENTATION_PLAN.md' \
     ':!worker/food-lookup/README.md' || true
```

Android package safety:

```bash
grep -R -n "com\.gymledger" app/src || true
```

## 32. Stop Conditions

Stop immediately when:

- Phase 17E.3 is absent from `dev`
- repository is dirty unexpectedly
- tests fail
- typecheck fails
- dry run fails
- authentication is bypassable
- lookup services execute before authentication
- incorrect Cloudflare account is active
- D1 database ID does not match
- unexpected migration appears
- required secret name is missing
- secret value is tracked
- public config exposes sensitive data
- deployment unexpectedly requires billing
- runtime flags cannot be restored
- Android files changed
- provider code requires modification

After deployment, stop and report before patching when:

- real provider response differs materially from tests
- authentication fails
- safe mode fails
- cache stores raw payload
- cache keys are incorrect
- usage counters are inconsistent
- rollback is required

## 33. Code Preparation Report

Before remote deployment, report:

1. Files created.
2. Files modified.
3. Authentication route behavior.
4. Authentication tests.
5. Total unit-test count.
6. Typecheck result.
7. Wrangler dry-run result.
8. Wrangler configuration audit.
9. Android untouched.
10. No secrets committed.
11. Whether deployment is approved to proceed.

## 34. Deployment Report

After deployment and safe-state restoration, report:

1. Cloudflare account verified.
2. D1 database verified.
3. Configured secret names.
4. Remote migration result.
5. Deployment URL.
6. Deployment identifier.
7. Deployed commit SHA.
8. Health result.
9. Config result.
10. Authentication results.
11. Safe-mode result.
12. USDA result.
13. USDA cache delta.
14. Open Food Facts result.
15. Open Food Facts cache delta.
16. Invalid barcode result.
17. Unknown barcode result.
18. Runtime-switch results.
19. Budget-gate result.
20. Safe defaults restored.
21. Known limitations.
22. Rollback readiness.

Do not include secret values.

## 35. Suggested Commits

Planning:

```text
docs: plan phase 17e4 worker deployment
```

Authentication preparation:

```text
fix: enforce food lookup API authentication
```

Deployment documentation:

```text
docs: record food lookup worker deployment
```