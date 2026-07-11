export interface NutritionPer100g {
  caloriesKcal: number | null;
  proteinG: number | null;
  carbohydrateG: number | null;
  fatG: number | null;
}

export interface GenericFoodResult {
  externalId: string;
  name: string;
  description: string;
  dataType: string;
  nutritionPer100g: NutritionPer100g;
}

export interface GenericFoodLookupResponse {
  query: string;
  source: string;
  attribution: string;
  isApproximate: boolean;
  results: GenericFoodResult[];
}

function isFiniteOrNull(value: unknown): value is number | null {
  if (value === null) return true;
  if (typeof value !== "number") return false;
  return Number.isFinite(value);
}

function isNutritionPer100g(value: unknown): value is NutritionPer100g {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return false;
  }
  const obj = value as Record<string, unknown>;
  return (
    isFiniteOrNull(obj.caloriesKcal) &&
    isFiniteOrNull(obj.proteinG) &&
    isFiniteOrNull(obj.carbohydrateG) &&
    isFiniteOrNull(obj.fatG)
  );
}

function isGenericFoodResult(value: unknown): value is GenericFoodResult {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return false;
  }
  const obj = value as Record<string, unknown>;
  return (
    typeof obj.externalId === "string" &&
    typeof obj.name === "string" &&
    typeof obj.description === "string" &&
    typeof obj.dataType === "string" &&
    isNutritionPer100g(obj.nutritionPer100g)
  );
}

export function isGenericFoodLookupResponse(
  value: unknown
): value is GenericFoodLookupResponse {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return false;
  }
  const obj = value as Record<string, unknown>;
  return (
    typeof obj.query === "string" &&
    obj.source === "USDA" &&
    typeof obj.attribution === "string" &&
    obj.isApproximate === true &&
    Array.isArray(obj.results) &&
    obj.results.every(isGenericFoodResult)
  );
}
