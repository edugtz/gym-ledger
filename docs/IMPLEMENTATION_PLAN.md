# Phase 17F Implementation Plan — Android Remote Food Lookup Integration

## 1. Executive Decision

Implement **generic remote food search** by extending the existing `SmartFoodEntrySheet` with an optional "Online search" mode that calls the deployed GymLedger Worker. Keep everything offline-first and minimal:

- OkHttp client (approved by `ARCHITECTURE.md`/`PROJECT_SPEC`) behind a `okhttp3.Call.Factory` seam for testability.
- App-owned DTOs decoded with existing `kotlinx-serialization-json` using strict syntax (`ignoreUnknownKeys=true`, `isLenient=false`).
- A cancellation-aware OkHttp bridge using `Call.enqueue` + `suspendCancellableCoroutine`; the underlying `Call` is cancelled when the coroutine is cancelled.
- A `RemoteFoodLookupRepository` that orchestrates `/v1/config` (lazy, 5-minute in-memory cache via an injectable monotonic time source) and `/v1/foods/generic?q=`, gated by the existing `OnlineAssistanceSettings` plus a validated endpoint.
- Blank endpoint → default production Worker URL; blank API key → remote not configured.
- A remote result maps to the existing `FoodReference` shape so the current Smart Entry selected/quantity/nutrition UI renders it with source and approximate badges, then `FoodRepository.create` saves it as a normal local `Food` after explicit confirmation.
- Nullable nutrition contract: DTO nutrients nullable; incomplete/negative/non-finite results filtered; valid calories rounded to `Int`; `Empty` if all filtered. `FoodReference` schema unchanged.
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
- `SettingsRepository` already exposes endpoint, key, toggles, and flow. No settings DataStore change needed.
- `SettingsViewModel` exposes update methods; helper text currently says an endpoint must be entered when lookup is enabled, which conflicts with "Leave blank to use default." This must be resolved.
- Worker `/v1/config` is public; protected routes require `X-GymLedger-Key`.
- Current production Worker: conservative defaults, `safeMode=true`, all features off — remote search will report disabled against live production by default.

## 3. Current Repository State

- minSdk 28, targetSdk 35, Java/Kotlin 17, AGP 8.7.0, Kotlin 2.0.21, KSP 2.0.21-1.0.26, Room 2.6.1, kotlinx-serialization 1.7.3.
- Single `:app` module. Manual DI via `object AppContainer`.
- DB version 6, `exportSchema = false`, `fallbackToDestructiveMigration`. No 17F schema change.
- `AndroidManifest.xml` has no `INTERNET` permission today; must be added.
- `buildFeatures { compose = true }`; no `buildConfig`. No BuildConfig injection needed (key is user-entered).
- Existing tests: Robolectric + `runTest`; `kotlinx-coroutines-test` present. No existing ViewModel tests for nutrition or settings features.

## 4. Chosen Architecture and Rationale

```
SmartFoodEntryViewModel
  -> SettingsRepository (Flow<OnlineAssistanceSettings>)
  -> RemoteFoodLookupRepository
       -> EndpointValidator (default URL when blank; validate custom HTTPS)
       -> FoodLookupClient (interface)
            -> OkHttpFoodLookupClient(okhttp3.Call.Factory)
                production: shared OkHttpClient
                tests: FakeCallFactory / FakeCall
                Call.enqueue + suspendCancellableCoroutine
       -> in-memory config cache (TTL 5 min, injectable monotonic time source)
       -> Worker /v1/config, /v1/foods/generic
  -> FoodReferenceRepository (local, unchanged)
  -> FoodRepository.create (save local Food, unchanged)
```

- Remote result → `RemoteFoodLookupResult` domain model → mapped to `FoodReference` so the existing selected/quantity/nutrition/Save flow is reused with minimal UI changes, and source/approximate badges already exist as chips.
- A sealed `FoodLookupOutcome` keeps remote success/empty/error distinct and testable; ViewModel maps it to inline UI state consistent with existing patterns.
- Repository owns all gating logic (local gates + endpoint validation + remote config gates + query length) so the ViewModel stays free of transport concerns.
- The `Call.Factory` seam tests the real `OkHttpFoodLookupClient` without network. Repository/ViewModel tests may fake `FoodLookupClient`.

## 5. Dependency Decision

Add **OkHttp** only.

- Why not JDK `HttpURLConnection`: more boilerplate, manual timeouts, no clean interceptor/Call.Factory seam, less ergonomic.
- Exact dependency: `com.squareup.okhttp3:okhttp` (version pinned in `gradle/libs.versions.toml`, e.g. `4.12.0`). No `okhttp-logging` in release; debug logging, if used, must never print the `X-GymLedger-Key` header.
- Runtime cost: small, free, widely used on Android API 28+.
- Testability: `OkHttpFoodLookupClient` receives `okhttp3.Call.Factory`; production injects a shared `OkHttpClient` (which implements `Call.Factory`); tests inject a `FakeCallFactory`/`FakeCall` that captures the `Request` and returns a programmed `Response` or throws. No `MockWebServer` dependency.
- Cancellation: `Call.enqueue` + `suspendCancellableCoroutine`; `invokeOnCancellation` calls `Call.cancel()`. Do not rely on `withTimeout` around blocking `Call.execute` as if it cancelled the underlying network operation. A coroutine timeout still cancels the suspended coroutine, which cancels the `Call`.
- Lifecycle/coroutines: no extra coroutine/network dependency. OkHttp connect/read/write timeouts ~5s. No additional dependency may be added.

## 6. Secret/Config Strategy

- **Key source:** user-entered in Settings → `SettingsRepository.foodLookupApiKey` (DataStore). Already masked in UI. Never in source/tests/docs/logs.
- **Endpoint source:** user-entered in Settings → `SettingsRepository.foodLookupEndpoint`. Blank → default public production Worker URL constant (non-secret). A non-blank endpoint must be validated (absolute HTTPS, no userinfo, no query, no fragment, trailing slash normalized).
- **No BuildConfig/local.properties for secrets.** No gradle.properties secret.
- **Header:** `X-GymLedger-Key: <key>` added by `OkHttpFoodLookupClient` only on protected routes; never on `/v1/config`; never appended to the URL; never placed on `data` classes; never concatenated into error/log messages.
- **Missing key:** repository treats remote assistance as unavailable; no network call.
- **Release behavior:** identical to debug; no embedded key in any build.
- **gitignore:** unchanged. `.env`, `*.secret`, `local.properties` already ignored. No new secret file created.
- **Tests proving behavior:** a test asserts the request URL contains no key and the header contains the key on `/v1/foods/generic`; a test asserts `/v1/config` has no key header; a test asserts error/exception messages and DTO `toString()` output do not contain the key value.

## 7. Endpoint Semantics

- `foodLookupEndpoint` blank → default production Worker URL constant (`https://gymledger-food-lookup.eduardo-gutierrez-2325.workers.dev/` with normalized trailing slash). No custom endpoint required.
- `foodLookupApiKey` blank → remote lookup is not configured; no network call.
- A non-blank custom endpoint is validated by `EndpointValidator`:
  - Must be an absolute `https://` URL.
  - Must have no userinfo (no embedded `user:pass@`).
  - Must have no query.
  - Must have no fragment.
  - Normalize to a single trailing slash.
  - Invalid → remote unavailable with an inline "invalid endpoint" state; no network call.
- The API key is sent only to the resolved validated origin.
- `SettingsViewModel` may expose endpoint validity for helper text; `SettingsScreen` may update helper text only to resolve the "Leave blank to use default" contradiction. `SettingsRepository` and `OnlineAssistanceSettings` are not modified.

## 8. Complete Generic-Search Gating

Local gates (must all hold before any network call):

1. `onlineFoodLookupEnabled = true`
2. `usdaEnabled = true`
3. `safeModeEnabled = false`
4. API key non-blank
5. endpoint blank (→ default URL) or valid custom HTTPS endpoint

Remote config gates (from cached `/v1/config`, must all hold):

1. `onlineLookupAvailable = true`
2. `providers.usda = true`
3. `features.genericFoodSearch = true`
4. `safeMode = false`
5. query length ≥ `minQueryLength`

`openFoodFactsEnabled` is explicitly unused in Phase 17F and remains for Phase 17G.

## 9. Nullable Nutrition Contract

The actual Worker contract allows `null` for `caloriesKcal`, `proteinG`, `carbohydrateG`, and `fatG`. `FoodReference` does not allow nullable nutrition and must not be modified.

Policy:

- DTO nutrient fields (`caloriesKcal`, `proteinG`, `carbohydrateG`, `fatG`) remain nullable (`Double?` / `Int?` as matches the JSON; use `Double?` for all and round calories).
- Do not coerce `null` to zero.
- Filter any result that has a missing (`null`), negative, or non-finite nutrient among calories/protein/carbs/fat.
- Convert valid calories to `Int` using an explicitly documented rounding rule: round to nearest integer, 0.5 rounds up, then require ≥ 0.
- If all provider results are filtered out, return `Empty`.
- Do not modify `FoodReference` or the Room schema.

## 10. Exact Files to Create

Main source:

- `app/src/main/java/com/edu/gymledger/data/remote/dto/FoodLookupConfigDto.kt` — `@Serializable` DTOs for `/v1/config` envelope + payload (`onlineLookupAvailable`, `providers { usda, openFoodFacts }`, `features { genericFoodSearch, barcodeLookup }`, `minQueryLength`, `safeMode`).
- `app/src/main/java/com/edu/gymledger/data/remote/dto/GenericLookupResponseDto.kt` — `@Serializable` DTOs for generic success envelope + result item + `nutritionPer100g` with nullable `caloriesKcal`, `proteinG`, `carbohydrateG`, `fatG`.
- `app/src/main/java/com/edu/gymledger/data/remote/FoodLookupClient.kt` — interface: `suspend fun fetchConfig(baseUrl: String): FoodLookupOutcome<FoodLookupConfig>` and `suspend fun searchGeneric(baseUrl: String, apiKey: String, query: String): FoodLookupOutcome<List<GenericLookupItemDto>>`.
- `app/src/main/java/com/edu/gymledger/data/remote/OkHttpFoodLookupClient.kt` — OkHttp implementation receiving `okhttp3.Call.Factory`; `X-GymLedger-Key` header on protected routes only; ~5s connect/read/write timeouts; `Call.enqueue` + `suspendCancellableCoroutine` with `invokeOnCancellation { call.cancel() }`; maps HTTP status + body to `FoodLookupOutcome`.
- `app/src/main/java/com/edu/gymledger/data/remote/FoodLookupOutcome.kt` — sealed type: `Success<T>`, `Empty`, `Error(reason: FoodLookupError)` plus `FoodLookupError` mapping (Transport, Unauthorized, InvalidQuery, LookupDisabled, ProviderDisabled, FeatureDisabled, BudgetExceeded, ConfigurationError, ProviderError, MalformedResponse).
- `app/src/main/java/com/edu/gymledger/data/remote/EndpointValidator.kt` — resolves blank → default URL; validates custom HTTPS (no userinfo/query/fragment; trailing slash); returns sealed `EndpointResult { Default, Valid(url), Invalid }`.
- `app/src/main/java/com/edu/gymledger/domain/model/lookup/RemoteFoodLookupResult.kt` — domain model: `externalId`, `name`, `description?`, `dataType?`, `source`, `attribution`, `isApproximate`, `caloriesPer100g: Int`, `proteinPer100g: Double`, `carbohydratePer100g: Double`, `fatPer100g: Double`.
- `app/src/main/java/com/edu/gymledger/domain/model/lookup/RemoteFoodReferenceMapper.kt` — filters incomplete/negative/non-finite nutrients; rounds valid calories to `Int` (0.5 up); maps complete results to `FoodReference` (id=externalId, sourceLabel=attribution, gramsPerUnit=null, unitLabel=null); returns only valid items or `Empty` if all filtered.
- `app/src/main/java/com/edu/gymledger/data/repository/lookup/RemoteFoodLookupRepository.kt` — orchestrates endpoint validation, config (in-memory cache + 5-min TTL via injectable monotonic time source), all local + remote gating, generic search; returns `FoodLookupOutcome<List<RemoteFoodLookupResult>>` and exposes effective availability + minQueryLength.
- `app/src/main/java/com/edu/gymledger/data/remote/MonotonicTimeSource.kt` — minimal interface (`fun nowMillis(): Long`) for TTL tests; production uses `SystemClock.elapsedRealtime()` or `System.currentTimeMillis()`.

Tests:

- `app/src/test/java/com/edu/gymledger/data/remote/OkHttpFoodLookupClientTest.kt` — fake `Call.Factory`/`FakeCall`; captures `Request`; verifies route, URL encoding, key header present + not in URL, config route has no key, success/empty/error mappings, malformed body, cancellation cancels the `Call`.
- `app/src/test/java/com/edu/gymledger/data/remote/EndpointValidatorTest.kt` — blank → default; valid HTTPS → normalized; http → invalid; userinfo → invalid; query → invalid; fragment → invalid; missing trailing slash → normalized.
- `app/src/test/java/com/edu/gymledger/data/repository/lookup/RemoteFoodLookupRepositoryTest.kt` — config fallback, enabled/safeMode/feature-disabled/provider-usda-disabled gating, minQueryLength gating, settings/key/usdaEnabled/safeModeEnabled gating, endpoint blank/valid/invalid, success/empty/error, cache freshness via fake time source.
- `app/src/test/java/com/edu/gymledger/domain/model/lookup/RemoteFoodReferenceMapperTest.kt` — complete → mapped; incomplete (null) → filtered; mixed → only complete; negative → filtered; non-finite → filtered; all filtered → empty; calories rounding (0.5 up).
- `app/src/test/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModelRemoteTest.kt` — online setting off → toggle absent; enabled missing key → inline not configured; enabled `usdaEnabled=false` → inline; enabled `safeModeEnabled=true` → inline; enabled invalid endpoint → inline; enabled valid remote disabled → inline temporarily disabled; too-short query → no call; valid query → loading/success/empty/error; prefill on select; no auto-save; manual after failure; cancellation on leave/dismiss; duplicate concurrent prevented; key never in UI/error.

Docs:

- `docs/CURRENT_PHASE.md` (replaced).
- `docs/IMPLEMENTATION_PLAN.md` (replaced).

## 11. Exact Files to Modify

- `gradle/libs.versions.toml` — add `okhttp = "4.12.0"` version + `okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }` library.
- `app/build.gradle.kts` — `implementation(libs.okhttp)`.
- `app/src/main/AndroidManifest.xml` — add `<uses-permission android:name="android.permission.INTERNET" />`.
- `app/src/main/java/com/edu/gymledger/app/AppContainer.kt` — build one shared `OkHttpClient` (lazy val), expose `remoteFoodLookupRepository` using `settingsRepository` + monotonic time source. Keep existing fields identical.
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModel.kt` — add `remoteFoodLookupRepository` + `settingsRepository` deps; add online search state (availability, minQueryLength, onlineMode, results, loading, error, concurrent guard); gate remote calls; map outcome to events/existing selected-reference flow via the mapper; manual submission only; cancel on leave/dismiss; keep local reference path intact.
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModelFactory.kt` — pass `remoteFoodLookupRepository` + `settingsRepository` (resolve from `AppContainer`).
- `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryScreen.kt` — add an "Online search" segment/toggle visible whenever `onlineFoodLookupEnabled=true`; show source + approximate badges for remote results (reuse the existing `sourceLabel` chip + "Approximate" chip); selecting a remote result reuses the existing selected/quantity/nutrition/Save UI; show inline user-safe states for not-configured/usda-disabled/safe-mode/invalid-endpoint/temporarily-disabled; manual submission via IME Search or "Search online" button; no per-keystroke network.
- `app/src/main/java/com/edu/gymledger/feature/settings/SettingsViewModel.kt` — expose endpoint validity/helper if needed for consistent helper text (no change to SettingsRepository/OnlineAssistanceSettings).
- `app/src/main/java/com/edu/gymledger/feature/settings/SettingsScreen.kt` — only if helper text must change to resolve the "Leave blank to use default" contradiction (e.g., remove "Enter an endpoint URL to enable online lookup." helper, keep "Leave blank to use default.").
- `app/src/test/java/com/edu/gymledger/feature/settings/SettingsViewModelEndpointValidationTest.kt` — if SettingsViewModel exposes endpoint validity, test validation assertions.

## 12. Exact Files Not to Touch

- `worker/**` (no backend/Worker changes).
- `app/src/main/java/com/edu/gymledger/data/db/**` (no schema change).
- `app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/SettingsRepository.kt` (read-only consumer; no DataStore key changes).
- `app/src/main/java/com/edu/gymledger/data/repository/OnlineAssistanceSettings.kt`.
- `app/src/main/java/com/edu/gymledger/data/repository/FoodReferenceRepository.kt`.
- `app/src/main/java/com/edu/gymledger/data/reference/FoodReferenceSeed.kt`.
- `app/src/main/java/com/edu/gymledger/domain/model/Food.kt`, `FoodReference.kt`, `FoodReferenceCalculator.kt`.
- `app/src/main/java/com/edu/gymledger/navigation/**`.
- All non-nutrition, non-settings features.
- `app/src/main/java/com/gymledger/**`.
- Barcode/product lookup work.

## 13. Data Flow

1. `SmartFoodEntrySheet` opens; ViewModel collects `OnlineAssistanceSettings` and computes local gate state. No config fetch at startup or while Local reference mode is active.
2. If `onlineFoodLookupEnabled=false` → toggle absent; only local reference search (current behavior).
3. If `onlineFoodLookupEnabled=true` → toggle visible. Entering Online search triggers a lazy config fetch (cached 5 min via monotonic time source) unless already cached fresh.
4. ViewModel computes effective availability from local gates + remote config; failures show inline state, not removal of the mode.
5. User types a query (`onValueChange` only updates state). User submits via IME Search or "Search online" button. Query must be trimmed and length ≥ `minQueryLength`; otherwise no call and an inline hint.
6. Repository validates endpoint, checks local gates, checks cached config gates, builds request to `/v1/foods/generic?q=<url-encoded-query>` with `X-GymLedger-Key`; runs via `Call.enqueue` + `suspendCancellableCoroutine` on `Dispatchers.IO` with ~5s timeouts.
7. `OkHttpFoodLookupClient` decodes body to `GenericLookupResponseDto` (strict JSON, `ignoreUnknownKeys=true`, `isLenient=false`); maps errors by code/status/transport.
8. Repository maps DTO results through `RemoteFoodReferenceMapper` (filter nullable/negative/non-finite nutrients; round calories), returns `Success|Empty|Error`.
9. ViewModel maps results to UI list; selecting one calls mapper → `FoodReference`, reusing selected/quantity/nutrition logic (per-100g + 100g default quantity).
10. User edits quantity/nutrition; `save()` calls `FoodRepository.create` (no new save path, no auto-save). Leaving Online search or dismissing the sheet cancels any in-flight search.

## 14. Config-Fetch Behavior

- Endpoint: `<baseUrl>/v1/config`, GET, **no** API key (public).
- Fetched lazily when entering Online search for the first time; never at app startup; never while Local reference mode is active.
- Cached in-memory in `RemoteFoodLookupRepository` with a TTL of 5 minutes; re-fetched when stale; reused across searches within TTL.
- Conservative fallback on any failure/timeout/malformed body: `onlineLookupAvailable=false`, `providers.usda=false`, `features.genericFoodSearch=false`, `safeMode=true`, `minQueryLength=3`.
- No config call per keystroke. No stale config used beyond TTL.
- Injectable monotonic time source (`MonotonicTimeSource`) so TTL tests are deterministic.
- When `onlineFoodLookupEnabled` becomes `false`, no config or search calls occur regardless of cache.

## 15. Generic-Search Flow

- Route: `<baseUrl>/v1/foods/generic?q=<encoded>`, GET, with `X-GymLedger-Key`.
- Query must be trimmed and length ≥ cached/default `minQueryLength` (default 3); below → no remote call.
- Manual submission only: `onValueChange` only updates query; IME Search or "Search online" button triggers the request. No per-keystroke calls. No debounce required.
- Prevent duplicate concurrent submissions (ignore a second submission while one is in flight).
- Leaving Online search mode or dismissing the sheet cancels the in-flight search coroutine, which cancels the OkHttp `Call`.
- Connect/read/write timeout ~5s.
- Decode success envelope; `data.results` may be empty → `Empty`.
- Map each result's `nutritionPer100g` through the nullable-aware mapper.
- A successful search does not persist anything; only Save persists.

## 16. Result-to-Local-Food Mapping

- Filter results with any missing (`null`), negative, or non-finite nutrient (calories/protein/carbs/fat).
- Round valid calories to `Int` (0.5 up, ≥ 0).
- Complete `RemoteFoodLookupResult` → `FoodReference` (id=externalId; name; caloriesPer100g; protein/carbs/fat per 100g; sourceLabel=attribution; gramsPerUnit=null; unitLabel=null).
- Default quantity = 100g; `FoodReferenceCalculator.calculateFromGrams(ref, 100.0)` yields editable per-serving defaults.
- `save()` reuses the existing path: `foodRepository.create(name, caloriesPerServing = calories, servingSize = grams, protein, carbs, fat)`.
- All values remain editable; Save is the only persistence step.

## 17. Error Mapping

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
| `results` empty or all filtered | `Empty` | "No foods found online. Try another term or add it manually." |

- Developer diagnostics (logcat) must omit the API key entirely. Only non-sensitive path/status/code are logged.
- Error `Throwable.message` and DTO `toString()` must not include the key.

## 18. ViewModel/UI-State Changes

- `SmartFoodEntryUiState` gains: `isOnlineAvailable: Boolean`, `minQueryLength: Int`, `onlineMode: Boolean`, `onlineResults: List<RemoteFoodLookupResult>`, `onlineLoading: Boolean`, `onlineError: String?`, `isSearching: Boolean` (concurrent guard). (Keep within one state class; do not split into micro-files.)
- Remote errors surface via `onlineError` in state rather than a snackbar to keep the sheet self-contained.
- Search section: when `onlineFoodLookupEnabled=true`, render a segmented switch "Local reference" / "Online search". When online mode is selected, show remote results list and the source chip on each row.
- Inline states (not mode removal) for: not-configured (missing key), USDA disabled, local safe mode, invalid endpoint, remote temporarily disabled.
- Selected section: reuse `SmartSelectedSection` (already shows `sourceLabel` + "Approximate" chips).
- Quantity/nutrition/save: unchanged.
- If `onlineFoodLookupEnabled=false`, the toggle is omitted and behavior is identical to today.
- Manual submission: IME Search or "Search online" button only; `onValueChange` updates query state only.
- Leaving Online search or dismissing the sheet cancels in-flight search.

## 19. Settings Helper Consistency

- Remove/replace the helper that says an endpoint must be entered to enable lookup. Keep "Leave blank to use default." consistent with blank → default URL.
- If `SettingsViewModel` needs to expose endpoint validity for helper text, add a minimal computed property; do not change `SettingsRepository` or `OnlineAssistanceSettings`.
- `SettingsScreen` may update helper text only to resolve the contradiction.

## 20. Cache Decision

- No Room lookup cache in Phase 17F.
- Unconfirmed suggestions are ephemeral (in-memory only).
- Worker D1 caches provider responses.
- Confirmed results become normal local `Food` rows via `FoodRepository.create`.
- The remote domain model remains compatible with a future local lookup cache.
- No fake or hidden Room cache is implemented now.

## 21. Test Plan by Layer

Transport/client (`OkHttpFoodLookupClientTest`, fake `Call.Factory`/`FakeCall`):
- URL: `<baseUrl>/v1/foods/generic?q=egg` with proper percent-encoding.
- API key header present on generic route; URL has no key.
- Config route `<baseUrl>/v1/config` has no key header.
- Success body decodes including `nutritionPer100g` with complete nutrients.
- Empty `results` → `Empty`.
- 400 → `InvalidQuery`; 401 → `Unauthorized`; 429 → `BudgetExceeded`; 503 with each known code → mapped; `provider_error` body → `ProviderError`.
- Malformed body → `MalformedResponse`.
- Cancellation-aware bridge cancels the `Call` when the coroutine is cancelled.

Endpoint validation (`EndpointValidatorTest`):
- Blank → default URL.
- Valid HTTPS → normalized with trailing slash.
- `http://` → invalid.
- Userinfo → invalid.
- Query → invalid.
- Fragment → invalid.
- Missing trailing slash → normalized.

Config (`RemoteFoodLookupRepositoryTest`):
- Config fetch failure/malformed → conservative fallback.
- Config success with `onlineLookupAvailable && providers.usda && features.genericFoodSearch && !safeMode` → available.
- `safeMode=true` → unavailable.
- `features.genericFoodSearch=false` → unavailable.
- `providers.usda=false` → unavailable.
- `minQueryLength` value honored.
- Cache reused within TTL, re-fetched when stale (fake time source).

Repository / gating (`RemoteFoodLookupRepositoryTest`):
- `onlineFoodLookupEnabled=false` → no config/search call.
- Key blank → unavailable, no call.
- `usdaEnabled=false` → unavailable, no call.
- `safeModeEnabled=true` → unavailable, no call.
- Endpoint blank → default URL used.
- Endpoint non-blank invalid → unavailable, no call.
- Query shorter than `minQueryLength` → no call.
- Valid query → `Success`/`Empty`/`Error` mapping.

ViewModel (`SmartFoodEntryViewModelRemoteTest`, fake repo + fake settings flow):
- Online setting disabled → toggle absent, no remote call.
- Enabled missing key → toggle visible, inline "not configured", no call.
- Enabled `usdaEnabled=false` → inline, no call.
- Enabled `safeModeEnabled=true` → inline, no call.
- Enabled invalid endpoint → inline, no call.
- Enabled valid, remote disabled → inline "temporarily disabled", manual usable.
- Too-short query → no remote call.
- Valid query → loading/success/empty/error states.
- Selecting a remote result prefills editable fields.
- Editable fields remain editable; no save until explicit call.
- Manual flow available after remote failure.
- Leaving Online search or dismissing sheet cancels in-flight search.
- Duplicate concurrent submission prevented.
- Key value never present in UI state or error strings.

Nullable nutrition (`RemoteFoodReferenceMapperTest`):
- Complete nutrients → mapped.
- Incomplete (null) → filtered.
- Mixed → only complete returned.
- Negative → filtered.
- Non-finite → filtered.
- All filtered → empty.
- Calories rounding (0.5 up).

Security (across layers):
- No key in URL, DTOs, error strings; no real network in unit tests.

Settings (if modified):
- Helper text consistent with "Leave blank to use default."
- Custom endpoint validation enforces HTTPS, no userinfo/query/fragment, trailing slash.

## 22. Implementation Order

1. Add OkHttp dependency + INTERNET permission; run clean assembleDebug to confirm empty wiring compiles.
2. Create DTOs + `FoodLookupOutcome`/`FoodLookupError` + `EndpointValidator` + tests.
3. Create `MonotonicTimeSource` + `FoodLookupClient` interface + `OkHttpFoodLookupClient` (Call.Factory seam, cancellation bridge); write `OkHttpFoodLookupClientTest` with `FakeCallFactory`/`FakeCall`.
4. Create `RemoteFoodLookupResult` + `RemoteFoodReferenceMapper` (nullable filtering + calorie rounding) + tests.
5. Create `RemoteFoodLookupRepository` with config cache + tests.
6. Wire `AppContainer` (shared `OkHttpClient` + repository + time source).
7. Extend `SmartFoodEntryViewModel` + factory with online mode + gating + prefill + manual submission + cancellation; add ViewModel tests.
8. Extend `SmartFoodEntryScreen` with online toggle, result rows, source/approximate badges, inline states, manual submission.
9. If needed, update `SettingsViewModel`/`SettingsScreen` helper text for endpoint default consistency; add test.
10. Run full validation; fix first real error only; no unrelated refactors.
11. Manual QA; record results.

## 23. Risks and Build Traps

- `@Serializable` DTOs must use nullable/`default` fields to tolerate minor Worker field changes; decode ignores unknown keys (`Json { ignoreUnknownKeys = true }`). Do **not** enable `isLenient=true`.
- OkHttp bridge must use `Call.enqueue` + `suspendCancellableCoroutine` and cancel the `Call` in `invokeOnCancellation`. Do not use blocking `Call.execute` with `withTimeout` as the cancellation mechanism.
- Always run the bridge on `Dispatchers.IO`; the coroutine suspension handles thread offloading.
- Avoid importing OkHttp logging interceptor in release builds; if used in debug, never log the key header.
- Gating order matters: local gates (setting → usda → safeMode → key → endpoint validation) → remote config gates (onlineLookupAvailable → providers.usda → genericFoodSearch → !safeMode) → query length.
- Don't URL-encode the leading `?`; only encode the query value.
- Don't mutate `FoodReference`/`FoodReferenceCalculator` (out of scope); only construct `FoodReference` instances.
- Don't add a `MockWebServer` dependency — fake `Call.Factory` instead.
- Don't coerce nullable nutrients to zero — filter them.
- Don't silently remove Online search mode when the Worker is disabled — show inline state.
- Don't add barcode fields.
- Add `INTERNET` permission unconditionally so release builds can use the feature once enabled by the user.

## 24. Validation Plan

From repo root:

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

Targeted unit tests:

```bash
./gradlew testDebugUnitTest --tests "com.edu.gymledger.data.remote.*" --tests "com.edu.gymledger.data.repository.lookup.*" --tests "com.edu.gymledger.domain.model.lookup.*" --tests "com.edu.gymledger.feature.nutrition.SmartFoodEntryViewModelRemoteTest" --tests "com.edu.gymledger.feature.settings.SettingsViewModelEndpointValidationTest"
```

Scope gate:

```bash
git status --short --untracked-files=all
git diff --name-status
git diff -- worker/ || true
git diff -- app/src/main/java/com/edu/gymledger/data/db || true
git diff -- app/src/main/java/com/edu/gymledger/domain/model/FoodReference.kt app/src/main/java/com/edu/gymledger/domain/model/FoodReferenceCalculator.kt || true
git diff -- app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt app/src/main/java/com/edu/gymledger/data/repository/SettingsRepository.kt app/src/main/java/com/edu/gymledger/data/repository/OnlineAssistanceSettings.kt || true
```

`worker/`, DB, FoodReference/Calculator, FoodRepository, SettingsRepository, and OnlineAssistanceSettings diffs must be empty.

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

## 25. Manual QA Checklist

- Online assistance OFF: Foods screen and Smart Entry sheet behave as before; no remote UI.
- Online assistance ON, no key: Online search visible with inline "not configured"; manual entry works.
- Online assistance ON, key present, `usdaEnabled=false`: Online search visible with inline "temporarily unavailable"; manual entry works.
- Online assistance ON, key present, `safeModeEnabled=true`: Online search visible with inline "temporarily unavailable"; manual entry works.
- Online assistance ON, key present, invalid custom endpoint: Online search visible with inline "invalid endpoint"; manual entry works.
- Online assistance ON, key present, endpoint blank (default URL), Worker default (safe mode): Online search visible; search reports "temporarily disabled"; manual entry works.
- Temporarily enable Worker generic search (only if user explicitly approves): enter `egg`; results appear; each shows source + approximate; selecting prefills editable fields.
- Edit values, then Save: local `Food` created with edited values; appears in Foods list.
- Discard (Cancel) after selecting a remote result: no `Food` created.
- App restart: settings persisted; no remote call at startup; Smart Entry resets to local mode.
- Airplane mode: remote search fails with "Couldn't reach …"; manual + local reference entry usable.
- Rotation/navigation: sheet state behaves per existing implementation; no crash.
- No barcode field/button anywhere.
- Verify `worker/` has zero diff after the phase.
- Settings helper text no longer says an endpoint must be entered when lookup is enabled.

## 26. Model Routing for Implementation, Debugging, and Review

- Implementation (UX integration): OpenCode Go Qwen3.6 Plus or Kimi K2.6.
- Android networking/runtime (OkHttp timeouts, threading, Call.Factory, permission, manifest): Gemini Android Studio.
- Pre-commit review: OpenCode Go DeepSeek V4 Pro.
- Final/visual review (optional screenshots): ChatGPT or Codex.
- Do not escalate simple build/test specifics to premium models.

## 27. Builder Preflight Prompt

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
- SettingsRepository/OnlineAssistanceSettings fields: onlineFoodLookupEnabled, foodLookupEndpoint, foodLookupApiKey, usdaEnabled, openFoodFactsEnabled, safeModeEnabled.
- AppContainer wiring pattern and settingsRepository lateinit.
- AndroidManifest has no INTERNET permission yet.
- SettingsScreen helper text that says an endpoint must be entered when lookup is enabled (must be fixed for "Leave blank to use default" consistency).

Verify the plan covers:
- Endpoint default semantics: blank -> default production Worker URL; non-blank -> validate absolute HTTPS, no userinfo/query/fragment, trailing slash; invalid -> unavailable.
- All local gates: onlineFoodLookupEnabled, usdaEnabled, safeModeEnabled=false, API key non-blank, endpoint blank/default or valid.
- Remote config gates: onlineLookupAvailable, providers.usda, features.genericFoodSearch, safeMode=false, query length >= minQueryLength.
- openFoodFactsEnabled is unused this phase.
- Nullable nutrient filtering: null/negative/non-finite filtered; calories rounded to Int (0.5 up); Empty if all filtered; FoodReference schema unchanged.
- Manual search submission: onValueChange updates query only; IME Search or button triggers; no per-keystroke calls; duplicate concurrent prevented; cancellation on leave/dismiss.
- Call.Factory test seam: OkHttpFoodLookupClient receives okhttp3.Call.Factory; production uses shared OkHttpClient; tests use FakeCallFactory/FakeCall; no MockWebServer.
- Cancellation-aware OkHttp: Call.enqueue + suspendCancellableCoroutine; cancel Call on coroutine cancellation; ~5s timeouts.
- Strict JSON: ignoreUnknownKeys=true; isLenient=false.
- Config fetch only when entering Online search; 5-min in-memory cache; injectable monotonic time source; conservative fallback.
- Online mode visible whenever onlineFoodLookupEnabled=true; failures show inline states; mode not silently removed.
- Settings helper consistency; SettingsViewModel/SettingsScreen may be modified; SettingsRepository/OnlineAssistanceSettings are not.
- No Room/Worker/barcode changes; no FoodReference/FoodReferenceCalculator changes; no navigation changes.

Output the Builder Preflight format (discovery, files to create/modify/not touch, summary, validation command, quality gates, manual QA, risks/mismatches, approval status). Stop and wait for approval.
```

## 28. Reviewer Prompt

```text
Review the Phase 17F diff only. Do not edit.

Read:
- docs/CURRENT_PHASE.md
- docs/IMPLEMENTATION_PLAN.md
- docs/AI_WORKFLOW.md
- git diff

Check:
1. Only generic remote search is implemented; no barcode/scanning/recents/favorites; openFoodFactsEnabled unused.
2. OkHttp is the only new dependency.
3. Endpoint semantics: blank -> default URL; non-blank validated (absolute HTTPS, no userinfo/query/fragment, trailing slash); invalid -> unavailable; API key only sent to resolved validated origin.
4. All local gates enforced: onlineFoodLookupEnabled, usdaEnabled, safeModeEnabled=false, API key non-blank, endpoint blank/default or valid.
5. All remote config gates enforced: onlineLookupAvailable, providers.usda, features.genericFoodSearch, safeMode=false, query length >= minQueryLength.
6. Nullable nutrient filtering: null/negative/non-finite filtered; null not coerced to zero; calories rounded to Int (0.5 up); Empty if all filtered; FoodReference/Room schema unchanged.
7. Manual search submission: onValueChange updates query only; IME Search or button triggers; no per-keystroke calls; duplicate concurrent prevented; cancellation on leave/dismiss.
8. Call.Factory test seam: OkHttpFoodLookupClient receives okhttp3.Call.Factory; tests use FakeCallFactory/FakeCall; no MockWebServer; no real network.
9. Cancellation-aware OkHttp: Call.enqueue + suspendCancellableCoroutine; Call cancelled on coroutine cancellation; ~5s timeouts; no blocking Call.execute with withTimeout as the cancellation mechanism.
10. Strict JSON: ignoreUnknownKeys=true; isLenient=false.
11. Online mode visible whenever onlineFoodLookupEnabled=true; failures show inline user-safe states; mode not silently removed.
12. Config fetch only when entering Online search; 5-min cache; injectable monotonic time source; conservative fallback; no startup fetch.
13. Settings helper text consistent with "Leave blank to use default"; SettingsViewModel/SettingsScreen changes limited to helper consistency.
14. API key user-entered, sent only as X-GymLedger-Key header, never in URL/DTOs/logs/errors.
15. Offline-first preserved; manual/local entry unaffected by all disabled/error states.
16. No Room schema change; no Worker changes; no new routes; no FoodReference/FoodReferenceCalculator changes.
17. Package com.edu.gymledger only.
18. UI text English; source + approximate badges present for remote results.
19. No auto-save; explicit Save only.
20. Tests cover transport, config, gating, nullable nutrition, ViewModel, settings helper, security invariants.

Return PASS, PASS_WITH_NOTES, or BLOCKED with blockers, non-blocking concerns, scope creep, risky files, and a suggested commit message.
```

## 29. Commit-Ready Gate

Before commit:

```bash
git status --short --untracked-files=all
git diff --stat
git diff -- worker/ || true
git diff -- app/src/main/java/com/edu/gymledger/data/db || true
git diff -- app/src/main/java/com/edu/gymledger/domain/model/FoodReference.kt app/src/main/java/com/edu/gymledger/domain/model/FoodReferenceCalculator.kt || true
git diff -- app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt app/src/main/java/com/edu/gymledger/data/repository/SettingsRepository.kt app/src/main/java/com/edu/gymledger/data/repository/OnlineAssistanceSettings.kt || true
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
git grep -nE "X-GymLedger-Key:" -- app/src || true
grep -R -n "com\.gymledger" app/src || true
```

Commit only if validation passes, manual QA done, scope clean, no secret value present, no worker/DB/FoodReference/FoodRepository/SettingsRepository diff, reviewer PASS/PASS_WITH_NOTES.

## 30. Stop Conditions

- Phase 17E.4 absent from branch history.
- Dirty repo unexpectedly.
- First real build/test/lint error not fixed within two local attempts (escalate).
- Real API key value found in source/tests/docs/logs.
- Worker code modified.
- Room schema/DB version changed.
- `FoodReference` or `FoodReferenceCalculator` modified.
- Barcode UI introduced.
- Remote results auto-save without confirmation.
- Any dependency beyond OkHttp appears necessary.
- `isLenient=true` is enabled in JSON parsing.
- `MockWebServer` or any extra test dependency is added.
- Reviewer returns BLOCKED and the blocker cannot be resolved within scope.

---