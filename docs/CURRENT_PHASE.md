### Status

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

### Completion Summary

Phase 17G implementation complete. Functional QA: PASS.

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