import type { PackagedFoodProduct, NutritionValues } from "../types/packagedFoodLookup";

interface OffNutrient {
  source_per?: string;
  unit?: string;
  value?: number;
}

interface OffNutrition {
  aggregated_set?: {
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

function isFiniteNumber(value: unknown): value is number {
  return typeof value === "number" && isFinite(value);
}

function toStringOrNull(value: unknown): string | null {
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : null;
  }
  return null;
}

function extractNutrient(
  nutrients: Record<string, OffNutrient> | undefined,
  key: string,
  sourcePer: string
): number | null {
  if (!nutrients) return null;
  const n = nutrients[key];
  if (!n || n.source_per !== sourcePer) return null;
  if (!isFiniteNumber(n.value)) return null;
  return n.value;
}

function normalizeNutrition(
  nutrients: Record<string, OffNutrient> | undefined,
  sourcePer: string
): NutritionValues {
  return {
    caloriesKcal: extractNutrient(nutrients, "energy-kcal", sourcePer),
    proteinG: extractNutrient(nutrients, "proteins", sourcePer),
    carbohydrateG: extractNutrient(nutrients, "carbohydrates", sourcePer),
    fatG: extractNutrient(nutrients, "fat", sourcePer),
  };
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
  if (code && code !== requestedBarcode) {
    return null;
  }

  const name = resolveProductName(p);
  const genericName = toStringOrNull(p.generic_name);
  const brands = normalizeBrands(p);
  const quantity = toStringOrNull(p.quantity);
  const servingSize = toStringOrNull(p.serving_size);

  const nutrients = p.nutrition?.aggregated_set?.nutrients;
  const nutritionPer100g = normalizeNutrition(nutrients, "100g");
  const nutritionPerServing = normalizeNutrition(nutrients, "serving");

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
