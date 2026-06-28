# GymLedger — Acceptance Criteria

## Global MVP Acceptance

The app is MVP-usable when:

- Installs correctly as APK.
- Opens without crash.
- Works without internet for core local flows.
- Has no required login.
- Has no required cloud sync.
- Does not require paid runtime APIs.
- Data persists after closing and reopening.
- Allows creating/managing exercises.
- Allows logging workouts and sets with reps, weight, RPE/RIR.
- Allows creating routines and starting workouts from routines.
- Allows creating foods and meals.
- Allows nutrition logging without manually entering all macros every time.
- Calculates daily calories and macros.
- Allows body weight and measurements.
- Allows JSON backup export/import.
- Allows CSV import/export for initial formats.
- Allows meal photos.
- Allows photo-assisted editable estimates if active phase implements it.
- Labels estimates/fetched calculations as approximate when applicable.

## Product Quality Acceptance

The app is not accepted as premium-feeling if core flows feel like raw CRUD forms.

Core flows must satisfy:

- Exercise creation supports presets or low-friction inputs.
- Workout logging supports fast repeated set entry.
- Nutrition logging supports saved foods, local reference foods, recents/favorites, and later online lookup.
- Dashboard shows useful real data.
- Empty states guide action.
- Validation errors are clear.
- Primary actions are visible and reachable.
- All Android UI text is English.

## Online-Assisted Acceptance

Online-assisted features are accepted only if:

- Disabled state works.
- Offline fallback works.
- Manual entry fallback works.
- Results are cached locally when useful.
- Results show source/attribution.
- Results are editable before saving.
- No paid provider is required for core use.
- Free-tier guardrails exist.
- Secrets are not hardcoded.

## Backend Acceptance

Backend/cloud phases are accepted only if:

- Worker runs locally.
- Validation commands pass.
- Endpoints return stable JSON.
- Error codes are stable.
- API key strategy is documented.
- No personal user data is stored unless explicitly approved.
- Provider rate limits are respected.
- Cache/budget rules are implemented when provider calls exist.

## AI/Workflow Acceptance

Each completed phase must have:

- Scoped diff.
- Validation commands passed.
- Manual QA completed when applicable.
- Review by a different model/tool when risk is medium/high.
- No future phase work.
- No unnecessary dependency.
- Suggested commit only; user commits manually.
