# GymLedger Food Lookup Worker

Cloudflare Worker foundation for GymLedger online-assisted food lookup.

Phase 17E.1 — D1 Cache and Budget Foundation.

Phase 17E.2 — USDA Generic Food Lookup Provider.

Phase 17E.3 — Open Food Facts Barcode Lookup Provider.

Phase 17E.4 — Worker Deploy and Production Smoke Tests.

## Status

USDA generic food lookup provider implemented.

Open Food Facts barcode lookup provider implemented.

D1 cache and budget foundation implemented.

Authentication wired into protected lookup routes.

No personal data storage.

No Android integration yet.

No secrets should be committed.

## Production Deployment

See [docs/FOOD_LOOKUP_DEPLOYMENT.md](../../docs/FOOD_LOOKUP_DEPLOYMENT.md) for the deployment template and production details.

## Endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| GET | /v1/health | Health check | Public |
| GET | /v1/config | Safe public config | Public |
| GET | /v1/foods/generic?q=<query> | USDA generic food lookup | Protected |
| GET | /v1/foods/barcode/:barcode | Open Food Facts barcode lookup | Protected |

## Authentication

Protected endpoints (`/v1/foods/generic` and `/v1/foods/barcode/:barcode`) require the `X-GymLedger-Key` header when `GYMLEDGER_API_KEY` is configured.

Behavior:
- Missing or incorrect key → HTTP 401 `unauthorized`
- Correct key → request proceeds to service
- No key configured (local development) → request proceeds without auth

Public endpoints (`/v1/health` and `/v1/config`) are always accessible without authentication.

## Setup

```bash
cd worker/food-lookup
npm install
```

## D1 Database Setup

To set up the local D1 database for development:

1. Create the database:
```bash
npx wrangler d1 create gymledger-food-lookup
```

2. Copy the generated `database_id` into `wrangler.toml`:
```toml
[[d1_databases]]
binding = "DB"
database_name = "gymledger-food-lookup"
database_id = "<YOUR_DATABASE_ID_HERE>"
```

3. Run the initial migration:
```bash
npx wrangler d1 migrations apply gymledger-food-lookup --local
```

## USDA API Key Setup

Create a `.dev.vars` file in `worker/food-lookup/` (this file is gitignored):

```
USDA_API_KEY=your_usda_api_key_here
```

Get a USDA API key from https://fdc.nal.usda.gov/api-key-signup

Never commit `.dev.vars` or real API keys.

For production, use Cloudflare Workers secrets:

```bash
wrangler secret put USDA_API_KEY
```

## Local Dev

```bash
npm run dev
```

Worker starts at `http://localhost:8787`.

## Validate

```bash
npm run typecheck
npm test
```

D1 local migration validation:
```bash
npx wrangler d1 migrations list gymledger-food-lookup --local
npx wrangler d1 migrations apply gymledger-food-lookup --local
```

## Manual Curl Check

```bash
curl -i http://localhost:8787/v1/health
curl -i http://localhost:8787/v1/config
curl -i "http://localhost:8787/v1/foods/generic?q=egg"
curl -i "http://localhost:8787/v1/foods/generic?q="
curl -i "http://localhost:8787/v1/foods/generic?q=ab"
curl -i -X POST http://localhost:8787/v1/foods/generic?q=egg
curl -i http://localhost:8787/v1/foods/barcode/3017620422003
curl -i http://localhost:8787/v1/foods/barcode/1234
curl -i http://localhost:8787/v1/foods/barcode/ABC123
curl -i -X POST http://localhost:8787/v1/foods/barcode/3017620422003
```

## Local Provider QA

By default, the Worker runs in safe mode with all providers disabled.

### USDA Provider QA

1. Ensure `.dev.vars` contains `USDA_API_KEY=<your-key>`.

2. Apply local D1 migrations:
```bash
npx wrangler d1 migrations apply gymledger-food-lookup --local
```

3. Enable lookup via local D1:
```bash
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('safe_mode', 'false', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('online_lookup_enabled', 'true', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('usda_provider_enabled', 'true', datetime('now'));"
```

4. Start the Worker:
```bash
npm run dev
```

5. Test:
```bash
curl -i "http://localhost:8787/v1/foods/generic?q=egg"
```

6. Repeat the same query to verify cache behavior (second call is served from cache).

7. Restore conservative defaults:
```bash
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('safe_mode', 'true', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('online_lookup_enabled', 'false', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('usda_provider_enabled', 'false', datetime('now'));"
```

### Open Food Facts Barcode Provider QA

1. Ensure `.dev.vars` contains `OPEN_FOOD_FACTS_USER_AGENT=GymLedger/0.1 (contact@example.invalid)`.
   Use a clearly non-real example email. Do not commit a real address.

2. Apply local D1 migrations:
```bash
npx wrangler d1 migrations apply gymledger-food-lookup --local
```

3. Enable barcode lookup via local D1:
```bash
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('safe_mode', 'false', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('online_lookup_enabled', 'true', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('open_food_facts_provider_enabled', 'true', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('barcode_lookup_enabled', 'true', datetime('now'));"
```

4. Start the Worker:
```bash
npm run dev
```

5. Test with a known barcode:
```bash
curl -i http://localhost:8787/v1/foods/barcode/3017620422003
```

6. Test invalid barcodes:
```bash
curl -i http://localhost:8787/v1/foods/barcode/1234
curl -i http://localhost:8787/v1/foods/barcode/ABC123
```

7. Repeat the valid barcode to verify cache behavior (second call is served from cache).

8. Restore conservative defaults:
```bash
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('safe_mode', 'true', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('online_lookup_enabled', 'false', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('open_food_facts_provider_enabled', 'false', datetime('now'));"
npx wrangler d1 execute gymledger-food-lookup --local --command="INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES ('barcode_lookup_enabled', 'false', datetime('now'));"
```

## Runtime Configuration

| Key | Default | Description |
|-----|---------|-------------|
| safe_mode | true | Blocks all external provider calls |
| online_lookup_enabled | false | Master switch for online lookups |
| usda_provider_enabled | false | Enables USDA provider |
| open_food_facts_provider_enabled | false | Enables Open Food Facts provider |
| barcode_lookup_enabled | false | Enables barcode lookup feature |
| daily_external_call_budget | 25 | Max external calls per UTC day |
| cache_enabled | true | Enables D1 cache |
| cache_ttl_seconds | 86400 | Cache TTL (24 hours) |

## Secrets

Set keys in `.dev.vars` for local testing:

```
GYMLEDGER_API_KEY=test-key
USDA_API_KEY=your-usda-key
OPEN_FOOD_FACTS_USER_AGENT=GymLedger/0.1 (contact@example.invalid)
```

Use a clearly non-real example email for the User-Agent. Do not commit a real address.

For production, use Cloudflare Workers secrets:

```bash
npx wrangler secret put GYMLEDGER_API_KEY
npx wrangler secret put USDA_API_KEY
npx wrangler secret put OPEN_FOOD_FACTS_USER_AGENT
```

Enter values interactively when prompted. Never commit secret values.

Never commit `.dev.vars` or real API keys.

## Production Deployment

```bash
cd worker/food-lookup
npm ci
npm run typecheck
npm test
npx wrangler deploy --dry-run
npx wrangler deploy
```

Requires Wrangler authentication with the intended Cloudflare account.

Before deploying, verify:
- Cloudflare account identity: `npx wrangler whoami`
- D1 database exists: `npx wrangler d1 list`
- D1 database ID matches `wrangler.toml`
- Required secrets are configured

## Safe Runtime Defaults

Production must begin and end in a conservative state:

| Key | Safe Value | Description |
|-----|------------|-------------|
| safe_mode | true | Blocks all external provider calls |
| online_lookup_enabled | false | Master switch for online lookups |
| usda_provider_enabled | false | Enables USDA provider |
| open_food_facts_provider_enabled | false | Enables Open Food Facts provider |
| generic_food_search_enabled | false | Enables generic food search |
| barcode_lookup_enabled | false | Enables barcode lookup feature |
| daily_external_call_budget | 25 | Max external calls per UTC day |
| cache_enabled | true | Enables D1 cache |
| cache_ttl_seconds | 86400 | Cache TTL (24 hours) |

## Rollback

If production issues occur:
1. Set `safe_mode=true` via D1
2. Set `online_lookup_enabled=false`
3. Set all provider and feature flags to `false`
4. Redeploy the previous approved commit if needed
