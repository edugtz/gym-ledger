# Phase 17F Implementation Plan — Android Remote Food Lookup Integration

## 1. Executive Decision

Implement **generic remote food search** by extending the existing `SmartFoodEntrySheet` with an optional "Online search" mode that calls the deployed GymLedger Worker. Keep everything offline-first and minimal:

- OkHttp client (approved by `ARCHITECTURE.md`/`PROJECT_SPEC`) behind an interface for testability.
- App-owned DTOs decoded with existing `kotlinx-serialization-json`.
- A `RemoteFoodLookupRepository` that orchestrates `/v1/config` (lazy, short TTL in-memory cache) and `/v1/foods/generic?q=`, gated by the existing `OnlineAssistanceSettings`.
- A remote result maps to the existing `FoodReference` shape so the current Smart Entry selected/quantity/nutrition UI renders it with source and approximate badges, then `FoodRepository.create` saves it as a normal local `Food` after explicit confirmation.
- No Room result cache. No barcode. No backend/Worker changes. No schema change. No new routes.

## 2. Discovery Findings

- Package is `com.edu.gymledger`. Empty `com/gymledger/**` dirs exist (pre-existing; untouched).
- UI event pattern: sealed `…UiEvent` over a CONFLATED `Channel`, collected in a `LaunchedEffect`.
- No HTTP client today. `kotlinx-serialization-json` present and configured. Serialization plugin applies to module.
- `FoodReference` domain model matches the remote per-100g shape (`caloriesPer100g: Int`, `proteinPer100g/carbsPer100g/fatPer100g: Double`, `sourceLabel: String`, `gramsPerUnit: Double?`, `unitLabel: String?`). Ideal reuse target for remote results (set `id = externalId`, `sourceLabel = attribution`, `gramsPerUnit = null`).
- `FoodReferenceCalculator` scales per-100g → grams; reused unchanged.
- `FoodRepository.create(...)` validates and persists per-serving `Food`; unchanged.
- `SmartFoodEntryViewModel` is synchronous for reference search, uses `viewModelScope.launch` for save. Adding remote (suspend) search needs `viewModelScope` + IO; this stays consistent.
- `AppContainer.settingsRepository` is `lateinit var ... private set`, initialized in `AppContainer.initialize(context)`.
- `SettingsRepository` already exposes endpoint, key, toggles, and flow. No settings change needed.
- Worker `/v1/config` is public; protected routes require `X-GymLedger-Key`.
- Current production Worker: conservative defaults, `safeMode=true`, all features off — remote search will report disabled against live production by default.

## 3. Current Repository State

- minSdk 28, targetSdk 35, Java/Kotlin 17, AGP 8.7.0, Kotlin 2.0.21, KSP 2.0.21-1.0.26, Room 2.6.1, kotlinx-serialization 1.7.3.
- Single `:app` module. Manual DI via `object AppContainer`.
- DB version 6, `exportSchema = false`, `fallbackToDestructiveMigration`. No 17F schema change.
- `AndroidManifest.xml` has no `INTERNET` permission today; must be added.
- `buildFeatures { compose = true }`; no `buildConfig`. No BuildConfig injection needed (key is user-entered).
- Existing tests: Robolectric + `runTest`; `kotlinx-coroutines-test` present. No existing ViewModel tests for nutrition features.

## 4. Chosen Architecture and Rationale

```
SmartFoodEntryViewModel
  -> SettingsRepository (Flow<OnlineAssistanceSettings>)
  -> RemoteFoodLookupRepository
       -> FoodLookupClient (interface)
            -> OkHttpFoodLookupClient
       -> in-memory config cache (TTL ~5 min)
       -> Worker /v1/config, /v1/foods/generic
  -> FoodReferenceRepository (local, unchanged)
  -> FoodRepository.create (save local Food, unchanged)
```

- Remote result → `RemoteFoodLookupResult` domain model → mapped to `FoodReference` so the existing selected/quantity/nutrition/Save flow is reused with minimal UI changes, and source/approximate badges already exist as chips.
- A sealed `FoodLookupOutcome` keeps remote success/empty/error distinct and testable; ViewModel maps it to `SmartFoodEntryEvent`/UI state consistent with existing patterns.
- Repository owns all gating logic (setting, key, config, safeMode, minQueryLength) so the ViewModel stays free of transport concerns.

## 5. Dependency Decision

Add **OkHttp** only.

- Why not JDK `HttpURLConnection`: more boilerplate, manual timeouts, no clean interceptor seam, less ergonomic. Would still need a transport interface for tests.
- Exact dependency: `com.squareup.okhttp3:okhttp` (version pinned in `gradle/libs.versions.toml`, e.g. `4.12.0`). No `okhttp-logging` in release; debug logging, if used, must never print the `X-GymLedger-Key` header.
- Runtime cost: small, free, widely used on Android API 28+.
- Testability: the `FoodLookupClient` interface lets unit tests inject a fake. OkHttp `Interceptor`/`MockWebServer` are optional and not required for unit tests. This plan uses a plain fake interface (no `MockWebServer` dependency needed this phase).
- Lifecycle/coroutines: OkHttp calls are synchronous blocking calls executed on `Dispatchers.IO` from a `viewModelScope.launch`; `withTimeout(5.s)` wraps the call. No extra coroutine integration libraries needed.

No other dependencies may be added.

## 6. Secret/Config Strategy

- **Key source:** user-entered in Settings → `SettingsRepository.foodLookupApiKey` (DataStore). Already masked in UI. Never in source/tests/docs/logs.
- **Endpoint source:** user-entered in Settings → `SettingsRepository.foodLookupEndpoint`. Blank → default public production URL constant in the remote client (non-secret).
- **No BuildConfig/local.properties for secrets.** No gradle.properties secret.
- **Header:** `X-GymLedger-Key: <key>` added by `OkHttpFoodLookupClient` only; never appended to the URL; never placed on `data` classes; never concatenated into error/log messages.
- **Missing key:** repository treats remote assistance as unavailable; no network call.
- **Release behavior:** identical to debug; no embedded key in any build.
- **gitignore:** unchanged. `.env`, `*.secret`, `local.properties` already ignored. No new secret file created.
- **Tests proving behavior:** a test asserts the request URL contains no key and the header contains the key; a test asserts error/exception messages and DTO `toString()` output do not contain the key value (the DTOs do not carry the key at all).

## 7. Exact Files to Create

Main source:

- `app/src/main/java/com/edu/gymledger/data/remote/dto/FoodLookupConfigDto.kt` — `@Serializable` DTOs for `/v1/config` envelope + payload (`onlineLookupAvailable`, `providers`, `features`, `minQueryLength`, `safeMode`).
- `app/src/main/java/com/edu/gymledger/data/remote/dto/GenericLookupResponseDto.kt` — `@Serializable` DTOs for generic success envelope + result item + `nutritionPer100g`.
- `app/src/main/java/com/edu/gymledger/data/remote/FoodLookupClient.kt` — interface: `suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<Config>` and `suspend fun searchGeneric(baseUrl: String, apiKey: String, query: String): FoodLookupOutcome<List<RemoteResultDto>>`.
- `app/src/main/java/com/edu/gymledger/data/remote/OkHttpFoodLookupClient.kt` — OkHttp implementation; `X-GymLedger-Key` header; 5s connect/read/write timeout; maps HTTP status + body to `FoodLookupOutcome`.
- `app/src/main/java/com/edu/gymledger/data/remote/FoodLookupOutcome.kt` — sealed type: `Success<T>`, `Empty`, `Error(reason: FoodLookupError)` plus `FoodLookupError` enum/sealed mapping (Transport, Unauthorized, InvalidQuery, LookupDisabled, ProviderDisabled, FeatureDisabled, BudgetExceeded, ConfigurationError, ProviderError, MalformedResponse).
- `app/src/main/java/com/edu/gymledger/domain/model/lookup/RemoteFoodLookupResult.kt` — domain model: `externalId`, `name`, `description?`, `dataType?`, `source`, `attribution`, `isApproximate`, `caloriesPer100g`, `proteinPer100g`, `carbohydratePer100g`, `fatPer100g`.
- `app/src/main/java/com/edu/gymledger/domain/model/lookup/RemoteFoodReferenceMapper.kt` — maps `RemoteFoodLookupResult` → `FoodReference` (id=externalId, sourceLabel=attribution, gramsPerUnit=null, unitLabel=null) and maps to editable field defaults (100g quantity).
- `app/src/main/java/com/edu/gymledger/data/repository/lookup/RemoteFoodLookupRepository.kt` — orchestrates config (in-memory cache + TTL), gating, generic search; returns `FoodLookupOutcome<List<RemoteFoodLookupResult>>` and exposes effective availability + minQueryLength.

Tests:

- `app/src/test/java/com/edu/gymledger/data/remote/OkHttpFoodLookupClientTest.kt` — URL routing/encoding, header present + not in URL, success/empty/error mappings, malformed body, timeout.
- `app/src/test/java/com/edu/gymledger/data/repository/lookup/RemoteFoodLookupRepositoryTest.kt` — config fallback, enabled/safeMode/feature-disabled gating, minQueryLength gating, settings/key gating, success/empty/error, cache freshness.
- `app/src/test/java/com/edu/gymledger/domain/model/lookup/RemoteFoodReferenceMapperTest.kt` — per-100g → FoodReference mapping, name/attribution preserved, null gramsPerUnit.
- `app/src/test/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModelRemoteTest.kt` — online setting off no call; missing key unavailable; too-short query no call; valid query states; prefill on select; no auto-save; manual after failure; key never in UI/error state.

Docs:

- `docs/CURRENT_PHASE.md` (replaced).
- `docs/IMPLEMENTATION_PLAN.md` (replaced).

## 8. Exact Files to Modify

- `gradle/libs.versions.toml` — add `okhttp = "4.12.0"` version + `okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }` library.
- `app/build.gradle.kts` — `implementation(libs.okhttp)`.
- `app/src/main/AndroidManifest.xml` — add `<uses-permission android:name="android.permission.INTERNET" />`.
- `app/src/main/java/com/edu/gymledger/app/AppContainer.kt` — build one shared `OkHttpClient` (lazy val), expose `remoteFoodLookupRepository` using `settingsRepository`. Keep existing fields identical.
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModel.kt` — add `remoteFoodLookupRepository` + `settingsRepository` deps; add online search state (availability, minQueryLength, results, loading, error, selected remote mode); gate remote calls; map outcome to events/existing selected-reference flow via the mapper; keep local reference path intact.
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModelFactory.kt` — pass `remoteFoodLookupRepository` + `settingsRepository` (resolve from `AppContainer`).
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryScreen.kt` — add an "Online search" segment/toggle in the search section when remote assistance is available; show source + approximate badges for remote results (reuse the existing `sourceLabel` chip + "Approximate" chip from `SmartSelectedSection`); selecting a remote result reuses the existing selected/quantity/nutrition/Save UI; show a concise remote unavailable/error message that does not expose internals.

## 9. Exact Files Not to Touch

- `worker/**` (no backend/Worker changes).
- `app/src/main/java/com/edu/gymledger/data/db/**` (no schema change).
- `app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/SettingsRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/OnlineAssistanceSettings.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/FoodReferenceRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/reference/FoodReferenceSeed.kt`.
- `app/src/main/java/com/edu/gymledger/domain/model/Food.kt`, `FoodReference.kt`, `FoodReferenceCalculator.kt`.
- `app/src/main/java/com/edu/gymledger/navigation/**`.
- `app/src/main/java/com/edu/gymledger/feature/settings/**`.
- All non-nutrition features.
- `app/src/main/java/com/gymledger/**`.

## 10. Data Flow

1. `SmartFoodEntrySheet` opens; ViewModel collects `OnlineAssistanceSettings` and asks repository for effective availability (may trigger lazy config fetch from Worker, cached).
2. If remote unavailable → only the existing local reference search is shown (current behavior).
3. If remote available → search section shows an "Online search" toggle. With online mode selected, typing/triggering a search with query length ≥ `minQueryLength` calls `RemoteFoodLookupRepository.searchGeneric`.
4. Repository checks setting + key + cached config gates; builds request to `/v1/foods/generic?q=<url-encoded-query>` with `X-GymLedger-Key`; runs on IO with timeout.
5. `OkHttpFoodLookupClient` decodes body to `GenericLookupResponseDto`; maps errors by code/status/transport.
6. Repository maps DTO results to `List<RemoteFoodLookupResult>`, returns `Success|Empty|Error`.
7. ViewModel maps results to UI list; selecting one calls `RemoteFoodReferenceMapper` → `FoodReference`, reusing `selectReference`-equivalent logic (per-100g + 100g default quantity).
8. User edits quantity/nutrition in the existing editable section; `save()` calls `FoodRepository.create` (no new save path, no auto-save).

## 11. Config-Fetch Behavior

- Endpoint: `<baseUrl>/v1/config`, GET, **no** API key (public).
- Fetched lazily on first remote search attempt; never at app startup.
- Cached in-memory in `RemoteFoodLookupRepository` with a TTL of ~5 minutes; re-fetched when stale; reused across searches within TTL.
- Conservative fallback on any failure/timeout/malformed body: `onlineLookupAvailable=false`, `genericFoodSearch=false`, `safeMode=true`, `minQueryLength=3`.
- No config call per keystroke. No stale config used beyond TTL.
- When `onlineFoodLookupEnabled` becomes `false`, no config or search calls occur regardless of cache.

## 12. Generic-Search Flow

- Route: `<baseUrl>/v1/foods/generic?q=<encoded>`, GET, with `X-GymLedger-Key`.
- Query must be trimmed and length ≥ cached/default `minQueryLength` (default 3); below → no remote call.
- Connect/read/write timeout = 5s; wrapped in `withTimeout`.
- Decode success envelope; `data.results` may be empty → `Empty` outcome.
- Map each result's `nutritionPer100g` into per-100g domain model.
- A successful search does not persist anything; only Save persists.

## 13. Result-to-Local-Food Mapping

- `RemoteFoodLookupResult` → `FoodReference` (id=externalId; name; caloriesPer100g; protein/carbs/fat per 100g; sourceLabel=attribution; gramsPerUnit=null; unitLabel=null).
- Default quantity = 100g; `FoodReferenceCalculator.calculateFromGrams(ref, 100.0)` yields editable per-serving defaults.
- `save()` reuses the existing path: `foodRepository.create(name, caloriesPerServing = calories, servingSize = grams, protein, carbs, fat)`.
- All values remain editable; Save is the only persistence step.

## 14. Error Mapping

| Source | Outcome | User message (English, no internals) |
|---|---|---|
| No network / `IOException` not a timeout | `Error(Transport)` | "Couldn't reach the lookup service. Check your connection and try again." |
| Timeout | `Error(Transport)` | same |
| Malformed JSON | `Error(MalformedResponse)` | "Lookup service returned an unexpected response." |
| 401 / missing local key | `Error(Unauthorized)` | "Online lookup isn't configured. Add an API key in Settings." |
| 400 `invalid_query` | `Error(InvalidQuery)` | "Enter a longer search term." |
| 429 `budget_exceeded` | `Error(BudgetExceeded)` | "Daily lookup limit reached. Try again tomorrow or add the food manually." |
| 503 `lookup_disabled` | `Error(LookupDisabled)` | "Online lookup is temporarily disabled." |
| 503 `provider_disabled` | `Error(ProviderDisabled)` | "Online lookup is temporarily unavailable." |
| 503 `feature_disabled` | `Error(FeatureDisabled)` | same |
| 503 `configuration_error` | `Error(ConfigurationError)` | "Online lookup is temporarily unavailable." |
| Body `provider_error` | `Error(ProviderError)` | "Online lookup is temporarily unavailable." |
| `results` empty | `Empty` | "No foods found online. Try another term or add it manually." |

- Developer diagnostics (logcat) must omit the API key entirely. Only non-sensitive path/status/code are logged.
- Error `Throwable.message` and DTO `toString()` must not include the key.

## 15. ViewModel/UI-State Changes

- `SmartFoodEntryUiState` gains: `isOnlineAvailable: Boolean`, `minQueryLength: Int`, `onlineMode: Boolean`, `onlineResults: List<RemoteFoodLookupResult>`, `onlineLoading: Boolean`, `onlineError: String?`. (Keep within one state class; do not split into micro-files.)
- New `SmartFoodEntryEvent`: reuse existing `SaveSucceeded`/`Error`; remote errors surface via `onlineError` in state rather than a snackbar to keep the sheet self-contained.
- Search section: when `isOnlineAvailable`, render a segmented switch with "Local reference" / "Online search". When online mode is selected, show remote results list and the source chip on each row.
- Selected section: reuse `SmartSelectedSection` (already shows `sourceLabel` + "Approximate" chips) — the mapper ensures remote results populate `sourceLabel` with attribution.
- Quantity/nutrition/save: unchanged.
- If `!isOnlineAvailable`, the toggle is omitted and behavior is identical to today.

## 16. Test Plan by Layer

Transport/client (`OkHttpFoodLookupClientTest`, fake transport via interface):
- URL: `<baseUrl>/v1/foods/generic?q=egg` with proper percent-encoding.
- API key header present; URL has no key.
- Success body decodes including `nutritionPer100g`.
- Empty `results` → `Empty`.
- 400 → `InvalidQuery`; 401 → `Unauthorized`; 429 → `BudgetExceeded`; 503 with each known code → mapped; `provider_error` body → `ProviderError`.
- Malformed body → `MalformedResponse`.
- Timeout/IOException → `Transport`.
- Config route: `<baseUrl>/v1/config`, no key header; success/fallback.

Config (`RemoteFoodLookupRepositoryTest`):
- Config fetch failure/malformed → conservative fallback.
- Config success with `onlineLookupAvailable && genericFoodSearch && !safeMode` → available.
- `safeMode=true` → unavailable.
- `features.genericFoodSearch=false` → unavailable.
- `minQueryLength` value honored.
- Cache reused within TTL, re-fetched when stale.

Repository / gating (`RemoteFoodLookupRepositoryTest`):
- Setting disabled → no config/search call.
- Key blank → unavailable, no call.
- Query shorter than `minQueryLength` → no call.
- Valid query → `Success`/`Empty`/`Error` mapping.
- Endpoint blank → default base URL used.

ViewModel (`SmartFoodEntryViewModelRemoteTest`, fake repo + fake settings flow):
- Online setting disabled → toggle absent, no remote call.
- Missing key → unavailable.
- Too-short query → no remote call.
- Valid query → loading/success/empty/error states.
- Selecting a remote result prefills editable fields.
- Editable fields remain editable; no save until explicit call.
- Manual flow available after remote failure.
- Key value never present in UI state or error strings.

Security (across layers):
- No key in URL, DTOs, error strings; no real network in unit tests.

## 17. Implementation Order

1. Add OkHttp dependency + INTERNET permission; run clean assembleDebug to confirm empty wiring compiles.
2. Create DTOs + `FoodLookupOutcome`/`FoodLookupError`.
3. Create `FoodLookupClient` interface + `OkHttpFoodLookupClient`; write `OkHttpFoodLookupClientTest` with a fake transport.
4. Create `RemoteFoodLookupResult` + `RemoteFoodReferenceMapper` + tests.
5. Create `RemoteFoodLookupRepository` with config cache + tests.
6. Wire `AppContainer` (shared `OkHttpClient` + repository).
7. Extend `SmartFoodEntryViewModel` + factory with online mode + gating + prefill; add ViewModel tests.
8. Extend `SmartFoodEntryScreen` with online toggle, result rows, source/approximate badges, unavailable/error copy.
9. Run full validation; fix first real error only; no unrelated refactors.
10. Manual QA; record results.

## 18. Risks and Build Traps

- `@Serializable` DTOs must use nullable/`default` fields to tolerate minor Worker field changes; decode ignores unknown keys (configure `Json { ignoreUnknownKeys = true; isLenient = true }`).
- OkHttp call must be invoked off the main thread; always wrap in `withContext(Dispatchers.IO)` / `withTimeout`.
- Avoid importing OkHttp logging interceptor in release builds; if used in debug, never log the key header.
- Don't accidentally trigger config/search when only `onlineFoodLookupEnabled` toggled on but key blank — gating order matters (setting → key → config → query length).
- Don't URL-encode the leading `?` of the query parameter; only encode the query value.
- Don't mutate `FoodReference` data class shape (out of scope); only construct instances.
- Don't add a `MockWebServer` dependency — fake the client interface instead.
- Don't add barcode fields even though the remote result DTO might conceptually allow it.
- Network permission present only in debug is fine if lint complains about release; but add it unconditionally so release builds can use the feature once enabled by the user.

## 19. Validation Plan

From repo root:

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

Targeted unit tests:

```bash
./gradlew testDebugUnitTest --tests "com.edu.gymledger.data.remote.*" --tests "com.edu.gymledger.data.repository.lookup.*" --tests "com.edu.gymledger.domain.model.lookup.*" --tests "com.edu.gymledger.feature.nutrition.SmartFoodEntryViewModelRemoteTest"
```

Scope gate:

```bash
git status --short --untracked-files=all
git diff --name-status
git diff -- worker/ || true
git diff -- app/src/main/java/com/edu/gymledger/data/db || true
```

Secret gate:

```bash
git grep -nE "X-GymLedger-Key:" -- app/src || true
git grep -n "foodLookupApiKey" -- app/src/main app/src/test || true
```

Package safety:

```bash
grep -R -n "com\.gymledger" app/src || true
```

Install/run (UI phase):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Worker tests: not run (Worker is not touched).

## 20. Manual QA Checklist

- Online assistance OFF: Foods screen and Smart Entry sheet behave as before; no remote UI.
- Online assistance ON, endpoint/key blank: online toggle absent or "not configured"; manual entry works.
- Online assistance ON, key present, Worker default (safe mode): online toggle present; triggering search yields "temporarily disabled"; manual entry works.
- Temporarily enable Worker generic search (only if user explicitly approves): enter `egg`; results appear; each shows source + approximate; selecting prefills editable fields (calories/macros/quantity).
- Edit values, then Save: local `Food` created with edited values; appears in Foods list.
- Discard (Cancel) after selecting a remote result: no `Food` created.
- App restart: settings persisted; no remote call at startup; Smart Entry resets to local mode.
- Airplane mode: remote search fails with "Couldn't reach …"; manual + local reference entry usable.
- Rotation/navigation: sheet state behaves per existing implementation; no crash.
- No barcode field/button anywhere.
- Verify `worker/` has zero diff after the phase.

## 21. Model Routing for Implementation, Debugging, and Review

- Implementation (UX integration): OpenCode Go Qwen3.6 Plus or Kimi K2.6.
- Android networking/runtime (OkHttp timeouts, threading, permission, manifest): Gemini Android Studio.
- Pre-commit review: OpenCode Go DeepSeek V4 Pro.
- Final/visual review (optional screenshots): ChatGPT or Codex.
- Do not escalate simple build/test specifics to premium models.

## 22. Builder Preflight Prompt

```text
Role: Builder preflight only. Do not edit files yet.

Read in order:
1. AGENTS.md
2. docs/CURRENT_PHASE.md
3. docs/IMPLEMENTATION_PLAN.md
4. docs/ARCHITECTURE.md
5. docs/AI_WORKFLOW.md
6. docs/ONLINE_ASSISTED_PLATFORM.md
7. docs/FOOD_LOOKUP_DEPLOYMENT.md

Confirm:
- Package is com.edu.gymledger; no com.gymledger.
- Actual existing files listed in the plan exist with the described patterns.
- SmartFoodEntryViewModel/Factory/Screen current behavior.
- SettingsRepository/OnlineAssistanceSettings fields: onlineFoodLookupEnabled, foodLookupEndpoint, foodLookupApiKey.
- AppContainer wiring pattern and settingsRepository lateinit.
- AndroidManifest has no INTERNET permission yet.
- Confirm exact files to create and modify per docs/IMPLEMENTATION_PLAN.md.
- Confirm no Room schema change, no Worker change, no new routes.

Output the Builder Preflight format (discovery, files to create/modify/not touch, summary, validation command, quality gates, manual QA, risks/mismatches, approval status). Stop and wait for approval.
```

## 23. Reviewer Prompt

```text
Review the Phase 17F diff only. Do not edit.

Read:
- docs/CURRENT_PHASE.md
- docs/IMPLEMENTATION_PLAN.md
- docs/AI_WORKFLOW.md
- git diff

Check:
1. Only generic remote search is implemented; no barcode/scanning/recents/favorites.
2. OkHttp is the only new dependency.
3. API key is user-entered, sent only as X-GymLedger-Key header, never in URL/DTOs/logs/errors.
4. Offline-first preserved; manual/local entry unaffected by all disabled/error states.
5. No Room schema change; no Worker changes; no new routes.
6. Package com.edu.gymledger only.
7. UI text English; source + approximate badges present for remote results.
8. No auto-save; explicit Save only.
9. Tests use fakes; no real network.
10. Tests cover transport, config, gating, ViewModel, security invariants.

Return PASS, PASS_WITH_NOTES, or BLOCKED with blockers, non-blocking concerns, scope creep, risky files, and a suggested commit message.
```

## 24. Commit-Ready Gate

Before commit:

```bash
git status --short --untracked-files=all
git diff --stat
git diff -- worker/ || true
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
git grep -nE "X-GymLedger-Key:" -- app/src || true
grep -R -n "com\.gymledger" app/src || true
```

Commit only if validation passes, manual QA done, scope clean, no secret value present, no worker diff, no schema diff, reviewer PASS/PASS_WITH_NOTES.

## 25. Stop Conditions

- Phase 17E.4 absent from branch history.
- Dirty repo unexpectedly.
- First real build/test/lint error not fixed within two local attempts (escalate).
- Real API key value found in source/tests/docs/logs.
- Worker code modified.
- Room schema/DB version changed.
- Barcode UI introduced.
- Remote results auto-save without confirmation.
- Any dependency beyond OkHttp appears necessary.
- Reviewer returns BLOCKED and the blocker cannot be resolved within scope.

---