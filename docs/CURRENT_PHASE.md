## Phase 17F — Android Remote Food Lookup Integration

### Phase Type

Android platform and application integration (online-assisted). Not a backend phase.

### Objective

Connect Android to the deployed GymLedger Food Lookup Worker for **generic food search only**, while preserving offline/manual behavior. Remote results are optional, approximate, editable suggestions that become local `Food` rows only after explicit user confirmation.

### User-Visible Outcome

- When online assistance is enabled and configured, a user can search the Worker for generic foods from the existing Smart Food Entry sheet.
- Results show source and approximate badges.
- Selecting a result prefills the existing editable quantity + nutrition fields.
- Saving creates a normal local `Food`; nothing is saved automatically.
- With online assistance off, unconfigured, offline, disabled by Worker config, or on error, the existing manual and local reference flows work unchanged.

### In Scope

- App-owned DTOs for Worker `/v1/config` and `/v1/foods/generic?q=`.
- JSON decoding with existing `kotlinx-serialization-json` (strict: `ignoreUnknownKeys=true`, `isLenient=false`).
- HTTP client abstraction + OkHttp implementation behind a `okhttp3.Call.Factory` seam.
- Cancellation-aware OkHttp bridge (`Call.enqueue` + `suspendCancellableCoroutine`).
- Explicit `X-GymLedger-Key` header handling; key never in URL/logs/DTOs.
- Timeout (~5s connect/read/write) and transport-error mapping to user-safe outcomes.
- Custom endpoint validation (absolute HTTPS, no userinfo/query/fragment, normalized trailing slash).
- Blank endpoint → default production Worker URL; blank key → remote not configured.
- `RemoteFoodLookupRepository` orchestration using `SettingsRepository` endpoint/key.
- Lazy `/v1/config` fetch (only when entering Online search) with 5-minute in-memory cache via an injectable monotonic time source and conservative fallback.
- Generic-search flow integrated into the existing `SmartFoodEntrySheet` as an optional "Online search" mode.
- Manual search submission (IME Search / button); no per-keystroke network calls; duplicate-concurrent prevention.
- User confirmation before local save (existing `FoodRepository.create` path).
- Nullable nutrition contract: DTO nutrients nullable; filter incomplete/negative/non-finite results; round valid calories to Int; map to `FoodReference` without changing its schema.
- Source and approximate attribution display (reuse existing chips).
- Graceful offline / disabled / missing-key / unconfigured / invalid-endpoint / error states shown inline.
- Unit tests with fake `Call.Factory` / `FoodLookupClient` (no real network).
- Manual runtime QA.
- Settings helper-text consistency: resolving the contradiction between "Leave blank to use default" and the old "endpoint must be entered" helper.

### Explicitly Out of Scope

- Barcode lookup UI or scanning.
- CameraX.
- Recents and favorites.
- Auto-saving provider results.
- Caching provider results in Room (no Room lookup cache this phase).
- Cloud accounts.
- Cloud sync.
- Backend / Worker code changes or deployment changes.
- Worker migrations.
- Analytics or telemetry.
- Paid providers.
- Broad Nutrition redesign.
- Food schema changes.
- New navigation routes.
- `openFoodFactsEnabled` usage (reserved for Phase 17G).

### Architecture Boundaries

- Single `:app` module, package `com.edu.gymledger`. Never `com.gymledger`.
- Manual DI via `AppContainer`. No Hilt. No Retrofit. No multi-module.
- Android calls the GymLedger Worker only; never external providers directly.
- Remote results are suggestions; Room/local `FoodRepository` remains source of truth.
- New packages introduced only when needed: `data/remote`, `data/remote/dto`, `data/repository/lookup`, `domain/model/lookup` (per `docs/ARCHITECTURE.md`).
- No Room schema change. No DB version bump. No new entity.

### Security and Secret-Handling Rules

- The API key is user-entered in Settings and stored in DataStore on-device only.
- Do not put the key in Kotlin source, XML resources, docs, tests, committed `gradle.properties`, BuildConfig snapshots, or logs.
- The key is sent only as the `X-GymLedger-Key` request header, never as a query parameter or in the URL.
- The key is sent only to the resolved and validated origin (default production Worker URL or a user-entered valid custom HTTPS endpoint).
- Error/log messages and DTOs must not include the key.
- Build behavior with no configured key: remote lookup is unavailable; manual/local flows unaffected.
- The Worker base URL is public (documented in `docs/FOOD_LOOKUP_DEPLOYMENT.md`) and may be a default constant; it is not a secret.

### Offline-First Behavior

- Existing local food creation, editing, search, and smart (reference) entry continue working with no internet.
- Opening or using Nutrition/Foods must not require the Worker.
- No config fetch at application startup. No config fetch while "Local reference" mode is active.
- Network failure, timeout, missing key, invalid endpoint, 503 disabled states, or config-fetch failure leave manual/local entry usable and show an inline user-safe state.
- A remote result becomes local only after explicit Save.

### Endpoint Semantics

- `foodLookupEndpoint` blank → use the default production Worker URL constant (`https://gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev`). A custom endpoint is not required when the default is used.
- `foodLookupApiKey` blank → remote lookup is not configured (unavailable); no network call.
- A non-blank custom endpoint must be validated: absolute HTTPS URL, no embedded username/password (userinfo), no query, no fragment, and a normalized trailing slash. An invalid custom endpoint makes remote lookup unavailable with an inline user-safe message.
- The API key is sent only to the resolved validated origin.

### Complete Generic-Search Gating

Local gates (must all hold before any network call):

- `onlineFoodLookupEnabled = true`
- `usdaEnabled = true`
- `safeModeEnabled = false`
- API key non-blank
- endpoint blank (→ default URL) or valid custom HTTPS endpoint

Remote config gates (from cached `/v1/config`, must all hold):

- `onlineLookupAvailable = true`
- `providers.usda = true`
- `features.genericFoodSearch = true`
- `safeMode = false`
- query length ≥ `minQueryLength`

`openFoodFactsEnabled` is explicitly unused in Phase 17F and remains for Phase 17G.

### Nullable Nutrition Contract

The actual Worker contract allows `null` for `caloriesKcal`, `proteinG`, `carbohydrateG`, and `fatG`. `FoodReference` does not allow nullable nutrition and must not be modified.

Policy:

- DTO nutrient fields remain nullable.
- Do not coerce `null` to zero.
- Filter any result that has a missing, negative, or non-finite nutrient (calories, protein, carbs, fat).
- Convert valid calories to `Int` using an explicitly documented rounding rule (round to nearest, 0.5 rounds up, then require ≥ 0).
- If all provider results are filtered out, return `Empty`.
- Do not modify `FoodReference` or the Room schema.

### Online Mode Visibility

- The "Online search" toggle is absent only when `onlineFoodLookupEnabled = false`.
- When the user has enabled online assistance, "Online search" remains visible even if the Worker is temporarily disabled or a gate fails.
- Missing key, USDA disabled, local safe mode, invalid endpoint, remote safe mode, remote provider disabled, or remote feature disabled must show an inline user-safe state inside the Online search area.
- Do not silently remove the mode because the Worker is temporarily disabled.
- Manual/local entry always remains usable.

### Config-Fetch Timing

Exact flow:

- No config fetch at application startup.
- No config fetch while "Local reference" mode is active.
- Fetch config when entering "Online search" for the first time.
- Cache in memory for 5 minutes.
- Re-fetch when stale.
- Conservative unavailable state on failure (no config call per keystroke).
- Use an injectable monotonic time source for TTL tests.

### Search Trigger

Phase 17F uses manual remote submission:

- `onValueChange` only updates the query.
- IME Search or a "Search online" button starts the request.
- No Worker call per keystroke.
- No debounce is required because submission is explicit.
- Prevent duplicate concurrent submissions.
- Cancellation/reset behavior: leaving Online search mode or dismissing the sheet cancels an in-flight search coroutine; a new submission while one is in flight is ignored (no concurrent duplicate).

### Files/Layers Allowed

- `gradle/libs.versions.toml`, `app/build.gradle.kts` — add OkHttp dependency.
- `app/src/main/AndroidManifest.xml` — add `INTERNET` permission.
- `app/src/main/java/com/edu/gymledger/app/AppContainer.kt` — wire shared `OkHttpClient` + `RemoteFoodLookupRepository` + monotonic time source.
- `app/src/main/java/com/edu/gymledger/data/remote/**` — client, DTOs, errors, parser, `Call.Factory` seam, cancellation bridge, endpoint validation.
- `app/src/main/java/com/edu/gymledger/data/repository/lookup/RemoteFoodLookupRepository.kt`.
- `app/src/main/java/com/edu/gymledger/domain/model/lookup/**` — remote result domain model + mapper (nullable-aware filtering + calorie rounding).
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntry*.kt` — online mode integration.
- `app/src/main/java/com/edu/gymledger/feature/settings/SettingsViewModel.kt` — endpoint validation helper exposure if needed.
- `app/src/main/java/com/edu/gymledger/feature/settings/SettingsScreen.kt` — only if helper text must change to resolve the "Leave blank to use default" contradiction.
- `app/src/test/java/com/edu/gymledger/**` — new unit tests.
- `docs/CURRENT_PHASE.md`, `docs/IMPLEMENTATION_PLAN.md` — this replacement.

### Files/Layers Forbidden

- `worker/**`.
- `app/src/main/java/com/edu/gymledger/data/db/entity/FoodEntity.kt`.
- `app/src/main/java/com/edu/gymledger/data/db/dao/FoodDao.kt`.
- `app/src/main/java/com/edu/gymledger/data/db/GymLedgerDatabase.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/SettingsRepository.kt` (read-only consumer; no DataStore key changes).
- `app/src/main/java/com/edu/gymledger/data/repository/OnlineAssistanceSettings.kt`.
- `app/src/main/java/com/edu/gymledger/domain/model/FoodReference.kt` and `FoodReferenceCalculator.kt`.
- `app/src/main/java/com/edu/gymledger/navigation/**`.
- All features other than `feature/nutrition` and `feature/settings`.
- Barcode/product lookup work.
- `app/src/main/java/com/gymledger/**` (empty legacy dirs).

### Cache Decision

- No Room lookup cache in Phase 17F.
- Unconfirmed suggestions are ephemeral (in-memory only).
- Worker D1 caches provider responses.
- Confirmed results become normal local `Food` rows via `FoodRepository.create`.
- The remote domain model remains compatible with a future local lookup cache.
- No fake or hidden Room cache is implemented now.

### Acceptance Criteria

- Generic remote search works only when all local gates and all remote config gates hold, and query length ≥ server `minQueryLength`.
- Blank endpoint uses the default Worker URL; a custom endpoint must pass validation or remote is unavailable; no custom endpoint is required to use the default.
- Blank API key means remote is not configured; no network call occurs.
- `usdaEnabled=true` is required locally; `providers.usda=true` is required remotely.
- `openFoodFactsEnabled` is not used this phase.
- Disabled/unconfigured/offline/invalid-endpoint/error states preserve manual and local reference entry.
- The "Online search" toggle is visible whenever `onlineFoodLookupEnabled=true`; failures show inline user-safe states and do not silently remove the mode.
- Selecting a remote result prefills editable fields; Save creates a normal local `Food`.
- Source and approximate badges are shown for remote results.
- Remote results with missing/negative/non-finite nutrients are filtered; if all are filtered, `Empty` is returned; `null` is never coerced to zero.
- No product/barcode lookup UI introduced.
- No Room schema change.
- No backend/Worker changes.
- Setting helper text is consistent with endpoint default semantics.
- Validation passes.
- Scope is clean; no unrelated files changed.

### Required Tests

Transport/client (`OkHttpFoodLookupClientTest` via fake `Call.Factory`):
- Correct route + URL encoding for `/v1/foods/generic`.
- `X-GymLedger-Key` header present; key not in URL; key not in DTOs or error strings.
- Config route `/v1/config` has no key header.
- Success decoding including `nutritionPer100g` with complete nutrients.
- Empty results list → `Empty`.
- Each HTTP/error mapping: 400 `invalid_query`, 401 `unauthorized`, 429 `budget_exceeded`, 503 `lookup_disabled`/`provider_disabled`/`feature_disabled`/`configuration_error`, body `provider_error`.
- Malformed body → `MalformedResponse`.
- Cancellation-aware bridge cancels the OkHttp `Call` when the coroutine is cancelled.

Config:
- Conservative fallback when config fetch fails or body malformed.
- Enabled state (`onlineLookupAvailable && providers.usda && features.genericFoodSearch && !safeMode`) allows search.
- `safeMode=true` suppresses search.
- `features.genericFoodSearch=false` suppresses search.
- `providers.usda=false` suppresses search.
- `minQueryLength` advertises and gates query length.
- In-memory cache reuses fresh config and re-fetches when stale (injectable monotonic time source).

Repository / product:
- `onlineFoodLookupEnabled=false` → no remote call.
- Missing API key → remote unavailable, no call.
- `usdaEnabled=false` → remote unavailable, no call.
- `safeModeEnabled=true` → remote unavailable, no call.
- Invalid custom endpoint → remote unavailable, no call.
- Too-short query → no remote call.
- Endpoint blank → default URL used.
- Endpoint non-blank invalid → unavailable.
- Enabled valid query → loading/success/empty/error states.
- Selecting a remote result prefills the editable fields.
- Result remains editable; no automatic local save.
- Manual flow remains available after remote failure.
- Duplicate concurrent submission prevented.

Nullable nutrition:
- Complete nutrients decode and map.
- Incomplete (null) nutrient → result filtered.
- Mixed complete/incomplete → only complete results returned.
- Negative nutrient → filtered.
- Non-finite nutrient → filtered.
- All filtered → `Empty`.
- Valid calories rounded to `Int` per documented rule.

ViewModel:
- Online setting disabled → toggle absent, no remote call.
- Online setting enabled, missing key → toggle visible, inline "not configured" state, no call.
- Online setting enabled, `usdaEnabled=false` → inline state, no call.
- Online setting enabled, `safeModeEnabled=true` → inline state, no call.
- Online setting enabled, invalid endpoint → inline state, no call.
- Online setting enabled, valid, remote disabled → inline "temporarily disabled" state, manual usable.
- Too-short query → no remote call.
- Valid query → loading/success/empty/error states.
- Selecting a remote result prefills editable fields.
- Editable fields remain editable; no save until explicit call.
- Manual flow available after remote failure.
- Leaving Online search mode or dismissing sheet cancels in-flight search.
- Key value never present in UI state or error strings.

Security:
- No key in error strings, DTOs, or URL.
- No real network access in unit tests.

Settings (if modified):
- Helper text consistent with "Leave blank to use default."
- Custom endpoint validation enforces HTTPS, no userinfo/query/fragment, trailing slash.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

Targeted unit tests if supported:

```bash
./gradlew testDebugUnitTest
```

Secret gate (from repo root, no committed key value):

```bash
git grep -nE "X-GymLedger-Key:" -- app/src || true
git grep -n "foodLookupApiKey" -- app/src/main app/src/test || true
```

No real key value should appear. The DataStore key name `food_lookup_api_key` is expected; no literal secret value.

Scope gate:

```bash
git diff --name-status
git diff --stat
git diff -- worker/ || true
git diff -- app/src/main/java/com/edu/gymledger/data/db || true
```

`worker/` and DB diffs must be empty.

### Manual QA

- Online assistance setting OFF: remote toggle absent; manual + smart entry work.
- Online assistance ON, no key: Online search visible with inline "not configured"; manual entry works.
- Online assistance ON, key present, `usdaEnabled=false`: Online search visible with inline "temporarily unavailable"; manual entry works.
- Online assistance ON, key present, `safeModeEnabled=true`: Online search visible with inline "temporarily unavailable"; manual entry works.
- Online assistance ON, key present, invalid custom endpoint: Online search visible with inline "invalid endpoint"; manual entry works.
- Online assistance ON, key present, endpoint blank (default URL), Worker in conservative/safe mode (current production): Online search visible; search reports "temporarily disabled"; manual entry works.
- Worker temporarily enabled for controlled testing (optional, only if user explicitly approves): successful USDA generic result prefills fields; each result shows source + approximate.
- Edit-before-save: change values then Save; verify local `Food` created with edited values.
- Persistence only after Save: discarding a remote suggestion never creates a `Food`.
- App restart: settings (endpoint/key/toggles) persist; no remote call at startup.
- Manual food entry unaffected by remote flow.
- Rotation/navigation preserve the Smart Entry sheet state where supported by existing behavior.
- No barcode controls present.
- Airplane mode: remote search fails gracefully; manual + local reference entry usable.

### Stop Conditions

Stop immediately when:
- Phase 17E.4 is absent from the active branch history.
- Repository is unexpectedly dirty.
- Build/test/lint fails after the first real error (do not loop beyond two local attempts; escalate per `docs/AI_WORKFLOW.md`).
- A real API key value is detected in source/tests/docs/logs.
- Worker code is modified.
- Room schema/DB version changes.
- Barcode UI is introduced.
- Remote results auto-save without confirmation.
- Adding a dependency beyond OkHttp becomes necessary.
- `FoodReference` or `FoodReferenceCalculator` is modified.
- `isLenient=true` is enabled in JSON parsing.

### Suggested Commit

```text
feat: add online food lookup
```

### Phase-Completion Definition

- All acceptance criteria met.
- `./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug` passes.
- No `worker/` diff.
- No Room schema change.
- Manual QA completed and recorded.
- Reviewer returns PASS or PASS_WITH_NOTES.
- Branch `17f-plan` ready for ChatGPT GitHub review before merge to `dev`.

---
## Phase 17F Completion Record

### Status

**COMPLETE**

Completion date: 2026-08-23

Implementation branch:

`17f-android-remote-lookup`

### Final Implementation Summary

Phase 17F connected the Android application to the deployed GymLedger Food Lookup Worker for optional generic USDA food search while preserving GymLedger's local-first and offline-capable behavior.

Final Android flow:

`SmartFoodEntrySheet`
→ `SmartFoodEntryViewModel`
→ `RemoteFoodLookupRepository`
→ `FoodLookupClient`
→ `OkHttpFoodLookupClient`
→ GymLedger Cloudflare Worker
→ USDA FoodData Central

Implemented behavior includes:

- optional Online search mode inside Smart Food Entry;
- blank endpoint using the production Worker default;
- HTTPS validation for custom endpoints;
- user-entered `X-GymLedger-Key`;
- local and remote availability gates;
- lazy `/v1/config` loading;
- 5-minute monotonic in-memory config cache;
- manual remote submission through IME Search or Search online;
- cancellation-aware OkHttp requests;
- strict JSON decoding;
- source and Approximate attribution;
- nullable/invalid nutrition filtering;
- remote result selection into the existing editable food flow;
- quantity-based calorie and macro recalculation;
- explicit local Save only;
- no automatic persistence of remote suggestions;
- selected-food vertical scrolling and IME-safe access to all editable fields/actions;
- scroll reset when changing or re-selecting a reference food.

No barcode lookup UI was introduced.

### Automated Validation

Final validation:

```text
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
BUILD SUCCESSFUL
```

Test result:

```text
289 tests
0 failures
0 errors
```

Additional validation:

```text
git diff --check
PASS
```

No real API key or secret was committed.

### Technical Review

ChatGPT GitHub technical review:

**PASS**

Final implementation review confirmed:

- Android-only Phase 17F scope;
- Worker contract alignment;
- endpoint validation;
- local and remote gating;
- safe secret handling;
- strict response parsing;
- cancellation behavior;
- config caching;
- nullable nutrition filtering;
- explicit-save semantics;
- offline-first behavior;
- no Room schema changes;
- no Worker changes;
- no barcode implementation;
- no navigation changes;
- no `FoodReference` / `FoodReferenceCalculator` changes;
- final Smart Food Entry scrolling and reselection reset behavior.

### Manual UI QA

Manual UI QA completed on 2026-08-23.

PASS:

- Online-disabled local flow.
- Missing API key gate.
- USDA-disabled local gate.
- Local safe-mode gate.
- Invalid custom endpoint.
- Default endpoint behavior.
- Remote config loading state.
- Config cancellation behavior.
- Smart Entry sheet dismissal/cancellation behavior.
- Valid-query pre-submit state.
- IME Search request submission.
- Search online button.
- Live USDA generic lookup through the production Worker.
- USDA source attribution.
- Approximate attribution.
- Stale remote results clear when the query changes.
- Remote-result selection.
- Editable quantity and nutrition fields.
- Calories, protein, carbs, and fat recalculate with quantity changes.
- Explicit Save creates a normal local Food.
- Discarding a remote suggestion does not auto-save it.
- Saved Food persists after app restart / force-stop.
- Saved Food remains available offline.
- Local reference flow remains usable.
- Selected-food form is vertically scrollable.
- Bottom actions remain reachable.
- Change returns to reference search.
- Selecting another reference after Change resets the selected-food scroll position.
- Re-selecting a reference starts from the top.
- No barcode UI introduced.

Overall manual QA:

**PASS**

### Production Worker Restoration

The Worker was temporarily enabled only for controlled Phase 17F integration QA.

After QA, production runtime configuration was restored to its conservative default state.

Verified final behavior:

- `safeMode = true`
- `onlineLookupAvailable = false`
- USDA provider disabled
- Open Food Facts provider disabled
- generic food search disabled
- barcode lookup disabled

Runtime overrides used for controlled QA were removed after verification.

### Scope Verification

Confirmed no Phase 17F implementation changes to:

- `worker/**`
- Room entities / DAO / database schema
- `FoodRepository`
- `SettingsRepository`
- `OnlineAssistanceSettings`
- `FoodReference`
- `FoodReferenceCalculator`
- navigation
- barcode/scanning functionality

OkHttp was the only new runtime dependency required by the phase.

### Known Unrelated Issue

During manual QA, an existing Foods-screen search-input issue was discovered:

- rapid typing can cause cursor jumps / reordered characters in `Search foods`;
- the behavior was confirmed to already exist on `dev`;
- it was not introduced by Phase 17F;
- it is intentionally excluded from this phase and will be fixed in a dedicated post-17F bugfix.

### Final Verdict

**PASS — PHASE 17F COMPLETE**

All Phase 17F acceptance criteria, automated validation, technical review, manual runtime QA, live Worker integration, offline behavior, explicit-save semantics, scope gates, and production restoration requirements are complete.

Next planned roadmap phase after the dedicated Foods search bugfix and workflow-document refresh:

**Phase 17G — Manual Barcode Lookup**