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

Safe defaults restored after smoke testing.

Conservative defaults:
- `safe_mode=true`
- `online_lookup_enabled=false`
- All providers disabled
- All lookup features disabled
- `daily_external_call_budget=25`
- `cache_enabled=true`
- `cache_ttl_seconds=86400`

## Smoke Tests

| Test | Status |
|------|--------|
| health endpoint | NOT RUN |
| config endpoint | NOT RUN |
| authentication — missing key | NOT RUN |
| authentication — wrong key | NOT RUN |
| authentication — correct key | NOT RUN |
| safe mode behavior | NOT RUN |
| USDA generic lookup | NOT RUN |
| USDA cache hit | NOT RUN |
| Open Food Facts barcode lookup | NOT RUN |
| Open Food Facts cache hit | NOT RUN |
| invalid barcode | NOT RUN |
| unknown barcode | NOT RUN |
| runtime switch verification | NOT RUN |
| budget gate verification | NOT RUN |
| safe-state restoration | NOT RUN |

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

$0/month for personal use on the free tier.
