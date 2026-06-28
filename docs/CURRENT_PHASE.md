# Phase 17D — Cloudflare Worker Foundation

## Objective

Create the TypeScript Cloudflare Worker foundation for GymLedger Food Lookup Gateway.

This phase creates backend infrastructure only.

It does not integrate external food providers yet.

It does not modify Android.

## Product Quality Goal

GymLedger should have a small, low-cost, serverless backend foundation that can later support online-assisted food lookup through safe, normalized, cacheable endpoints.

This phase should establish:

* local Worker development
* predictable endpoint structure
* stable JSON response shape
* stable error response shape
* simple API key middleware
* safe public config endpoint
* README instructions for local dev and deployment

## Recommended AI Route

* Preflight: MiMo V2.5, Devstral 6bit, or Qwen Coder 30B 5bit
* Builder: Qwen Coder 30B 5bit or OpenCode Go Qwen3.6 Plus
* Review: ChatGPT with GitHub connector
* Escalation: DeepSeek Pro only if TypeScript/Wrangler issues become confusing
* Codex: not needed unless repo/CI becomes broken

## Tasks

* Create `worker/food-lookup` TypeScript Cloudflare Worker project.
* Add package scripts for dev, typecheck, test, and deploy if applicable.
* Add Wrangler config.
* Add `GET /v1/health`.
* Add `GET /v1/config`.
* Add structured JSON success response helper.
* Add structured JSON error response helper.
* Add stable error codes.
* Add optional API key middleware using `X-GymLedger-Key`.
* Add README with setup, local dev, secrets, validation, and deploy notes.
* Keep cost target at $0/month for personal use.
* Use `workers.dev` as the default endpoint strategy.

## Do Not Do

* Do not modify Android app code.
* Do not modify Gradle.
* Do not add OkHttp.
* Do not add Retrofit.
* Do not call USDA.
* Do not call Open Food Facts.
* Do not add D1.
* Do not add KV.
* Do not add provider cache.
* Do not add barcode lookup.
* Do not add Android remote lookup integration.
* Do not store personal workouts, meals, body measurements, photos, API keys, or secrets.
* Do not require user accounts.
* Do not require custom domain.
* Do not add paid provider dependencies.
* Do not implement future backend phases.
* Do not commit; the user commits manually.

## Acceptance Criteria

* `worker/food-lookup` exists.
* Worker can run locally.
* `GET /v1/health` returns a stable success response.
* `GET /v1/config` returns safe public config only.
* Error responses use a stable shape.
* Missing/invalid API key behavior is defined and testable.
* No secrets are committed.
* No Android files are modified.
* No external provider calls exist.
* No D1/KV/cache exists yet.
* README explains local setup, validation, and deployment.
* Cost target remains $0/month.

## Validation Commands

```bash
cd worker/food-lookup
npm install
npm run typecheck
npm test
npm run dev
```

Manual curl validation:

```bash
curl http://localhost:8787/v1/health
curl http://localhost:8787/v1/config
curl -H "X-GymLedger-Key: test-key" http://localhost:8787/v1/health
```

## Quality Gates

```bash
git diff --name-status
git diff --stat
git diff --check
```

Android no-touch gate:

```bash
git diff -- app/src build.gradle.kts settings.gradle.kts gradle/libs.versions.toml
```

Provider no-call gate:

```bash
grep -R -nE "openfoodfacts|fdc\.nal\.usda|USDA|Open Food Facts" worker/food-lookup/src || true
```

Secrets gate:

```bash
grep -R -nE "api[_-]?key|secret|token|password" worker/food-lookup --exclude README.md || true
```

Expected: no real secrets. Placeholder names in env typings/config docs are acceptable.

## Manual QA Checklist

* Start Worker locally.
* Open `/v1/health`.
* Open `/v1/config`.
* Test unknown route.
* Test missing API key behavior if middleware is enabled for protected routes.
* Confirm safe config does not expose secrets.
* Confirm README commands work.
* Confirm no Android files changed.
* Confirm no provider URLs or calls exist.

## Suggested Commit

```text
feat: add food lookup worker foundation
```
