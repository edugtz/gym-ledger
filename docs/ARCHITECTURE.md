# GymLedger — Architecture

## Architecture Goal

Use simple, maintainable architecture.

Avoid overengineering.

Android v1 architecture remains:

```text
Compose UI
  ↓
ViewModel
  ↓
Repository
  ↓
Room / DataStore / File IO
```

Online-assisted features add optional remote sources, but do not replace the local source of truth.

## Source-of-Truth Rules

1. `docs/CURRENT_PHASE.md` is the active scope.
2. Actual Kotlin source files are the source of truth for exact fields/methods/routes.
3. If docs conflict with code, report before editing.
4. Room/local data is the user source of truth.
5. Remote data is a suggestion and must be editable before saving.
6. Do not add future-phase fields/screens unless active phase requires them.

## Android Module Structure

Use a single Android module in v1:

```text
:app
```

Do not use Android multi-module architecture in v1.

## Package

Use package:

```text
com.edu.gymledger
```

Never create or reference:

```text
com.gymledger
```

## Dependency Injection

Do not use Hilt in v1.

Use manual dependency injection through `AppContainer`.

Repositories should be wired from Room DAOs, DataStore, File IO, and approved remote sources only when their active phase requires it.

## Navigation

Use Navigation Compose.

`NavigationRoute.kt` and `AppNavigation.kt` are source of truth for route names and route parameters.

Do not add routes unless active phase explicitly requires them.

## Data Storage

Use Room for structured data.

Use DataStore Preferences for settings.

Use app-specific storage and Storage Access Framework when import/export/photo phases are active.

## Online-Assisted Architecture

When an active phase requires online lookup:

```text
Compose UI
  ↓
ViewModel
  ↓
Repository
  ↓
Local Room Cache
  ↓
Remote Source
  ↓
Cloudflare Worker
  ↓
External Provider
```

Preferred Android packages, introduced only when needed:

```text
data/remote
data/remote/dto
data/remote/source
data/repository/lookup
domain/model/lookup
```

## Network Client

Preferred when remote lookup phase is active:

```text
OkHttp + Kotlinx Serialization
```

Do not use Retrofit in v1 unless explicitly approved later.

## Backend Architecture

Approved backend pattern:

```text
Cloudflare Worker TypeScript gateway
```

Responsibilities:

- normalize external provider data
- cache external results
- protect provider rate limits
- expose stable DTOs
- track usage budget
- return friendly error codes

The Worker should not store personal workout/meal/body data initially.

## External Provider Policy

Preferred providers:

- USDA FoodData Central for generic foods
- Open Food Facts for products/barcodes

Avoid paid providers initially.

## Secrets

Do not hardcode API keys in Android.

Personal API key should be user-entered in Settings or stored as local dev secret for Worker testing.

Cloudflare secrets belong in Cloudflare environment variables, not source code.
