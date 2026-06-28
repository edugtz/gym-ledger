# GymLedger — Backend / Cloud Phase Guide

## Purpose

This document defines backend/cloud phases for the online-assisted parts of GymLedger.

Backend is not a full SaaS. It is a low-cost serverless helper for lookup, cache, normalization, and optional future catalogs.

## Backend Principle

```text
Cloud helps discover data.
Room owns saved data.
```

No backend phase should store personal workouts, meals, body measurements, photos, or secrets unless a future phase explicitly approves it.

---

## Backend Phase B0 — Cloudflare Account and Strategy Setup

### Maps to roadmap

Before Phase 17D.

### Objective

Prepare Cloudflare account/project choices before code.

### Tasks

- Confirm Cloudflare account.
- Decide repo location: same repo under `worker/food-lookup` or separate repo.
- Decide endpoint strategy: workers.dev first.
- Decide API key strategy.
- Decide D1 database name.
- Decide local dev environment.
- Document secrets that must not be committed.

### Do Not Do

- Do not write Worker code.
- Do not call providers.
- Do not change Android app.

### Acceptance Criteria

- Account strategy is clear.
- Repo location is chosen.
- Cost target remains $0/month.
- No secrets are committed.

### Validation

```bash
git diff --check
```

### Suggested Commit

```text
docs: document cloud setup strategy
```

---

## Backend Phase B1 — Worker Foundation

### Maps to roadmap

Phase 17D.

### Objective

Create the TypeScript Cloudflare Worker skeleton.

### Tasks

- Create `worker/food-lookup` project.
- Add Wrangler config.
- Add `GET /v1/health`.
- Add `GET /v1/config`.
- Add structured JSON response helpers.
- Add stable error response shape.
- Add API key middleware using `X-GymLedger-Key`.
- Add README with local dev/deploy instructions.

### Do Not Do

- Do not call USDA.
- Do not call Open Food Facts.
- Do not add D1 provider cache unless explicitly approved.
- Do not modify Android.

### Acceptance Criteria

- Worker runs locally.
- Health endpoint returns OK.
- Config endpoint returns safe config.
- Missing/invalid key behavior is defined.
- No secrets in repo.

### Validation

```bash
npm install
npm run typecheck
npm test
npm run dev
curl http://localhost:8787/v1/health
curl http://localhost:8787/v1/config
```

### Suggested Commit

```text
feat: add food lookup worker foundation
```

---

## Backend Phase B2 — D1 Cache and Budget Foundation

### Maps to roadmap

Part of Phase 17E.

### Objective

Add structured cache and usage/budget tables.

### Tasks

- Add D1 database binding.
- Add migrations.
- Create `food_lookup_cache` table.
- Create `usage_daily` table.
- Create `runtime_config` table.
- Add cache read/write helpers.
- Add daily external call budget.
- Add safe-mode/kill-switch config.

### Do Not Do

- Do not call providers yet unless combined with B3/B4 in approved plan.
- Do not store personal data.

### Acceptance Criteria

- D1 migrations run locally/preview.
- Cache helpers can read/write.
- Budget logic can block external calls.

### Validation

```bash
npm run typecheck
npm test
wrangler d1 migrations apply <DB_NAME> --local
npm run dev
```

### Suggested Commit

```text
feat: add worker cache and budget foundation
```

---

## Backend Phase B3 — USDA Provider

### Maps to roadmap

Part of Phase 17E.

### Objective

Add generic food lookup through USDA FoodData Central.

### Tasks

- Add USDA provider client.
- Add generic search endpoint.
- Normalize nutrients into app DTO.
- Map calories/protein/carbs/fat per 100g.
- Add source attribution.
- Add provider timeout.
- Cache normalized result.
- Add tests for normalization.

### Do Not Do

- Do not expose raw USDA payload to Android.
- Do not add paid provider.
- Do not use USDA data as certainty; mark approximate.

### Acceptance Criteria

- `GET /v1/foods/generic?q=egg` returns normalized results.
- Results include attribution.
- Cache hit avoids provider call.
- Provider failures return stable error.

### Validation

```bash
npm run typecheck
npm test
npm run dev
curl "http://localhost:8787/v1/foods/generic?q=egg"
```

### Suggested Commit

```text
feat: add USDA food lookup provider
```

---

## Backend Phase B4 — Open Food Facts Provider

### Maps to roadmap

Part of Phase 17E.

### Objective

Add packaged food and barcode lookup through Open Food Facts.

### Tasks

- Add Open Food Facts provider client.
- Add barcode lookup.
- Add product search only if approved by plan.
- Normalize brand/name/barcode/serving/nutrients.
- Add source attribution.
- Add custom User-Agent.
- Add provider timeout.
- Cache normalized result.

### Do Not Do

- Do not use OFF search-as-you-type.
- Do not expose raw OFF payload to Android.
- Do not ignore attribution/licensing fields.

### Acceptance Criteria

- Barcode lookup returns normalized product when found.
- Unknown barcode returns stable not_found.
- Cache hit avoids provider call.
- Rate-limit friendly behavior documented.

### Validation

```bash
npm run typecheck
npm test
npm run dev
curl "http://localhost:8787/v1/foods/barcode/KNOWN_BARCODE"
```

### Suggested Commit

```text
feat: add Open Food Facts lookup provider
```

---

## Backend Phase B5 — Worker Deploy and Smoke Tests

### Maps to roadmap

End of Phase 17E.

### Objective

Deploy Worker safely and verify real endpoint behavior.

### Tasks

- Configure Cloudflare secrets.
- Deploy to workers.dev.
- Run health/config checks.
- Run USDA generic check.
- Run OFF barcode check.
- Verify cache hit behavior.
- Verify budget exceeded behavior.
- Document endpoint URL for Android settings.

### Do Not Do

- Do not add Android integration in this phase.
- Do not publish keys.

### Acceptance Criteria

- Deployed Worker responds.
- Auth/key behavior works.
- Provider endpoints work within limits.
- Endpoint URL documented locally.

### Validation

```bash
wrangler deploy
curl https://<worker>.workers.dev/v1/health
curl https://<worker>.workers.dev/v1/config
```

### Suggested Commit

```text
chore: deploy food lookup worker
```

---

## Backend Phase B6 — Android Integration Contract Support

### Maps to roadmap

Phase 17F.

### Objective

Provide final DTO/error contract for Android integration.

### Tasks

- Freeze DTO fields.
- Freeze error codes.
- Provide example responses.
- Provide offline/timeout behavior.
- Document source badges.
- Document caching expectations.

### Do Not Do

- Do not change providers unless bug discovered.
- Do not store personal data.

### Acceptance Criteria

- Android can implement against stable contract.
- Error handling is predictable.

### Validation

```bash
npm run typecheck
npm test
```

### Suggested Commit

```text
docs: finalize food lookup API contract
```
