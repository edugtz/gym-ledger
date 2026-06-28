# GymLedger v2 Plan — Advanced Cost-Conscious Features

v2 adds optional platform integrations, larger local/imported data, advanced analytics, optional AI, privacy, and long-term hardening. Basic food lookup/barcode now happens before v2; v2 focuses on richer/polished variants.

---

## Phase 54 — Health Connect Optional Integration

### Objective

Optionally read/write selected health data via Health Connect.

### Product Quality Goal

Must be optional, permission-minimal, and local Room remains source of truth.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Settings toggle.
- Request minimum permissions.
- Map body weight if approved.
- Handle denied/unavailable states.

### Do Not Do

- Do not require Health Connect.
- Do not request broad permissions.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add optional Health Connect integration
```

---

## Phase 55 — Camera Barcode Scanner Polish

### Objective

Add camera scanner after manual barcode lookup works.

### Product Quality Goal

Scanner should reduce friction but not be required for nutrition logging.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Camera permission flow.
- Barcode scanning screen.
- Use existing barcode lookup pipeline.
- Unknown barcode fallback.

### Do Not Do

- Do not bypass Worker/providers.
- Do not require scanner.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add camera barcode scanner
```

---

## Phase 56 — User-Imported Food Database

### Objective

Import a user-provided local food database.

### Product Quality Goal

User can avoid paid food APIs by importing their own data.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Define import format.
- Import barcode/name/brand/serving/macros.
- Duplicate handling.
- Validation/reporting.

### Do Not Do

- Do not bundle copyrighted DB.
- Do not call external API.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: import local food database
```

---

## Phase 57 — Advanced Food Search and Barcode Mapping

### Objective

Improve local/online food lookup quality.

### Product Quality Goal

Searching larger food data should feel fast and useful.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Search name/brand/barcode.
- Prioritize favorites/recent/source confidence.
- Show source.
- Optional duplicate merge.

### Do Not Do

- Do not add paid providers by default.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: improve food search
```

---

## Phase 58 — Progress Photos

### Objective

Add local progress photo tracking.

### Product Quality Goal

Photos should support body progress without cloud upload.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Local photo attach/view/delete.
- Privacy warnings.
- Storage handling.

### Do Not Do

- Do not upload photos.
- Do not add cloud gallery.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add progress photos
```

---

## Phase 59 — Optional BYO AI Estimate Adapter

### Objective

Allow optional user-provided AI key for estimates.

### Product Quality Goal

AI must be optional, editable, approximate, and user-controlled.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Settings for BYO key.
- Provider abstraction.
- Estimate request flow.
- No key = feature disabled.

### Do Not Do

- Do not hardcode API keys.
- Do not require AI.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add optional AI estimate adapter
```

---

## Phase 60 — Progressive Overload Suggestions

### Objective

Add local training suggestions.

### Product Quality Goal

Suggestions should help but not override user control.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Simple local rules.
- Last performance-based suggestions.
- Editable targets.

### Do Not Do

- Do not add cloud coaching.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add progressive overload suggestions
```

---

## Phase 61 — Notifications and Reminders

### Objective

Add optional local reminders.

### Product Quality Goal

Reminders should be helpful, not spammy.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Workout reminders.
- Meal/body logging reminders.
- Settings toggles.

### Do Not Do

- Do not add server push.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add local reminders
```

---

## Phase 62 — Encrypted Backup

### Objective

Add optional encrypted backup export.

### Product Quality Goal

Private data should be portable and protectable.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Password-based export.
- Encrypted import.
- Failure handling.

### Do Not Do

- Do not build cloud sync.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add encrypted backup
```

---

## Phase 63 — Reports

### Objective

Generate local reports.

### Product Quality Goal

User should be able to review progress outside the app.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Workout/nutrition/body report.
- Export to file.
- Readable summaries.

### Do Not Do

- Do not add paid report service.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add local reports
```

---

## Phase 64 — Wear OS Research

### Objective

Research wearable support.

### Product Quality Goal

Only proceed if value is clear.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Spike only.
- Evaluate feasibility/cost.
- Document findings.

### Do Not Do

- Do not implement production Wear app.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
docs: research Wear OS support
```

---

## Phase 65 — Optional Catalog Updates

### Objective

Explore optional updateable catalogs.

### Product Quality Goal

Exercise/food template catalogs can improve UX without full sync.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Worker endpoint for app catalog.
- Cache in app.
- Manual refresh.

### Do Not Do

- Do not add user accounts.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
feat: add optional catalog updates
```

---

## Phase 66 — Privacy and Security Hardening

### Objective

Review privacy/security for local+online-assisted app.

### Product Quality Goal

Personal data handling should be safe and explainable.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Secret review.
- Network review.
- Export review.
- Provider attribution/privacy review.

### Do Not Do

- Do not add features.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
chore: harden privacy and security
```

---

## Phase 67 — v2 Release Hardening

### Objective

Prepare v2 release candidate.

### Product Quality Goal

v2 should be stable, documented, and cost-controlled.

### Recommended AI Route

Gemini for Android platform integrations; OpenCode Go for UX; Codex/DeepSeek for backend/security/final review.

### Tasks

- Full regression.
- Backend smoke tests.
- Cost review.
- Release notes.

### Do Not Do

- Do not add features.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Feature is optional where appropriate.
- Offline behavior remains safe.
- Validation/manual QA pass.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Run phase-specific QA.
- Verify feature disabled/offline states.

### Suggested Commit

```text
chore: prepare v2 release
```
