# GymLedger — Online-Assisted Platform Strategy

## 1. Principio global

GymLedger es local-first y offline-capable.

La app puede usar servicios online opcionales cuando aporten valor claro:

- reducir fricción de captura
- mejorar calidad de datos
- permitir lookup de alimentos/productos
- apoyar estimaciones aproximadas editables
- facilitar catálogos o templates futuros

Room/local data es la fuente de verdad.

Online data es una sugerencia.

## 2. Reglas no negociables

- La app debe seguir siendo útil sin internet.
- No debe haber login obligatorio para uso personal.
- No debe haber sync cloud obligatorio.
- No debe haber API pagada requerida para core use.
- Todo dato calculado/fetch debe poder editarse antes de guardar.
- Todo resultado remoto debe indicar fuente y si es aproximado.
- No se deben guardar secretos en el APK.
- No se deben mandar datos privados personales a proveedores externos salvo fase explícita.

## 3. Uso previsto para 2 personas

Optimizar para:

- 2 usuarios
- 2 dispositivos
- uso personal
- tráfico bajo
- costo objetivo $0/mes
- techo aceptable futuro $5 USD/mes si vale la pena

## 4. Stack recomendado

Backend serverless:

- Cloudflare Worker
- TypeScript
- D1 para cache estructurado
- KV opcional para cache exacto por barcode

Fuentes externas:

- USDA FoodData Central para alimentos genéricos
- Open Food Facts para productos/barcodes

No usar inicialmente:

- VPS
- Node/Express always-on
- Nutritionix
- Spoonacular
- Edamam
- paid AI runtime

## 5. Flujo de lookup

```text
Android UI
  -> ViewModel
  -> FoodLookupRepository
  -> Saved foods / Room
  -> Local references
  -> Local lookup cache / Room
  -> RemoteFoodLookupSource
  -> Cloudflare Worker
  -> D1/KV cache
  -> USDA / Open Food Facts
```

## 6. Orden de búsqueda

1. Saved foods
2. Recent/favorite foods
3. Local reference foods
4. Local lookup cache
5. Remote Worker, si está habilitado
6. Manual entry fallback

## 7. Worker endpoints planeados

```text
GET /v1/health
GET /v1/config
GET /v1/foods/generic?q=
GET /v1/foods/search?q=
GET /v1/foods/barcode/:barcode
```

## 8. Seguridad mínima

Para uso personal:

- Endpoint configurable en Settings.
- API key personal ingresada por el usuario.
- Header: `X-GymLedger-Key`.
- Cloudflare secret del lado Worker.
- No hardcodear key en app.

## 9. Guardrails de costo

- Online lookup deshabilitado por default.
- Mínimo 3 caracteres para búsqueda online.
- Debounce 500–800 ms en Android.
- Cache Room primero.
- Cache D1/KV segundo.
- Proveedor externo solo en cache miss.
- Budget diario en Worker.
- Kill switch remoto.
- Timeout 3–5 segundos.
- Manual fallback siempre.

## 10. DTO normalizado

El Worker devuelve DTOs propios, no raw payloads.

```json
{
  "id": "usda:171287",
  "source": "USDA",
  "type": "generic",
  "name": "Whole egg, large",
  "brand": null,
  "barcode": null,
  "servingLabel": "1 large egg",
  "servingGrams": 50.0,
  "caloriesPer100g": 143.0,
  "proteinPer100g": 12.6,
  "carbsPer100g": 0.7,
  "fatPer100g": 9.5,
  "confidence": "high",
  "isApproximate": true,
  "attribution": "USDA FoodData Central"
}
```
