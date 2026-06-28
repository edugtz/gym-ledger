# GymLedger Food Lookup Worker

Cloudflare Worker foundation for GymLedger online-assisted food lookup.

Phase 17D only — foundation and safe public endpoints.

## Status

No USDA provider yet.

No Open Food Facts provider yet.

No D1/KV cache yet.

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
