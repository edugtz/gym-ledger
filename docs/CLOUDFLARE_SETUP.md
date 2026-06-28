# GymLedger — Cloudflare Setup

## Status

Cloudflare deployment verified.

## Routing

workers.dev subdomain: eduardo-gutierrez-2325.workers.dev
Worker name: gymledger-food-lookup
Worker URL: https://gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev

## Verified Endpoints

GET /v1/health -> 200 OK
GET /v1/config -> 200 OK
GET /unknown -> 404 not_found
POST /v1/health -> 405 method_not_allowed

## Current Capabilities

Health endpoint: yes
Safe config endpoint: yes
Stable success response: yes
Stable error response: yes
API key helper: present
Protected endpoints: no
USDA provider: no
Open Food Facts provider: no
D1 cache: no
KV: no
Android integration: no
Personal data storage: no

## Secrets

No production secrets are configured for this phase.

Do not commit:
- .dev.vars
- .env
- .env.*
- Cloudflare API tokens
- USDA API key
- Open Food Facts credentials
- personal API keys

Future protected endpoints may use:
- GYMLEDGER_API_KEY
- X-GymLedger-Key

## Commands

Local validation from worker/food-lookup:

npm run typecheck
npm test

Deploy from worker/food-lookup:

npm run deploy

## Guardrails

Do not add the following before an approved future phase:
- D1
- KV
- USDA provider
- Open Food Facts provider
- Android remote lookup integration
- barcode lookup
- cloud sync
- user accounts
- personal data storage
- paid provider dependencies
