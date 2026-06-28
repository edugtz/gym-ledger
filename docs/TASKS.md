# GymLedger — Master Roadmap

This file is the roadmap context. It is not the active implementation scope.

Active implementation scope is always:

```text
docs/CURRENT_PHASE.md
```

## Global Strategy

GymLedger is:

- local-first
- offline-capable
- online-assisted optional
- personal-use optimized for 2 users
- low-cost/free-tier friendly

Room/local data remains source of truth.

## MVP Roadmap Summary

MVP includes:

- app shell/navigation
- Room foundation
- exercises
- workouts
- routines
- body measurements
- foods
- smart food entry
- optional online food lookup
- manual barcode lookup
- food recents/favorites
- meals and daily nutrition
- settings
- dashboard
- import/export
- meal photos
- editable approximate estimates
- MVP hardening

## Detailed MVP Phases

See:

```text
docs/MVP_PHASES.md
```

Key immediate sequence after Phase 17:

```text
17B Settings Foundation for Online Assistance
17C Smart Food Entry Local Foundation
17D Cloudflare Worker Foundation
17E Worker Food Providers and Cache
17F Android Remote Food Lookup Integration
17G Manual Barcode Lookup
17H Food Recents and Favorites
18 Nutrition Repository
19 Nutrition Day UI
20 Meal Detail and Items UI
```

## Backend / Cloud Roadmap

See:

```text
docs/BACKEND_CLOUD_PHASES.md
```

Backend phases:

```text
B0 Cloudflare account/setup strategy
B1 Worker foundation
B2 D1 cache and budget foundation
B3 USDA provider
B4 Open Food Facts provider
B5 deploy/smoke tests
B6 Android integration contract support
```

## v1.5 Roadmap

See:

```text
docs/GYMLEDGER_V1_5_PHASES.md
```

v1.5 focuses on:

- workout speed
- copy previous workout
- rest timers
- exercise history
- PR/1RM
- dashboard insights
- meal templates
- copy meals/day
- body trends
- polish/hardening

## v2 Roadmap

See:

```text
docs/GYMLEDGER_V2_PHASES.md
```

v2 focuses on:

- Health Connect
- camera barcode scanner polish
- user-imported food DB
- advanced food search
- progress photos
- optional BYO AI adapter
- progressive overload
- reminders
- encrypted backup
- reports
- release hardening
