# Food Lookup Worker Deployment

## Deployment

- Worker: `gymledger-food-lookup`
- Base URL: `https://gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev`
- Environment: production
- Git commit: `eb6b9c189ceacd0d28d0eff8af77598d0e802a83`
- Worker version ID: `6cfbac1c-d699-4aad-85f5-f78866334052`
- Deployment date: 2026-07-11
- Wrangler version: 4.105.0

## Public Endpoints

- `GET /v1/health` — health check, no authentication required
- `GET /v1/config` — safe public configuration, no authentication required

## Protected Endpoints

- `GET /v1/foods/generic?q=<query>` — USDA generic food lookup
- `GET /v1/foods/barcode/:barcode` — Open Food Facts barcode lookup

Authentication header:

```
X-GymLedger-Key: <configured secret>
```

Production requires `GYMLEDGER_API_KEY` to be configured via `npx wrangler secret put GYMLEDGER_API_KEY`.

When the key is configured:
- Missing or incorrect header returns HTTP 401 `unauthorized`
- Correct header allows request processing
- Health and config remain public

When no key is configured (local development only):
- Requests proceed without authentication

## Required Secrets

```bash
npx wrangler secret put GYMLEDGER_API_KEY
npx wrangler secret put USDA_API_KEY
npx wrangler secret put OPEN_FOOD_FACTS_USER_AGENT
```

Enter values interactively. Never commit secret values.

## D1 Migration

```bash
npx wrangler d1 migrations list gymledger-food-lookup --remote
npx wrangler d1 migrations apply gymledger-food-lookup --remote
```

Verify tables exist:
- `food_lookup_cache`
- `usage_daily`
- `runtime_config`

## Runtime State

Target final runtime state: conservative defaults.
Restoration verification: PASS — verified after targeted retest.

Conservative defaults:
- `safe_mode=true`
- `online_lookup_enabled=false`
- All providers disabled
- All lookup features disabled
- `daily_external_call_budget=25`
- `cache_enabled=true`
- `cache_ttl_seconds=86400`

## Smoke Tests

### Passed on Current Deployed Version

| Test | Status |
|------|--------|
| Worker deployment | PASS |
| D1 migration | PASS |
| health endpoint — returns 200, no secrets | PASS |
| config endpoint — returns 200, no secrets exposed | PASS |
| authentication — missing key returns 401 | PASS |
| authentication — wrong key returns 401 | PASS |
| authentication — correct key proceeds | PASS |
| safe-mode uncached generic lookup returns lookup_disabled | PASS |
| safe-mode uncached barcode lookup returns lookup_disabled | PASS |
| live USDA generic lookup | PASS |
| USDA cache hit | PASS |
| live Open Food Facts barcode lookup | PASS |
| Open Food Facts cache hit | PASS |
| invalid barcode validation | PASS |
| unknown valid barcode returns not_found | NOT VERIFIED |
| provider-disabled gate | PASS |
| barcode-feature-disabled gate | PASS |
| cached USDA response before safe-mode gate | PASS |
| cached barcode response before safe-mode gate | PASS |
| restoration to conservative defaults | PASS |

### Passed After Redeploy (Hardening Patch)

The three defects discovered during initial production smoke testing were corrected by a hardening patch (deployed commit `eb6b9c189ceacd0d28d0eff8af77598d0e802a83`). Targeted retests after redeploy confirm all three pass:

| Test | Status | Detail |
|------|--------|--------|
| daily_external_call_budget=0 | PASS AFTER REDEPLOY | Zero-budget request returned HTTP 429 `budget_exceeded`; external_calls did not increment. |
| generic_food_search_enabled=false | PASS AFTER REDEPLOY | Unauthenticated generic lookup returned HTTP 503 `feature_disabled`; external_calls remained stable. |
| dynamic /v1/config | PASS AFTER REDEPLOY | Endpoint reflects runtime overrides when D1 runtime_config rows exist, and conservative defaults when runtime_config is empty. |

#### Detailed Targeted Retest Results

**Empty runtime_config (conservative defaults):**
- `onlineLookupAvailable=false`
- `providers.usda=false`
- `providers.openFoodFacts=false`
- `features.genericFoodSearch=false`
- `features.barcodeLookup=false`
- `minQueryLength=3`
- `safeMode=true`

**Runtime overrides reflected dynamically:**
- `onlineLookupAvailable=true`
- `providers.usda=true`
- `providers.openFoodFacts=true`
- `features.genericFoodSearch=false` (intentionally left disabled)
- `features.barcodeLookup=true`
- `safeMode=false`

**generic_food_search_enabled=false gate:**
- HTTP 503 `feature_disabled`
- `external_calls` remained 4

**daily_external_call_budget=0:**
- HTTP 429 `budget_exceeded`
- `external_calls` remained 4

**Final conservative restoration:**
- `runtime_config` emptied
- Public config returned conservative defaults
- Uncached authenticated generic lookup returned HTTP 503 `lookup_disabled`
- `external_calls` remained 4

## Smoke-Test Defects Discovered

Production smoke testing discovered three defects in the initial deployed version. The hardening patch (deployed commit `eb6b9c189ceacd0d28d0eff8af77598d0e802a83`) corrected all three. Each passed targeted production retest after redeploy.

### History

1. **Zero budget fallback** (initial: FAIL → PATCHED → PASS AFTER REDEPLOY): `daily_external_call_budget="0"` incorrectly resolved to 25 due to JavaScript truthiness fallback in `getMaxDailyExternalCalls`. A budget of 0 should block all external calls. Fix: parse with explicit zero handling; negative→0, decimal→25, malformed→25, empty→25.

2. **Missing generic feature gate** (initial: FAIL → PATCHED → PASS AFTER REDEPLOY): `generic_food_search_enabled` existed in runtime config defaults but was not enforced in `handleGenericFoodLookup`. The gate chain skipped the feature-enabled check, allowing generic lookups to proceed when only the provider flag was enabled. Fix: added `generic_food_search_enabled` gate to `handleGenericFoodLookup`.

3. **Static public config** (initial: FAIL → PATCHED → PASS AFTER REDEPLOY): `GET /v1/config` returned a hardcoded static constant instead of reflecting actual D1 runtime configuration. The endpoint disagreed with effective runtime state. Fix: `GET /v1/config` now reads runtime overrides from D1 `runtime_config` and falls back to conservative defaults when empty.

## Metrics

### Baseline (after initial smoke tests, before targeted retests)

- `external_calls=4`
- `cache_hits=4`
- `cache_misses=14`
- `blocked_calls=10`

### Final (after targeted retests and conservative restoration)

- `external_calls=4`
- `cache_hits=4`
- `cache_misses=17`
- `blocked_calls=13`

### Change Explanation

| Metric | Delta | Cause |
|--------|-------|-------|
| cache_misses | +3 | One each for: feature-disabled test, zero-budget test, final safe-mode validation |
| blocked_calls | +3 | One each for: feature-disabled test, zero-budget test, final safe-mode validation |
| external_calls | 0 | No new external provider calls during targeted retests |
| cache_hits | 0 | No cache-hit-producing requests during targeted retests |

All targeted retests verified runtime gates and budget enforcement without invoking external providers.

## Barcode Result Accuracy

- **Invalid barcode syntax** (e.g., `1234`): PASS — returns HTTP 400 `invalid_barcode`, no provider call.
- **Valid but unknown barcode**: NOT VERIFIED. The attempted barcode `9999999999999` returned HTTP 200 because it exists in Open Food Facts as a test product. No production `not_found` result was obtained against a genuinely unknown barcode.

## Final Phase State

Phase 17E.4 / Backend Phase B5 is complete:

- Worker deployed to Cloudflare Workers.
- D1 migration applied remotely.
- Required secrets (`GYMLEDGER_API_KEY`, `USDA_API_KEY`, `OPEN_FOOD_FACTS_USER_AGENT`) configured via `wrangler secret put`.
- Authentication contract verified: public endpoints accessible, protected endpoints require `X-GymLedger-Key`.
- USDA generic food lookup passed production smoke test (HTTP 200, source `USDA`, normalized DTO, no raw fields).
- Open Food Facts barcode lookup passed production smoke test (HTTP 200, source `OPEN_FOOD_FACTS`, barcode preserved as string).
- Cache behavior confirmed for both providers (cache hit increments, no repeat provider call).
- Runtime configuration gates verified after hardening patch redeploy:
  - Zero-budget parsing corrected.
  - Generic feature-gate enforcement added.
  - Dynamic `/v1/config` implemented.
- Final conservative state verified: `runtime_config` empty returns conservative defaults; uncached authenticated lookup returns HTTP 503 `lookup_disabled`.
- No secrets are included in this document.
- Branch `17e4-worker-deploy-smoke-plan` is not merged to `dev` (pending user approval).

This phase closes Phase 17E (Backend Phases B1–B5). Android integration (Phase 17F / Backend Phase B6) is the next phase.

## Known Limitations

- Open Food Facts per-serving nutrition may be unavailable
- Open Food Facts products using per-100-ml nutrition are not converted into per-100-g values
- Provider-derived nutrition remains approximate
- Android integration is not implemented yet
- Custom domain is not configured

## Rollback

Immediate safety controls:

```bash
npx wrangler d1 execute gymledger-food-lookup --remote --command="
INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('safe_mode', 'true', datetime('now'));
INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('online_lookup_enabled', 'false', datetime('now'));
INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('usda_provider_enabled', 'false', datetime('now'));
INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('open_food_facts_provider_enabled', 'false', datetime('now'));
INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('generic_food_search_enabled', 'false', datetime('now'));
INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('barcode_lookup_enabled', 'false', datetime('now'));
"
```

Rollback options:
- Redeploy the previously approved Git commit
- Use Cloudflare deployment/version rollback when available
- Restore the previous Worker deployment
- Disable all external lookup switches via D1

## Cost

Expected cost target: $0/month within Cloudflare free-tier limits.
Production cost verification: NOT YET DETERMINED — requires extended observation beyond smoke-test window.
