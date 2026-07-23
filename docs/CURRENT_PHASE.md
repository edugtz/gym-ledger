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
- JSON decoding with existing `kotlinx-serialization-json`.
- HTTP client abstraction + OkHttp implementation.
- Explicit `X-GymLedger-Key` header handling; key never in URL/logs/DTOs.
- Timeout and transport-error mapping to user-safe outcomes.
- `RemoteFoodLookupRepository` orchestration using `SettingsRepository` endpoint/key.
- Lazy `/v1/config` fetch with short-lived in-memory cache and conservative fallback.
- Generic-search flow integrated into the existing `SmartFoodEntrySheet` as an optional "Online search" mode.
- User confirmation before local save (existing `FoodRepository.create` path).
- Source and approximate attribution display (reuse existing chips).
- Graceful offline / disabled / missing-key / unconfigured / error states.
- Unit tests with fake transport/client (no real network).
- Manual runtime QA.

### Explicitly Out of Scope

- Barcode lookup UI or scanning.
- CameraX.
- Recents and favorites.
- Auto-saving provider results.
- Cloud accounts.
- Cloud sync.
- Backend / Worker code changes or deployment changes.
- Worker migrations.
- Analytics or telemetry.
- Paid providers.
- Caching provider results in Room.
- Broad Nutrition redesign.
- Food schema changes.
- New navigation routes.

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
- Error/log messages and DTOs must not include the key.
- Build behavior with no configured key: remote lookup is unavailable; manual/local flows unaffected.
- The Worker base URL is public (documented in `docs/FOOD_LOOKUP_DEPLOYMENT.md`) and may be a default constant; it is not a secret.

### Offline-First Behavior

- Existing local food creation, editing, search, and smart (reference) entry continue working with no internet.
- Opening or using Nutrition/Foods must not require the Worker.
- Network failure, timeout, missing key, 503 disabled states, or config-fetch failure leave manual/local entry usable.
- A remote result becomes local only after explicit Save.

### Files/Layers Allowed

- `gradle/libs.versions.toml`, `app/build.gradle.kts` — add OkHttp dependency.
- `app/src/main/AndroidManifest.xml` — add `INTERNET` permission.
- `app/src/main/java/com/edu/gymledger/app/AppContainer.kt` — wire OkHttp client + `RemoteFoodLookupRepository`.
- `app/src/main/java/com/edu/gymledger/data/remote/**` — client, DTOs, errors, parser.
- `app/src/main/java/com/edu/gymledger/data/repository/lookup/RemoteFoodLookupRepository.kt`.
- `app/src/main/java/com/edu/gymledger/domain/model/lookup/**` — remote result domain model + mapper.
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntry*.kt` — online mode integration.
- `app/src/test/java/com/edu/gymledger/**` — new unit tests.
- `docs/CURRENT_PHASE.md`, `docs/IMPLEMENTATION_PLAN.md` — this replacement.

### Files/Layers Forbidden

- `worker/**`.
- `app/src/main/java/com/edu/gymledger/data/db/entity/FoodEntity.kt`.
- `app/src/main/java/com/edu/gymledger/data/db/dao/FoodDao.kt`.
- `app/src/main/java/com/edu/gymledger/data/db/GymLedgerDatabase.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/SettingsRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/OnlineAssistanceSettings.kt`.
- `app/src/main/java/com/edu/gymledger/navigation/**`.
- All features other than `feature/nutrition` (and `feature/settings` is not modified).
- `app/src/main/java/com/gymledger/**` (empty legacy dirs).

### Acceptance Criteria

- Generic remote search works only when: online assistance enabled, endpoint/key configured, Worker config says `onlineLookupAvailable && features.genericFoodSearch && !safeMode`, and query length ≥ server `minQueryLength`.
- Disabled/unconfigured/offline/error states all preserve manual and local reference entry.
- Selecting a remote result prefills editable fields; Save creates a normal local `Food`.
- Source and approximate badges are shown for remote results.
- No product/barcode lookup UI introduced.
- No Room schema change.
- No backend/Worker changes.
- Validation passes.
- Scope is clean; no unrelated files changed.

### Required Tests

Transport/client (fakes, no network):
- Correct route + URL encoding for `/v1/foods/generic`.
- `X-GymLedger-Key` header present; key not in URL; key not in DTOs or error strings.
- Success decoding (including `nutritionPer100g`).
- Empty results list.
- Each HTTP/error mapping (400 invalid_query, 401 unauthorized, 429 budget_exceeded, 503 lookup_disabled/provider_disabled/feature_disabled/configuration_error, provider_error).
- Malformed body.
- Timeout/transport failure maps to a transport error.

Config:
- Conservative fallback when config fetch fails or body malformed.
- Enabled state (`onlineLookupAvailable && genericFoodSearch && !safeMode`) allows search.
- `safeMode=true` suppresses search.
- `features.genericFoodSearch=false` suppresses search.
- `minQueryLength` advertises and gates query length.
- In-memory cache reuses fresh config and re-fetches when stale.

Repository / product:
- Online setting disabled → no remote call.
- Missing API key → remote unavailable, no call.
- Too-short query → no remote call.
- Enabled valid query → loading/success/empty/error states.
- Selecting a remote result prefills the editable fields.
- Result remains editable; no automatic local save.
- Manual flow remains available after remote failure.
- Debounce is not required this phase (manual trigger). Cancellation is supported only if implementation introduces it.

Security:
- No key in error strings, DTOs, or URL.
- No real network access in unit tests.

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
```

`worker/` diff must be empty.

### Manual QA

- Online assistance setting OFF: remote controls absent or disabled; manual + smart entry work.
- Online assistance ON, no endpoint/key: remote assistance unavailable; manual entry works.
- Online assistance ON, valid endpoint + key, Worker in conservative/safe mode (current production): remote search reports disabled; manual entry works.
- Worker temporarily enabled for controlled testing (optional, only if user explicitly approves): successful USDA generic result prefills fields.
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
- Branch `17f-android-remote-lookup-plan` ready for ChatGPT GitHub review before merge to `dev`.

---