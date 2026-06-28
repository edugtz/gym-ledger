# GymLedger — MVP Review and Required Adjustments

## Purpose

This document captures roadmap corrections needed to make GymLedger useful enough for personal premium-like use while staying low-cost.

## Updated Cost Policy

GymLedger should target $0/month for two users.

Allowed when active phase approves:

- Cloudflare Worker
- Cloudflare D1/KV
- USDA FoodData Central
- Open Food Facts
- optional online lookup

Still avoid:

- paid runtime APIs as required core dependency
- VPS / always-on backend
- required login
- required cloud sync

## Roadmap Corrections

### 1. Move smart nutrition earlier

Food CRUD alone is not premium. Nutrition needs:

- local reference foods
- quantity calculator
- calculated macros
- recents/favorites
- optional online lookup
- manual barcode lookup

These move before the original Nutrition Repository/UI phases.

### 2. Add online-assisted platform docs before code

Add Phase 17A docs update.

### 3. Add settings foundation before network

Add Phase 17B so endpoint/API key/toggles are not hardcoded.

### 4. Add local smart food entry before backend

Add Phase 17C so common foods work without internet.

### 5. Add Worker backend in small phases

Add:

- 17D Worker foundation
- 17E providers/cache
- 17F Android integration

### 6. Keep Android source of truth local

Remote data must be cached/reviewed/edited before saving.

### 7. Update v1.5/v2 split

Move basic food lookup/barcode earlier.

Keep v2 for:

- camera barcode scanner polish
- imported large food DB
- optional AI adapter
- advanced reports
- encrypted backup
