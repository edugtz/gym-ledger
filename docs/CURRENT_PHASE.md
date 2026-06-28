
## Phase 17C — Smart Food Entry Local Foundation

### Objective

Add local smart food entry with reference foods and macro calculator.

### Product Quality Goal

User should calculate common foods like 10 eggs without knowing macros.

### Recommended AI Route

OpenCode Go for UX; local builder for calculator/tests.

### Tasks

- Add local reference food model/source.
- Add EN/ES aliases.
- Add per-100g and unit calculator.
- Show calculated preview.
- Save calculated result via FoodRepository.
- Label approximate values.

### Do Not Do

- Do not call backend.
- Do not call external APIs.
- Do not add barcode.
- Do not add meal logging.
- Do not implement future phases.
- Keep all user-facing Android UI text in English.
- Do not add unnecessary dependencies.
- Do not commit; the user commits manually.

### Acceptance Criteria

- Scope implemented only for this phase.
- Configured/disabled/offline states behave correctly where applicable.
- Validation passes.

### Validation Commands

```bash
./gradlew clean kspDebugKotlin lintDebug testDebugUnitTest assembleDebug
```

### Manual QA Checklist

- Follow phase-specific manual QA.
- Verify no unrelated files changed.

### Suggested Commit

```text
feat: add smart food entry foundation
```
