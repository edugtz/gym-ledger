# GymLedger Food Lookup Worker

Cloudflare Worker foundation for GymLedger online-assisted food lookup.

Phase 17E.1 — D1 Cache and Budget Foundation.

## Status

No USDA provider yet.

No Open Food Facts provider yet.

D1 cache and budget foundation implemented.

No personal data storage.

No Android integration yet.

No secrets should be committed.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /v1/health | Health check |
| GET | /v1/config | Safe public config |

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
curl -i http://localhost:8787/unknown
curl -i -X POST http://localhost:8787/v1/health
```

## Secrets

Set `GYMLEDGER_API_KEY` in `.dev.vars` for local testing:

```
GYMLEDGER_API_KEY=test-key
```

For production, use Cloudflare Workers secrets:

```bash
wrangler secret put GYMLEDGER_API_KEY
```

Never commit `.dev.vars` or real API keys.

## Deploy

```bash
npm run deploy
```

Requires Wrangler authentication with a Cloudflare account.

## Cost

$0/month for personal use on the free tier.
