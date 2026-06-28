# GymLedger — Import and Export Formats

## General Rules

- Use UTF-8.
- Use ISO-8601 date strings.
- Use dot decimal separator.
- CSV files must include headers.
- JSON backup must include `schemaVersion`.
- Invalid imports must show readable errors.
- Failed imports must not corrupt existing data.
- Do not export API keys or secrets.
- Do not export Cloudflare Worker secrets.

## Backup Scope

Default backup exports user-owned saved data:

- settings except secrets
- exercises
- routines
- workouts
- foods
- meals
- meal items
- body measurements
- photos metadata when implemented

Lookup cache export is optional and disabled by default.

## Food Fields

Food export should support current/future fields where present:

```json
{
  "id": 1,
  "name": "Chicken Breast",
  "brand": null,
  "barcode": null,
  "servingSize": 100.0,
  "caloriesPerServing": 165,
  "proteinPerServing": 31.0,
  "carbsPerServing": 0.0,
  "fatPerServing": 3.6,
  "source": "manual",
  "attribution": null,
  "isApproximate": false,
  "notes": "",
  "createdAt": "2026-05-23T18:00:00-06:00",
  "updatedAt": "2026-05-23T18:00:00-06:00"
}
```

## Lookup Cache Export

Lookup cache may include:

- source provider
- provider id
- barcode
- normalized name
- brand
- serving grams
- per-100g macros
- attribution
- cachedAt

Do not export raw provider JSON by default.

## Settings Export

Export settings like:

- weight unit
- theme
- macro goals
- online lookup enabled flag
- provider toggles

Do not export:

- personal API key
- Cloudflare secret
- tokens
