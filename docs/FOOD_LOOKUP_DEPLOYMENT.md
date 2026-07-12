# Food Lookup Worker Deployment

## Deployment

- Worker: `gymledger-food-lookup`
- Base URL: PENDING
- Environment: production
- Git commit: PENDING
- Deployment date: PENDING
- Wrangler version: PENDING

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
Restoration verification: PENDING.

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
| unknown valid barcode returns not_found | PASS |
| provider-disabled gate | PASS |
| barcode-feature-disabled gate | PASS |
| cached USDA response before safe-mode gate | PASS |
| cached barcode response before safe-mode gate | PASS |
| restoration to conservative defaults | PASS |

### Failed on Current Deployed Version

| Test | Status | Detail |
|------|--------|--------|
| daily_external_call_budget=0 | FAIL — PATCHED LOCALLY | Set to "0" in D1; new authenticated USDA request returned HTTP 200 and external_calls incremented. Root cause: `parseInt("0",10) \|\| 25` resolved 0 to 25 via JavaScript truthiness. |
| generic_food_search_enabled enforcement | FAIL — PATCHED LOCALLY | Flag existed in DEFAULT_CONFIG but `handleGenericFoodLookup` did not read or enforce it. Generic lookups proceeded without this gate. |
| dynamic /v1/config | FAIL — PATCHED LOCALLY | Endpoint returned static `PUBLIC_CONFIG` constant. Did not reflect D1 runtime configuration when flags were overridden. |

### Fixed Locally, Not Yet Verified in Production

| Fix | Local Test Status |
|-----|-------------------|
| Zero-budget parsing: "0"→0, negative→0, decimal→25, malformed→25, empty→25 | Unit tests PASS |
| generic_food_search_enabled gate in handleGenericFoodLookup | Unit tests PASS |
| Dynamic /v1/config reading from D1 runtime_config | Unit tests PASS |

### Pending After Redeploy

| Test | Status |
|------|--------|
| daily_external_call_budget=0 returns budget_exceeded without external call | PENDING REDEPLOY |
| generic_food_search_enabled=false returns feature_disabled | PENDING REDEPLOY |
| /v1/config reflects runtime overrides from D1 | PENDING REDEPLOY |
| /v1/config returns conservative defaults when runtime_config is empty | PENDING REDEPLOY |
| final conservative-state restoration after targeted retest | PENDING REDEPLOY |

## Smoke-Test Defects Discovered

Production smoke testing discovered three defects that require a local hardening patch before redeployment:

1. **Zero budget fallback**: `daily_external_call_budget="0"` incorrectly resolved to 25 due to JavaScript truthiness fallback in `getMaxDailyExternalCalls`. A budget of 0 should block all external calls.

2. **Missing generic feature gate**: `generic_food_search_enabled` existed in runtime config defaults but was not enforced in `handleGenericFoodLookup`. The gate chain skipped the feature-enabled check, allowing generic lookups to proceed when only the provider flag was enabled.

3. **Static public config**: `GET /v1/config` returned a hardcoded static constant instead of reflecting actual D1 runtime configuration. The endpoint disagreed with effective runtime state.

A local hardening patch has been prepared to fix all three defects. The patch has not yet been redeployed. Production remains restored to conservative defaults.

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
Production cost verification: PENDING.
