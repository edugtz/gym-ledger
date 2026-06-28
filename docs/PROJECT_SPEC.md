# GymLedger — Project Specification

## Product Summary

GymLedger is a personal Android app for tracking gym training, nutrition, body measurements, and local data backups.

The app is local-first and offline-capable. It may use optional online-assisted services when they reduce friction or improve data quality, especially for nutrition lookup.

The app must be installable as an APK.

## App Language

All user-facing Android UI text must be English.

Examples:

- "Workouts"
- "Exercises"
- "Nutrition"
- "Meals"
- "Body Measurements"
- "Import / Export"
- "Settings"
- "Add Set"
- "Save Meal"
- "Estimated calories"
- "This is an approximation. Please review before saving."

## Target Users

Initial target:

- 2 personal users
- independent local data per device
- low traffic
- no public SaaS requirement
- no account system required initially

## Core Product Principles

1. Local data remains the source of truth.
2. App must remain useful without internet.
3. Online-assisted services are optional and disabled by default until configured.
4. No paid API is required for core use.
5. No login is required for personal use.
6. All fetched/calculated values are editable before saving.
7. All estimates are labeled approximate.
8. Manual entry always remains available.
9. Cloud/backend features must be serverless, low-cost, and replaceable.
10. Core flows must not feel like raw CRUD forms.

## Technical Stack

Android:

- Kotlin
- Jetpack Compose
- Room
- DataStore
- Navigation Compose
- Kotlinx Serialization
- Gradle
- OkHttp only when remote lookup phase is active and approved

Backend/cloud, when active:

- Cloudflare Worker
- TypeScript
- Cloudflare D1
- Cloudflare KV only if needed
- USDA FoodData Central
- Open Food Facts

## MVP Scope

### Dashboard

The Dashboard should show:

- Today's calories
- Today's protein
- Today's carbs
- Today's fat
- Progress against macro goals
- Latest body weight
- Latest workout
- Quick actions:
  - New workout
  - Add meal
  - Add body measurement
  - Exercises
  - Foods

### Exercises

The user can:

- Create exercises
- Edit exercises
- Delete unused exercises
- View an exercise list
- Use presets for common exercises
- Store name/category/primary muscle/secondary muscles/equipment/notes where schema supports it

### Workouts

The user can:

- Create workout sessions
- View workout history
- Open workout detail
- Add sets
- Edit sets
- Delete sets
- End a workout session
- Start from routines

Each set can store:

- exercise
- set index
- reps
- weight
- RPE
- RIR
- notes

### Routines

The user can:

- Create routines
- Edit routines
- Delete routines
- Add exercises to routines
- Define targets where schema supports them
- Start a workout from a routine

### Nutrition

The user can:

- Create foods manually
- Edit foods
- Search saved foods
- Use local reference foods for common food calculations
- Use optional online food lookup when configured
- Use USDA-backed generic foods through Worker
- Use Open Food Facts-backed product/barcode lookup through Worker
- Create meals
- Add foods to meals
- Add manual meal items
- Edit calories and macros
- View daily nutrition summary

Food data includes, where supported by current schema:

- name
- brand if supported
- serving size / serving grams
- calories per serving
- protein per serving
- carbs per serving
- fat per serving
- source/attribution if lookup-derived
- approximate flag if calculated/fetched

### Meal Photos

The user can:

- Take or select a meal photo
- Attach the photo to a meal
- View/remove the photo
- Use an approximate editable estimate if that phase is active

Photo/AI estimation must be optional and editable.

## Out of Scope Initially

- Required login
- Required cloud sync
- public multi-user SaaS
- subscriptions/payments
- social/community features
- paid API dependency for core use
- cloud AI as required runtime
