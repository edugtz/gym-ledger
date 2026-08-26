# Phase 17G — Manual Barcode Lookup Implementation Plan

## Status

**COMPLETE**

Phase:

```text
17G — Manual Barcode Lookup
```

Implementation branch:

```text
17g-manual-barcode-lookup
```

Base branch:

```text
dev
```

Functional QA: PASS.

Real-device validation covered:

- barcode entry/navigation
- unavailable states
- barcode validation
- real Open Food Facts lookup
- UPC/EAN GTIN-equivalent normalization
- `success_with_warnings`
- `not_found`
- nameless product → Empty/Create manually
- incomplete/100 ml nutrition → reviewable/manual-only, no fabricated per100g
- usable per100g result → Use product
- quantity recalculation
- edit/save as custom food
- saved Food appears in Foods
- human-readable Open Food Facts attribution/copy

An OFF interoperability blocker discovered during QA was fixed separately in:

```text
hotfix/off-barcode-normalization
```

That hotfix was independently reviewed, merged to dev, deployed, and validated against real barcodes before Functional QA resumed.

---

## 1. Objective

Add manual typed/pasted packaged-food barcode lookup to Smart Food Entry.

The user must be able to:

```text
open Smart Food Entry
→ choose Barcode
→ type or paste a barcode
→ explicitly submit lookup
→ receive a packaged-food result from the GymLedger Worker
→ review product identity and nutrition
→ choose Use product
→ edit quantity/name/nutrition
→ explicitly save a normal local Food
```

If the barcode is valid but no product is found:

```text
No product found
→ Create manually
→ existing manual Food creation flow
```

Phase 17G is Android integration only.

The Cloudflare Worker barcode endpoint already exists and is deployed.

Do not modify Worker behavior in this phase.

---

## 2. Product Principle

Barcode lookup is an optional convenience feature.

The product remains:

```text
local-first
offline-capable
editable before save
explicit-save only
non-blocking when remote lookup is unavailable
```

A remote Open Food Facts result is a suggestion.

It does not become local GymLedger data until the user explicitly saves it.

Internet is NOT required for core GymLedger functionality.

Manual barcode lookup itself may be unavailable offline.

No Android-side barcode cache or Room barcode persistence is added in Phase 17G.

The existing Worker/D1 cache remains the barcode lookup cache for this phase.

---

## 3. Existing Foundation to Reuse

### Worker

Existing protected endpoint:

```text
GET /v1/foods/barcode/:barcode
```

Authentication:

```text
X-GymLedger-Key
```

Existing Worker behavior already includes:

```text
barcode normalization/validation
Open Food Facts provider
D1 cache
safe mode
online lookup gate
Open Food Facts provider gate
barcode feature gate
daily external-call budget
provider timeout/error mapping
normalized GymLedger-owned DTO
source attribution
approximate flag
```

Do not duplicate or reimplement these systems on Android.

### Android

Reuse the Phase 17F remote stack:

```text
FoodLookupClient
OkHttpFoodLookupClient
RemoteFoodLookupRepository
EndpointValidator
FoodLookupOutcome
FoodLookupError
FoodLookupConfigDto
OnlineAssistanceSettings
SmartFoodEntryViewModel
SmartFoodEntryScreen
existing selected-food editor
FoodReferenceCalculator
FoodRepository
```

The Phase 17F generic USDA flow must continue working unchanged.

---

## 4. AI Route

### Planning / architecture

```text
GPT-5.6 Sol ChatGPT + GitHub
```

### Primary implementation

Preferred first implementation trajectory:

```text
GPT-5.6 Luna Medium
```

Suitable for:

```text
Android integration
ViewModel state design
Compose UX
DTO/repository integration
cross-layer implementation
```

### Technical implementation / debugging alternative

```text
DeepSeek V4 Flash Max
/cloud-ds-max
```

Suitable for:

```text
HTTP client
serialization
error mapping
repository gates
technical debugging
targeted implementation patches
```

Do not have Luna Medium and DeepSeek modify the same task concurrently.

Use one builder trajectory at a time and provide a handoff if changing builders.

### Independent code review

```text
/local-review
Qwen3.8 27B AWQ 5bpw + Lightning MTP
```

Reviewer must not edit.

### Final review

```text
ChatGPT + GitHub
```

Final result:

```text
PASS
PASS_WITH_NOTES
BLOCKED
```

The user owns commits and pushes.

---

## 5. Scope

Implement only Android manual barcode lookup.

### In scope

```text
manual barcode input
paste support through normal TextField behavior
barcode format validation
barcode-specific remote DTOs
FoodLookupClient barcode request
OkHttp barcode request
Worker error mapping
Open Food Facts / barcode availability gates
barcode lookup repository flow
Smart Food Entry barcode mode
explicit submit only
result review card
product metadata display
source attribution
Approximate attribution
product selection
reuse of existing editable selected-food flow
explicit local Save
unknown barcode manual-create fallback
incomplete-product safe handling
request cancellation
state/reset behavior
tests
manual runtime QA
```

### Out of scope

```text
CameraX
camera scanner
CAMERA permission
ML Kit
ZXing dependency
automatic barcode scanning
Room schema changes
FoodEntity barcode field
Android barcode cache
barcode history
recents
favorites
Worker changes
D1 changes
provider changes
Open Food Facts text search
image downloads
product images
automatic serving-size parsing
automatic ounce/ml/unit conversion
future Phase 17H work
```

---

## 6. Architecture Decision — Smart Entry Mode

Do NOT add another boolean such as:

```text
barcodeMode
```

Replace the current binary Local/Online state with one explicit mode.

Preferred model:

```kotlin
enum class SmartFoodEntryMode {
    LOCAL,
    ONLINE_SEARCH,
    BARCODE
}
```

SmartFoodEntryUiState should contain:

```text
mode = LOCAL
```

rather than accumulating mutually-dependent booleans.

Expected behavior:

```text
LOCAL
→ existing local reference search

ONLINE_SEARCH
→ existing USDA generic search

BARCODE
→ manual Open Food Facts barcode lookup
```

Changing mode must:

```text
cancel any active remote lookup
clear stale results/errors for the mode being left
never trigger a network request automatically
preserve unrelated local functionality
```

The existing generic USDA behavior must not regress.

---

## 7. Smart Entry Mode UI

Replace the current two-option selector with three explicit choices:

```text
Local reference
Online search
Barcode
```

Prefer a layout that remains safe on narrow phone widths.

`FlowRow` or another wrapping-safe Material layout is preferred over forcing
three chips into a fixed-width Row.

Do not add a camera icon or scanner affordance.

Barcode must mean manual typed/pasted lookup only.

---

## 8. Barcode Input Rules

Add Android-side validation before any network request.

Required barcode normalization:

```text
trim surrounding whitespace
digits only
allowed lengths: 8, 12, 13, 14
preserve leading zeroes
store as String
never convert to Long/Int
do not guess
do not pad
do not remove internal characters
```

Examples:

```text
3017620422003    VALID
012345678905     VALID
12345670         VALID
00012345600012   VALID

1234             INVALID
ABC123           INVALID
12 345678        INVALID
```

Do not add check-digit validation in 17G.

Android validation should mirror the existing Worker contract, not invent a
stricter barcode format.

Preferred implementation:

```text
BarcodeValidator
```

with isolated unit tests.

Invalid barcode:

```text
show local validation error
perform zero network requests
```

---

## 9. No Search-As-You-Type

Barcode lookup must be manually submitted.

Typing or pasting:

```text
onBarcodeChange(...)
```

must only update UI state.

It must NOT call the Worker.

Allowed request triggers:

```text
IME Search
Look up barcode button
```

One explicit submission:

```text
→ at most one active barcode request
```

Prevent duplicate submission while a request is already running.

---

## 10. FoodLookupClient Extension

Extend the existing interface.

Conceptual contract:

```kotlin
suspend fun lookupBarcode(
    baseUrl: String,
    apiKey: String,
    barcode: String
): FoodLookupOutcome<PackagedFoodLookupDataDto>
```

Do not create a second HTTP client.

Do not add Retrofit.

Use the existing OkHttp stack.

---

## 11. OkHttp Barcode Request

Add:

```text
GET /v1/foods/barcode/:barcode
```

Use the existing:

```text
base URL resolution
X-GymLedger-Key
Call.Factory
enqueue
suspendCancellableCoroutine
Call.cancel()
response closing
strict kotlinx.serialization parsing
existing timeout configuration
```

Build the barcode as a path segment safely.

Do not interpolate arbitrary unvalidated text directly into a URL.

Barcode remains a String end-to-end.

---

## 12. Barcode DTO Contract

Add Android DTOs matching the Worker-owned packaged-food response.

Expected data:

```text
barcode
source
attribution
isApproximate

product:
    externalId
    name
    genericName
    brands
    quantity
    servingSize

    nutritionPer100g:
        caloriesKcal
        proteinG
        carbohydrateG
        fatG

    nutritionPerServing:
        caloriesKcal
        proteinG
        carbohydrateG
        fatG
```

Suggested DTO grouping:

```text
PackagedFoodLookupResponseDto
PackagedFoodLookupDataDto
PackagedFoodProductDto
PackagedNutritionDto
```

Do not reuse the USDA generic DTO for packaged-food responses.

Use:

```text
@Serializable
nullable provider-derived nutrients
strict decoding
ignoreUnknownKeys = true
isLenient = false
```

Do not convert missing nutrients to zero.

---

## 13. Error Mapping

Extend Android error handling with a barcode-specific invalid-input result.

Preferred:

```text
FoodLookupError.InvalidBarcode
```

Map Worker:

```text
invalid_barcode
→ InvalidBarcode
```

Preserve existing mappings for:

```text
unauthorized
lookup_disabled
provider_disabled
feature_disabled
budget_exceeded
configuration_error
provider_error
provider_unavailable
provider_rate_limited
provider_timeout
not_found
```

`not_found` should map to:

```text
FoodLookupOutcome.Empty
```

Do not treat unknown valid barcode as malformed input.

---

## 14. Barcode Availability

Do not reuse USDA-specific availability semantics blindly.

Create barcode-specific availability behavior.

It must evaluate local settings:

```text
onlineFoodLookupEnabled
foodLookupApiKey
openFoodFactsEnabled
safeModeEnabled
endpoint validity
```

Then remote config:

```text
!config.safeMode
config.onlineLookupAvailable
config.providers.openFoodFacts
config.features.barcodeLookup
```

Important:

```text
USDA enabled/disabled must NOT gate barcode lookup.
```

Suggested states:

```text
Disabled
NotConfigured
OpenFoodFactsDisabled
SafeMode
InvalidEndpoint
RemoteDisabled
Available
```

A generic refactor of all availability types is allowed only if preflight
proves it is smaller and safer than adding barcode-specific availability.

Do not redesign unrelated 17F behavior merely for abstraction purity.

---

## 15. RemoteFoodLookupRepository

Reuse the existing repository.

Do not create:

```text
BarcodeRepository
OpenFoodFactsRepository
```

Add conceptually:

```text
getBarcodeAvailability(...)
lookupBarcode(...)
```

`lookupBarcode()` must:

```text
validate local configuration
validate barcode
reuse ensureConfig()
evaluate remote config
resolve endpoint
call FoodLookupClient.lookupBarcode()
map DTO → domain result
return Success / Empty / Error
```

Reuse the existing five-minute config cache.

No Android product cache is added.

---

## 16. Packaged Food Domain Model

Do not force the packaged-food DTO directly into UI state.

Introduce a small domain representation if needed, for example:

```text
RemotePackagedFoodResult
```

It should preserve useful review metadata:

```text
barcode
externalId
name
genericName
brands
quantity
servingSize
source
attribution
isApproximate
nutritionPer100g
nutritionPerServing
```

Provider-derived nutrition remains nullable until validation/mapping.

---

## 17. Product-to-FoodReference Mapping

The existing selected-food editor works from FoodReference.

Reuse it.

Create a conservative packaged-food mapper.

A packaged product is eligible for:

```text
Use product
```

only when all required per-100-g values are present and valid:

```text
calories
protein
carbohydrate
fat
```

Rules:

```text
no null → 0
no negative values
no NaN
no infinity
no calorie overflow
usable nonblank product name required
```

Preferred name:

```text
product.name
→ genericName if product.name unavailable
```

Do not invent a product name from arbitrary provider fields.

Convert valid result into existing:

```text
FoodReference
```

with:

```text
sourceLabel = attribution
gramsPerUnit = null
unitLabel = null
```

Do not parse `servingSize` into gramsPerUnit in this phase.

---

## 18. Serving Metadata Policy

Open Food Facts may return strings such as:

```text
30 g
2 cookies (28 g)
1 bottle
1 bar (40 g)
```

Phase 17G must NOT implement heuristic serving parsing.

Use:

```text
quantity
servingSize
nutritionPerServing
```

for review/display only where useful.

The editable/save path remains based on validated per-100-g nutrition.

If per-100-g nutrition is incomplete:

```text
show the product result
explain that complete nutrition is unavailable
disable or omit Use product
offer Create manually
```

Do not silently substitute per-serving values.

Do not convert per-100-ml data into per-100-g.

---

## 19. Barcode Result Review UI

Successful lookup should NOT immediately select/save the product.

Required flow:

```text
lookup
→ result review card
→ explicit Use product
→ editable selected-food screen
```

Suggested result content:

```text
Product name
Brand(s), when available
Quantity, when available
Serving size, when available
Barcode
Calories/macros per 100 g when complete
Open Food Facts attribution
Approximate
```

Actions:

```text
Use product
Create manually
```

`Use product` is enabled only when the product can be mapped safely.

Remote result must never auto-save.

---

## 20. Unknown Barcode Flow

For:

```text
FoodLookupOutcome.Empty
```

display:

```text
No product found for this barcode.
```

Provide:

```text
Create manually
```

Do not fabricate a product.

Do not retry automatically.

Do not query another provider.

Do not convert the barcode into a text-search request.

---

## 21. Manual Create Fallback

Reuse the existing manual `FoodFormSheet`.

Do not build another manual-food editor.

Preferred integration:

```text
SmartFoodEntrySheet
    onManualCreate callback
        ↓
FoodsScreen closes Smart Food Entry
        ↓
opens existing FoodFormSheet
```

Conceptually:

```kotlin
SmartFoodEntrySheet(
    onDismiss = ...,
    onManualCreate = ...
)
```

In FoodsScreen:

```text
close Smart Food Entry
editingFood = null
open normal Add Food sheet
```

No barcode persistence is required in the manual fallback.

---

## 22. SmartFoodEntryViewModel State

Expected barcode state fields may include:

```text
mode
barcodeText
barcodeResult
isBarcodeSearching
hasSubmittedBarcodeLookup
barcodeError
barcodeAvailability
```

Exact names may differ after repository discovery.

Do not duplicate generic-search state unnecessarily.

Remote request jobs should be clearly owned and cancellable.

A single remote-lookup job may be reused for USDA and barcode if that produces
simpler lifecycle semantics.

---

## 23. Barcode State Transitions

### Enter Barcode mode

```text
cancel previous remote request
clear barcode stale result/error
evaluate local gates
fetch/reuse config only when required
determine barcode availability
```

No barcode product request yet.

### Barcode text changes

```text
update barcodeText immediately
clear stale barcode result
clear old lookup error
hasSubmittedBarcodeLookup = false
NO network request
```

### Submit

```text
reject if lookup already active
validate locally
verify availability
start request
show loading
```

### Success

```text
show result review
stop loading
```

### Empty

```text
show not-found state
offer Create manually
stop loading
```

### Error

```text
clear stale result
show actionable message
stop loading
```

### Leave Barcode mode

```text
cancel barcode lookup
clear transient barcode result/error/loading state
```

### Dismiss Smart Entry

```text
cancel remote request
```

---

## 24. User-Facing Error Messages

All Android UI text must remain English.

Examples:

```text
Invalid barcode.
Enter an 8, 12, 13, or 14 digit barcode.

Online lookup isn't configured. Add an API key in Settings.

Open Food Facts is disabled in Settings.

Online lookup isn't available while safe mode is on.

Barcode lookup is temporarily disabled.

No product found for this barcode.

This product doesn't include complete nutrition per 100 g.
You can create it manually instead.

Couldn't reach the food lookup service. Try again.
```

Do not expose:

```text
raw HTTP codes
provider payload
stack traces
API keys
internal Worker configuration
```

---

## 25. Generic USDA Regression Protection

Phase 17F functionality must remain unchanged.

Explicitly verify:

```text
Local reference mode
Online search mode
USDA config gating
generic query validation
manual Search online behavior
IME Search
remote result selection
editable nutrition
explicit Save
scroll behavior
Change/reselection behavior
```

Barcode implementation must not alter generic food lookup semantics.

---

## 26. No Startup Network

Do not fetch:

```text
/v1/config
barcode product
USDA search
```

when Smart Food Entry is not using a remote mode.

Do not perform barcode lookup merely by:

```text
opening Foods
opening Smart Entry
typing
pasting
switching to Barcode mode
```

Switching to Barcode may fetch/reuse `/v1/config` when required to determine
availability.

The product request itself requires explicit submission.

---

## 27. Expected File Areas

Builder preflight must confirm exact files before editing.

Likely production files:

```text
app/src/main/java/com/edu/gymledger/data/remote/FoodLookupClient.kt
app/src/main/java/com/edu/gymledger/data/remote/OkHttpFoodLookupClient.kt
app/src/main/java/com/edu/gymledger/data/remote/FoodLookupOutcome.kt
app/src/main/java/com/edu/gymledger/data/remote/BarcodeValidator.kt
app/src/main/java/com/edu/gymledger/data/remote/dto/PackagedFoodLookupResponseDto.kt

app/src/main/java/com/edu/gymledger/data/repository/lookup/RemoteFoodLookupRepository.kt

app/src/main/java/com/edu/gymledger/domain/model/lookup/RemotePackagedFoodResult.kt
app/src/main/java/com/edu/gymledger/domain/model/lookup/PackagedFoodReferenceMapper.kt

app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModel.kt
app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryScreen.kt
app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsScreen.kt
```

Likely tests:

```text
BarcodeValidatorTest
OkHttpFoodLookupClientTest
RemoteFoodLookupRepositoryTest
PackagedFoodReferenceMapperTest
SmartFoodEntryViewModelTest
```

Existing test files should be extended instead of duplicated where appropriate.

This list is expected scope, NOT automatic authorization.

Builder must inspect the actual repository and report the exact file list in
preflight before editing.

---

## 28. Files That Must Not Change

Unless preflight discovers a genuine blocker and stops for approval:

```text
worker/**
Room entities
FoodEntity
FoodDao
GymLedgerDatabase
database version
migrations
Food.kt
SettingsRepository
OnlineAssistanceSettings
navigation
AndroidManifest camera permissions
Gradle dependencies
FoodReferenceCalculator
```

`FoodReference` itself should not require modification.

No Worker deployment occurs as part of implementation.

---

## 29. Dependency Gate

Expected:

```text
ZERO new runtime dependencies
```

Reuse:

```text
OkHttp
kotlinx.serialization
Compose
coroutines
existing repositories
```

Immediate STOP if implementation proposes:

```text
CameraX
ML Kit
ZXing
Retrofit
Hilt
new barcode library
```

Those belong outside Phase 17G.

---

## 30. Unit Test Requirements

### Barcode validation

Test:

```text
8-digit accepted
12-digit accepted
13-digit accepted
14-digit accepted
leading zero preserved
outer whitespace normalized
blank rejected
letters rejected
internal spaces rejected
unsupported lengths rejected
```

### HTTP client

Test:

```text
correct GET path
barcode preserved as path String
leading zero preserved
X-GymLedger-Key sent
successful packaged DTO decoded
invalid_barcode mapping
unauthorized mapping
lookup_disabled mapping
provider_disabled mapping
feature_disabled mapping
budget_exceeded mapping
configuration_error mapping
provider errors
provider timeout
not_found → Empty
malformed success response
strict parsing
request cancellation
response body closure where testable
```

### Repository

Test local gates:

```text
online disabled
missing API key
Open Food Facts disabled
safe mode
invalid endpoint
invalid barcode
```

Test remote gates:

```text
remote safe mode
onlineLookupAvailable false
providers.openFoodFacts false
features.barcodeLookup false
```

Test:

```text
config cache reused
correct client arguments
valid success mapping
not found
provider error
malformed/incomplete product preserved safely
```

### Mapper

Test:

```text
complete nutrition → FoodReference
missing calories → cannot map
missing protein → cannot map
missing carbs → cannot map
missing fat → cannot map
negative values rejected
nonfinite values rejected
calorie overflow rejected
name fallback to genericName
missing usable name rejected
source/attribution preserved
no null → 0
```

### ViewModel

Test:

```text
default mode LOCAL
switch LOCAL → ONLINE_SEARCH
switch LOCAL → BARCODE
switch mode cancels active lookup

typing barcode updates state immediately
typing performs zero lookup
changing barcode clears stale result
invalid submit performs zero lookup

valid submit starts one lookup
second submit while loading ignored
success shows result
empty shows not-found state
error clears stale result

leaving Barcode cancels lookup
dismiss/reset cancels lookup

Use product maps to existing selected-food flow
selected barcode result remains editable
Change returns from selected flow safely

generic USDA behavior unchanged
local behavior unchanged
```

---

## 31. Automated Validation

Run:

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

Expected:

```text
BUILD SUCCESSFUL
0 test failures
0 test errors
```

Also:

```bash
git diff --check
git status --short
```

Scope checks:

```bash
git diff --name-only origin/dev...HEAD
git diff -- worker/
```

Expected:

```text
no worker diff
no Room schema diff
no database migration
no camera permission
no new barcode/scanner dependency
```

---

## 32. Independent Local Review

After builder validation, run:

```text
/local-review
```

Reviewer:

```text
Qwen3.8 27B AWQ 5bpw + Lightning MTP
read-only
independent trajectory
```

Review specifically:

```text
scope creep
barcode String preservation
leading-zero handling
no search-as-you-type
request cancellation
duplicate request prevention
strict DTO parsing
provider null handling
no null → 0
availability gates
USDA regression
manual fallback
explicit-save behavior
Room untouched
Worker untouched
no camera/scanner work
```

Reviewer returns:

```text
PASS
PASS_WITH_NOTES
BLOCKED
```

Builder must not be the sole reviewer.

---

## 33. Manual QA — Local / Disabled States

Verify:

```text
PASS — Local reference flow unchanged
PASS — Generic USDA online flow unchanged

PASS — Barcode mode available only through optional online assistance flow
PASS — missing API key handled
PASS — Open Food Facts disabled locally handled
PASS — local safe mode handled
PASS — invalid custom endpoint handled

PASS — remote safe mode handled
PASS — remote online lookup disabled handled
PASS — remote Open Food Facts provider disabled handled
PASS — remote barcode feature disabled handled
```

---

## 34. Manual QA — Barcode Validation

Verify manually:

```text
PASS — 1234 rejected without network
PASS — ABC123 rejected without network
PASS — internal spaces rejected
PASS — 8-digit barcode accepted
PASS — 12-digit barcode accepted
PASS — 13-digit barcode accepted
PASS — 14-digit barcode accepted
PASS — leading zero remains visible and unchanged
```

Do not require every accepted format to exist in Open Food Facts.

This validates input behavior, not provider coverage.

---

## 35. Manual QA — Request Behavior

Verify:

```text
PASS — typing causes no request
PASS — paste causes no request
PASS — IME Search submits
PASS — Look up barcode button submits
PASS — one submission produces one active lookup
PASS — duplicate submit while loading is prevented
PASS — editing barcode clears stale result
PASS — switching mode cancels active lookup
PASS — dismissing sheet cancels active lookup
```

---

## 36. Manual QA — Live Product

Temporarily enable only the Worker functionality required for controlled QA.

Target QA runtime state:

```text
safe_mode=false
online_lookup_enabled=true
open_food_facts_provider_enabled=true
barcode_lookup_enabled=true
```

Generic USDA may remain disabled during barcode-specific QA:

```text
usda_provider_enabled=false
generic_food_search_enabled=false
```

Verify with a known real packaged-food barcode:

```text
PASS — live Worker request succeeds
PASS — product identity is plausible
PASS — barcode preserved
PASS — product name shown
PASS — brand shown when present
PASS — quantity shown when present
PASS — serving size shown when present
PASS — Open Food Facts attribution shown
PASS — Approximate shown
PASS — per-100-g nutrition shown when available
```

Never paste secrets into agent/chat logs.

---

## 37. Manual QA — Product Selection and Save

For a product with complete per-100-g nutrition:

```text
PASS — Use product available
PASS — Use product enters existing selected-food flow
PASS — selected-food screen starts at top
PASS — grams editable
PASS — name editable
PASS — calories editable
PASS — protein editable
PASS — carbs editable
PASS — fat editable
PASS — recalculation behavior works
PASS — Change works
PASS — Cancel/discard does not save
PASS — Save as custom food persists locally
PASS — saved Food survives force-stop/restart
PASS — saved Food remains available offline
```

No remote suggestion auto-saves.

---

## 38. Manual QA — Unknown / Incomplete Product

Unknown valid barcode:

```text
PASS — no crash
PASS — not-found state shown
PASS — no fabricated nutrition
PASS — Create manually available
PASS — Create manually opens existing Food form
```

Incomplete Open Food Facts result:

```text
PASS — missing nutrients are not displayed as zero
PASS — product may still be reviewed
PASS — Use product disabled/omitted if mapping is unsafe
PASS — Create manually available
```

---

## 39. Worker Restoration

After controlled live QA, restore production to conservative defaults.

Required final runtime state:

```text
safe_mode=true
online_lookup_enabled=false
usda_provider_enabled=false
open_food_facts_provider_enabled=false
generic_food_search_enabled=false
barcode_lookup_enabled=false
```

Confirm `/v1/config` again after restoration.

Do not leave the Worker enabled merely because Android QA passed.

---

## 40. Completion Gate

Phase 17G is complete only when all are true:

```text
manual barcode input works
valid format validation works locally
typing does not trigger network
explicit submit works
Open Food Facts lookup works through Worker
product review works
unknown barcode fallback works
incomplete nutrition is handled conservatively
Use product enters existing editable flow
explicit Save creates a normal local Food
offline saved-food behavior remains intact
generic USDA flow has no regression
local reference flow has no regression

no CameraX
no camera permission
no scanner dependency
no Room schema change
no Worker change
no Android barcode cache

full Gradle gate passes
git diff --check passes
manual QA passes
independent Qwen review passes
ChatGPT GitHub review passes
```

---

## 41. Stop Conditions

STOP immediately and report before expanding scope if:

```text
Worker endpoint/DTO differs from documented contract
barcode lookup requires Worker modification
Room schema change appears necessary
FoodEntity barcode persistence appears necessary
Camera/scanner dependency appears necessary
new runtime dependency appears necessary
generic USDA regression cannot be isolated
existing selected-food editor cannot safely consume packaged food
implementation requires heuristic serving parsing
provider null values would need to be coerced to zero
same root blocker survives two attempts
```

Do not solve those by silently broadening Phase 17G.

---

## 42. Builder Preflight

Before editing, builder must read:

```text
AGENTS.md
docs/CURRENT_PHASE.md
docs/IMPLEMENTATION_PLAN.md
docs/AI_WORKFLOW.md
docs/FOOD_LOOKUP_DEPLOYMENT.md
```

Then inspect the actual relevant Android and Worker contract files.

Return BEFORE editing:

```text
1. Exact files to modify/create
2. Why each file is required
3. Existing code that will be reused
4. Any mismatch between this plan and current repository
5. Validation command
6. Quality gate
7. Confirmation that Worker/Room/Camera/dependencies remain untouched
```

STOP and wait for approval.

---

## 43. Suggested Commit

```text
feat: add manual barcode food lookup
```

Do not commit automatically.

The user commits manually after:

```text
automated validation PASS
manual QA PASS
independent review PASS/PASS_WITH_NOTES
```

Then:

```text
push branch
→ ChatGPT GitHub review
→ merge to dev only after PASS
```