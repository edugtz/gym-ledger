# Phase 17C — Smart Food Entry Local Foundation — Implementation Plan

## 1. Objective

Add a local smart food entry foundation that lets users search common reference foods, enter quantity, and automatically calculate approximate calories and macros.

This phase makes food entry feel closer to a real nutrition app instead of forcing the user to manually type every macro value.

This phase is strictly local-only.

No backend.
No external APIs.
No Cloudflare Worker.
No barcode.
No meal logging.

---

## 2. Product Quality Goal

The user should be able to:

1. Open Foods.
2. Choose Smart entry.
3. Search a common food such as `egg` or `huevo`.
4. Select `Whole egg, large`.
5. Enter `10` units or a gram amount.
6. See approximate calories, protein, carbs, and fat calculated automatically.
7. Edit the calculated name/macros if needed.
8. Save the result as a custom food.

The flow should feel like a calculator-assisted nutrition feature, not another CRUD form.

Calculated values must be visibly labeled as approximate.

All calculated values must be editable before saving.

---

## 3. Scope

### In scope

```text
local reference food model
local curated food seed list
local reference food repository
pure macro calculator
SmartFoodEntryViewModel
SmartFoodEntryViewModelFactory
SmartFoodEntryScreen
FoodsScreen integration
calculator/search tests
save calculated food through existing FoodRepository
```

### Out of scope

```text
network calls
OkHttp
Retrofit
Cloudflare Worker
USDA API
Open Food Facts API
barcode scanner
manual barcode lookup
meal logging
nutrition day screen
charts
macro goals
analytics
Room schema changes
new database entities
database version bump
Body/Workout/Routine/Exercise changes
Gradle changes
```

---

## 4. Architecture Decision

Use a static local Kotlin reference list wrapped by a small repository.

Do not create a Room table in this phase.

Reason:

```text
The initial reference food list is small.
The list is curated and app-owned.
No user-editable reference foods are needed yet.
No migration/version bump is needed.
Static data is enough for v1 personal use.
Future online lookup phases will introduce a broader FoodLookupRepository.
```

Use this structure:

```text
FoodReferenceSeed
  ↓
FoodReferenceRepository
  ↓
SmartFoodEntryViewModel
  ↓
SmartFoodEntryScreen
  ↓
FoodRepository.create(...)
```

Room remains untouched.

---

## 5. Files to Create

### 5.1 `app/src/main/java/com/edu/gymledger/domain/model/FoodReference.kt`

Create a domain model for local reference foods.

Recommended shape:

```kotlin
data class FoodReference(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val caloriesPer100g: Int,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val defaultUnitLabel: String? = null,
    val gramsPerUnit: Double? = null,
    val sourceLabel: String = "Local reference"
)
```

Rules:

```text
id must be stable and unique
name is user-facing English
aliases may include English and Spanish search terms
macros are per 100 g
gramsPerUnit is optional
sourceLabel should remain "Local reference" in this phase
```

---

### 5.2 `app/src/main/java/com/edu/gymledger/data/reference/FoodReferenceSeed.kt`

Create a curated static list of common foods.

Initial target: 40–60 foods.

Include common items such as:

```text
Whole egg, large
Egg white
Chicken breast, cooked
Ground beef, cooked
Salmon, cooked
Tuna, canned in water
White rice, cooked
Brown rice, cooked
Oats, dry
Pasta, cooked
Corn tortilla
Flour tortilla
White bread
Whole wheat bread
Potato, cooked
Sweet potato, cooked
Banana
Apple
Orange
Avocado
Broccoli
Spinach
Carrot
Tomato
Black beans, cooked
Pinto beans, cooked
Lentils, cooked
Greek yogurt, plain
Milk, whole
Milk, low-fat
Cottage cheese
Cheddar cheese
Peanut butter
Olive oil
Almonds
Tofu, firm
```

Search aliases should include Spanish where useful:

```kotlin
aliases = listOf("egg", "whole egg", "large egg", "huevo", "huevo entero")
```

Do not claim values are exact.

Do not include external provider names such as USDA or Open Food Facts in the UI/source label yet.

---

### 5.3 `app/src/main/java/com/edu/gymledger/data/repository/FoodReferenceRepository.kt`

Create a small repository around the static seed list.

Responsibilities:

```text
return all reference foods
search by name or aliases
case-insensitive matching
trim query
blank query returns a limited useful list or all references
```

Suggested API:

```kotlin
class FoodReferenceRepository(
    private val references: List<FoodReference> = FoodReferenceSeed.all
) {
    fun listAll(): List<FoodReference>

    fun search(query: String): List<FoodReference>
}
```

Search rules:

```text
blank query can return all references
search name and aliases
case-insensitive
stable alphabetical ordering by name
```

---

### 5.4 `app/src/main/java/com/edu/gymledger/domain/model/FoodReferenceCalculator.kt`

Create pure calculation logic.

Suggested shape:

```kotlin
object FoodReferenceCalculator {
    data class CalculatedFood(
        val referenceId: String,
        val referenceName: String,
        val suggestedName: String,
        val quantityLabel: String,
        val totalGrams: Double,
        val calories: Int,
        val protein: Double,
        val carbs: Double,
        val fat: Double,
        val sourceLabel: String,
        val isApproximate: Boolean = true
    )

    fun calculateFromGrams(
        reference: FoodReference,
        grams: Double
    ): CalculatedFood

    fun calculateFromUnits(
        reference: FoodReference,
        units: Double
    ): CalculatedFood
}
```

Calculation rules:

```text
grams must be finite and > 0
units must be finite and > 0
calculateFromUnits requires gramsPerUnit
totalGrams = units * gramsPerUnit
multiplier = totalGrams / 100.0
calories = round(reference.caloriesPer100g * multiplier)
protein = reference.proteinPer100g * multiplier
carbs = reference.carbsPer100g * multiplier
fat = reference.fatPer100g * multiplier
```

Use `roundToInt()` for calories.

Round displayed macros to a readable value in UI, preferably one decimal.

Do not truncate calories with `toInt()`.

Avoid bad pluralization by using labels like:

```text
10 × large egg
```

instead of trying to generate perfect English plurals.

---

### 5.5 `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModel.kt`

Create ViewModel for the smart entry flow.

It should depend on:

```text
FoodRepository
FoodReferenceRepository
```

Suggested UI state:

```kotlin
data class SmartFoodEntryUiState(
    val searchQuery: String = "",
    val referenceResults: List<FoodReference> = emptyList(),
    val selectedReference: FoodReference? = null,

    val quantityInUnits: String = "",
    val quantityInGrams: String = "",

    val calculatedFood: FoodReferenceCalculator.CalculatedFood? = null,

    val editedName: String = "",
    val editedCalories: String = "",
    val editedProtein: String = "",
    val editedCarbs: String = "",
    val editedFat: String = "",

    val validationMessage: String? = null,
    val saveSucceeded: Boolean = false
)
```

Required methods:

```text
updateSearchQuery(query)
selectReference(reference)
updateQuantityInUnits(value)
updateQuantityInGrams(value)
updateEditedName(value)
updateEditedCalories(value)
updateEditedProtein(value)
updateEditedCarbs(value)
updateEditedFat(value)
saveAsCustomFood()
clearSaveSucceeded()
dismissValidationMessage()
reset()
```

Behavior:

```text
search filters local FoodReferenceRepository
selecting reference initializes editedName
quantity update recalculates preview
calculated values populate editable fields
edited values override calculated values before save
save calls existing FoodRepository.create(...)
save success emits saveSucceeded = true
```

Validation:

```text
selected reference required
quantity required
quantity must be finite and > 0
edited name cannot be blank
calories must be integer >= 0
protein/carbs/fat must be finite and >= 0
do not save invalid values
show user-facing English validation message
```

---

### 5.6 `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryViewModelFactory.kt`

Create a standard ViewModel factory.

Constructor:

```kotlin
class SmartFoodEntryViewModelFactory(
    private val foodRepository: FoodRepository,
    private val foodReferenceRepository: FoodReferenceRepository
) : ViewModelProvider.Factory
```

---

### 5.7 `app/src/main/java/com/edu/gymledger/feature/nutrition/SmartFoodEntryScreen.kt`

Create the smart entry UI as a modal bottom sheet content or composable used by a modal sheet from `FoodsScreen`.

Sections:

#### Search section

```text
title: Smart food entry
subtitle/helper: Search common foods and calculate approximate macros.
search field: Search foods
results list: local reference foods
empty state: No matching reference foods
```

#### Selected food section

Show selected reference:

```text
name
source label: Local reference
approximate label
per 100 g summary if useful
```

#### Quantity section

If `gramsPerUnit != null`:

```text
Quantity
Units field
Grams field
helper: 1 × large egg ≈ 50 g
```

If `gramsPerUnit == null`:

```text
Quantity
Grams field only
```

Units and grams should sync:

```text
editing units updates grams
editing grams updates calculated values
if gramsPerUnit exists, grams can optionally update units display
```

Keep sync simple and avoid cursor bugs.

#### Calculated preview section

Label clearly:

```text
Approximate nutrition
```

Show editable fields:

```text
Name
Calories
Protein
Carbs
Fat
```

The user must be able to edit all fields before saving.

Show helper:

```text
Approximate values from local reference. Review before saving.
```

#### Actions

```text
Save as custom food
Cancel
```

Primary action should be disabled or show validation if required values are missing.

All user-facing UI text must be English.

---

## 6. Files to Modify

### 6.1 `app/src/main/java/com/edu/gymledger/app/AppContainer.kt`

Add:

```text
foodReferenceRepository
```

Use static repository:

```kotlin
val foodReferenceRepository: FoodReferenceRepository by lazy {
    FoodReferenceRepository()
}
```

No context required.

No DataStore changes.

No database changes.

---

### 6.2 `app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsScreen.kt`

Add entry point for Smart Entry.

Do not make it a tiny text button floating below the FAB.

Preferred UX:

```text
Add a visible action card or prominent button near the top of FoodsScreen:
- Smart entry
- Manual food
```

Acceptable approach:

```text
Keep existing FAB/manual add behavior.
Add a prominent "Smart entry" button/card above the food list or near the search/add area.
```

Avoid:

```text
tiny text button below FAB
hidden overflow action
changing the whole screen into a generic form
```

FoodsScreen should:

```text
open SmartFoodEntryScreen in a ModalBottomSheet
create SmartFoodEntryViewModel using SmartFoodEntryViewModelFactory
pass AppContainer.foodRepository
pass AppContainer.foodReferenceRepository
close sheet on save success
refresh existing foods list if needed
```

Do not modify navigation unless discovery proves it is necessary.

---

## 7. Files Not Approved

Do not modify:

```text
FoodRepository.kt
FoodDao.kt
FoodEntity.kt
GymLedgerDatabase.kt
FoodRepositoryTest.kt unless adding independent tests requires reference
SettingsRepository.kt
SettingsScreen.kt
Body feature files
Workout feature files
Routine feature files
Exercise feature files
Navigation files
Gradle files
worker/ files
```

No Room schema changes.

No new entities.

No database version bump.

---

## 8. Tests to Add

Add unit tests for pure logic.

### 8.1 `app/src/test/java/com/edu/gymledger/domain/model/FoodReferenceCalculatorTest.kt`

Test cases:

```text
calculate 10 large eggs using gramsPerUnit = 50 g
calculate by grams
reject zero grams
reject negative grams
reject NaN/infinite values
calculate calories with rounding, not truncation
```

### 8.2 `app/src/test/java/com/edu/gymledger/data/repository/FoodReferenceRepositoryTest.kt`

Test cases:

```text
blank query returns references
search by English name
search by English alias
search by Spanish alias
search is case-insensitive
unknown query returns empty list
results are stable/sorted
```

No Android instrumentation tests required.

---

## 9. Implementation Order

1. Create `FoodReference.kt`.
2. Create `FoodReferenceSeed.kt`.
3. Create `FoodReferenceRepository.kt`.
4. Create `FoodReferenceCalculator.kt`.
5. Add calculator tests.
6. Add repository tests.
7. Create `SmartFoodEntryViewModel.kt`.
8. Create `SmartFoodEntryViewModelFactory.kt`.
9. Create `SmartFoodEntryScreen.kt`.
10. Modify `AppContainer.kt`.
11. Modify `FoodsScreen.kt`.
12. Run full validation.
13. Perform manual QA.

---

## 10. Risks and Build Traps

### Overengineering

Do not add Room reference food entities.

Do not create a generic food database architecture in this phase.

### UI becoming CRUD

The UI must be calculator-first:

```text
search → select → quantity → calculated preview → edit/save
```

not:

```text
enter name/calories/protein/carbs/fat manually again
```

### Cursor/sync issues

Units and grams are linked. Keep the sync simple.

If cursor bugs appear, prefer `String` state in ViewModel and update only the counterpart field when the user changes input.

### FoodRepository signature mismatch

Builder must inspect actual `FoodRepository.create()` signature before implementing save.

If the actual signature differs from the plan, stop and report.

### Naming

Suggested default saved food name:

```text
10 × large egg
500 g chicken breast, cooked
```

Name must be editable.

### Approximation honesty

Every calculated preview must clearly say approximate.

Do not imply exact nutrition.

---

## 11. Validation Rules

Functional validation:

```text
Search by name works.
Search by alias works.
Spanish aliases work.
Selecting reference food works.
Units input works when gramsPerUnit exists.
Grams input works for all reference foods.
Calculated macros update.
Calculated values are editable.
Calculated name is editable.
Invalid inputs do not save.
Save creates a custom food through FoodRepository.
Saved food appears in Foods list.
Saved food persists after app restart.
No network calls exist.
No database schema changes exist.
```

---

## 12. Quality Gates

### Scope gate

```bash
git diff --name-status
git diff --stat
git diff --check
```

Expected touched areas:

```text
domain/model food reference/calculator
data/reference seed
data/repository food reference repository
feature/nutrition smart entry
AppContainer
tests
```

### Package gate

```bash
grep -R -nE "package com\.gymledger\b|import com\.gymledger\b" app/src/main/java app/src/test/java || true
```

Expected: no results.

### No-network gate

```bash
grep -R -nE "OkHttpClient|Retrofit|HttpURLConnection|java\.net\.URL|java\.net\.HttpURLConnection|openfoodfacts\.org|fdc\.nal\.usda|fetch\(" app/src/main/java || true
```

Expected: no results.

### No schema change gate

```bash
git diff -- app/src/main/java/com/edu/gymledger/data/db/entity app/src/main/java/com/edu/gymledger/data/db/dao app/src/main/java/com/edu/gymledger/data/db/GymLedgerDatabase.kt
```

Expected: no diff.

### Approximate label gate

```bash
grep -R -ni "approximate" app/src/main/java/com/edu/gymledger/feature/nutrition || true
```

Expected: Smart entry UI includes approximate language.

---

## 13. Validation Command

Run full gate:

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

---

## 14. Manual QA

1. Open Foods screen.
2. Confirm Smart entry action is visible.
3. Tap Smart entry.
4. Search `egg`.
5. Confirm `Whole egg, large` appears.
6. Search `huevo`.
7. Confirm `Whole egg, large` appears through alias.
8. Select `Whole egg, large`.
9. Confirm units and grams fields appear.
10. Enter `10` units.
11. Confirm grams becomes approximately `500`.
12. Confirm calculated nutrition appears.
13. Confirm copy says approximate.
14. Edit food name.
15. Edit calories.
16. Edit protein/carbs/fat.
17. Tap Save as custom food.
18. Confirm sheet closes.
19. Confirm new custom food appears in Foods list.
20. Close and reopen app.
21. Confirm saved food persists.
22. Test a food without unit support using grams.
23. Test blank quantity.
24. Test negative quantity.
25. Test huge quantity.
26. Confirm invalid values do not crash or save.
27. Confirm no network behavior exists.
28. Confirm manual Add food flow still works.

---

## 15. Recommended AI Route

Recommended:

```text
Planner/review:
ChatGPT or Qwen3.6 35B

Calculator/repository/tests:
Devstral 6bit or Qwen Coder 5bit

SmartFoodEntryScreen UI:
OpenCode Go Qwen3.6 Plus
Kimi K2.6 only if UI feels CRUD/cheap

Android runtime/Compose issue:
Gemini Android Studio

Final review:
Qwen27 / DeepSeek Pro / ChatGPT screenshot review
```

Do not use backend/cloud tools for this phase.

---

## 16. Builder Preflight Prompt

Builder preflight only. Do not edit files yet.

Read:

```text
AGENTS.md
docs/CURRENT_PHASE.md
docs/IMPLEMENTATION_PLAN.md
docs/ARCHITECTURE.md
app/src/main/java/com/edu/gymledger/domain/model/Food.kt
app/src/main/java/com/edu/gymledger/data/db/entity/FoodEntity.kt
app/src/main/java/com/edu/gymledger/data/repository/FoodRepository.kt
app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsScreen.kt
app/src/main/java/com/edu/gymledger/feature/nutrition/FoodsViewModel.kt
app/src/main/java/com/edu/gymledger/app/AppContainer.kt
```

Discovery tasks:

1. Report current Food domain model fields.
2. Report current FoodEntity fields.
3. Report current FoodRepository.create() signature.
4. Report current FoodsScreen structure and existing add behavior.
5. Report current FoodsViewModel methods and state.
6. Confirm no existing FoodReference files exist.
7. List exact files you intend to create and why.
8. List exact files you intend to modify and why.
9. List exact tests you intend to create and why.
10. List files you will not touch.
11. Report validation command.
12. Report quality gates.
13. Report any conflicts between this plan and actual repo.

Stop and wait for approval before editing.

Hard rules:

```text
Do not call network.
Do not add OkHttp.
Do not add Retrofit.
Do not add Cloudflare Worker code.
Do not call USDA or Open Food Facts.
Do not add barcode lookup.
Do not modify FoodRepository/FoodDao/FoodEntity/GymLedgerDatabase.
Do not add Room entities.
Do not add schema changes.
Do not modify Body/Workout/Routine/Exercise features.
Use static Kotlin list for reference food seed data.
All calculated values must be visibly labeled approximate.
All calculated values must be editable before saving.
Keep package com.edu.gymledger only.
Keep all UI text in English.
Do not commit.
```

Validation:

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

---

## 17. Suggested Commit

```text
feat: add smart food entry with local reference foods
```
