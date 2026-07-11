import type { NutritionPer100g, GenericFoodResult } from "../types/foodLookup";

interface UsdaNutrient {
  nutrientId?: number;
  nutrient?: {
    id?: number;
    number?: string;
    name?: string;
  };
  value?: number;
}

interface UsdaFoodItem {
  fdcId?: number;
  description?: string;
  dataType?: string;
  foodNutrients?: UsdaNutrient[];
}

const NUTRIENT_ID_ENERGY_KCAL = 1008;
const NUTRIENT_ID_PROTEIN = 1003;
const NUTRIENT_ID_CARBOHYDRATE = 1005;
const NUTRIENT_ID_FAT = 1004;

function findNutrientValue(
  foodNutrients: UsdaNutrient[] | undefined,
  nutrientId: number
): number | null {
  if (!foodNutrients || !Array.isArray(foodNutrients)) {
    return null;
  }
  for (const n of foodNutrients) {
    const id = n.nutrientId ?? n.nutrient?.id;
    if (id === nutrientId) {
      return typeof n.value === "number" && isFinite(n.value) ? n.value : null;
    }
  }
  return null;
}

export function normalizeUsdaFoodItem(
  item: UsdaFoodItem
): GenericFoodResult | null {
  if (!item || typeof item !== "object") {
    return null;
  }

  const fdcId = item.fdcId;
  if (fdcId === undefined || fdcId === null) {
    return null;
  }

  const description = item.description ?? "";
  if (!description) {
    return null;
  }

  const dataType = item.dataType ?? "Unknown";
  const nutrients = item.foodNutrients;

  const caloriesKcal = findNutrientValue(nutrients, NUTRIENT_ID_ENERGY_KCAL);
  const proteinG = findNutrientValue(nutrients, NUTRIENT_ID_PROTEIN);
  const carbohydrateG = findNutrientValue(nutrients, NUTRIENT_ID_CARBOHYDRATE);
  const fatG = findNutrientValue(nutrients, NUTRIENT_ID_FAT);

  const nutritionPer100g: NutritionPer100g = {
    caloriesKcal,
    proteinG,
    carbohydrateG,
    fatG,
  };

  return {
    externalId: String(fdcId),
    name: description,
    description,
    dataType,
    nutritionPer100g,
  };
}

export function normalizeUsdaSearchResponse(
  foods: unknown
): GenericFoodResult[] {
  if (!Array.isArray(foods)) {
    return [];
  }

  const results: GenericFoodResult[] = [];
  for (const item of foods) {
    const normalized = normalizeUsdaFoodItem(item as UsdaFoodItem);
    if (normalized) {
      results.push(normalized);
    }
  }
  return results;
}
