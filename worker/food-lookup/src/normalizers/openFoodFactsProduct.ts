import type { PackagedFoodProduct, NutritionValues } from "../types/packagedFoodLookup";
import { areGtinEquivalent } from "../barcode";

interface OffNutrient {
  source_per?: string;
  unit?: string;
  value?: number;
}

interface OffNutrition {
  aggregated_set?: {
    per?: string;
    nutrients?: Record<string, OffNutrient>;
  };
}

interface OffProduct {
  code?: unknown;
  product_name?: unknown;
  generic_name?: unknown;
  brands?: unknown;
  brands_tags?: unknown;
  quantity?: unknown;
  serving_size?: unknown;
  nutrition?: OffNutrition;
}

function isValidNutrientValue(value: unknown): value is number {
  return typeof value === "number" && isFinite(value) && value >= 0;
}

function toStringOrNull(value: unknown): string | null {
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }
  return null;
}

function extractAggregatedValue(
  nutrients: Record<string, OffNutrient> | undefined,
  key: string
): number | null {
  if (!nutrients) return null;
  const n = nutrients[key];
  if (!n) return null;
  if (!isValidNutrientValue(n.value)) return null;
  return n.value;
}

function normalizeBrands(product: OffProduct): string[] {
  const tags = product.brands_tags;
  if (Array.isArray(tags)) {
    const result: string[] = [];
    for (const tag of tags) {
      if (typeof tag === "string") {
        const trimmed = tag.trim();
        if (trimmed.length > 0) {
          const cleaned = trimmed.includes(":")
            ? trimmed.split(":").slice(1).join(":").trim()
            : trimmed;
          if (cleaned.length > 0 && !result.includes(cleaned)) {
            result.push(cleaned);
          }
        }
      }
    }
    if (result.length > 0) return result;
  }

  const brandsStr = toStringOrNull(product.brands);
  if (brandsStr) {
    return brandsStr
      .split(",")
      .map((b) => b.trim())
      .filter((b) => b.length > 0);
  }

  return [];
}

function resolveProductName(product: OffProduct): string | null {
  const name = toStringOrNull(product.product_name);
  if (name) return name;

  const generic = toStringOrNull(product.generic_name);
  if (generic) return generic;

  return null;
}

export function normalizeOpenFoodFactsProduct(
  product: unknown,
  requestedBarcode: string
): PackagedFoodProduct | null {
  if (
    product === null ||
    typeof product !== "object" ||
    Array.isArray(product)
  ) {
    return null;
  }

  const p = product as OffProduct;

  const code = toStringOrNull(p.code);
  if (code !== null && !areGtinEquivalent(code, requestedBarcode)) {
    return null;
  }

  const name = resolveProductName(p);
  const genericName = toStringOrNull(p.generic_name);
  const brands = normalizeBrands(p);
  const quantity = toStringOrNull(p.quantity);
  const servingSize = toStringOrNull(p.serving_size);

  const aggregatedSet = p.nutrition?.aggregated_set;
  const per =
    typeof aggregatedSet?.per === "string" ? aggregatedSet.per : undefined;
  const nutrients = aggregatedSet?.nutrients;

  const nutritionPer100g: NutritionValues =
    per === "100g"
      ? {
          caloriesKcal: extractAggregatedValue(nutrients, "energy-kcal"),
          proteinG: extractAggregatedValue(nutrients, "proteins"),
          carbohydrateG: extractAggregatedValue(nutrients, "carbohydrates"),
          fatG: extractAggregatedValue(nutrients, "fat"),
        }
      : {
          caloriesKcal: null,
          proteinG: null,
          carbohydrateG: null,
          fatG: null,
        };

  // aggregated_set.per controls aggregate semantics. The aggregate values are
  // already normalized to the per basis (e.g. "100g"); they must NOT be
  // re-interpreted as serving values. We therefore do NOT derive
  // nutritionPerServing from aggregated_set.source_per. There is no separate
  // already-supported structure whose values are explicitly serving-valued, so
  // nutritionPerServing is left null (no fabricated serving nutrition).
  const nutritionPerServing: NutritionValues = {
    caloriesKcal: null,
    proteinG: null,
    carbohydrateG: null,
    fatG: null,
  };

  return {
    externalId: requestedBarcode,
    name,
    genericName,
    brands,
    quantity,
    servingSize,
    nutritionPer100g,
    nutritionPerServing,
  };
}
