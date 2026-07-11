export interface NutritionValues {
  caloriesKcal: number | null;
  proteinG: number | null;
  carbohydrateG: number | null;
  fatG: number | null;
}

export interface PackagedFoodProduct {
  externalId: string;
  name: string | null;
  genericName: string | null;
  brands: string[];
  quantity: string | null;
  servingSize: string | null;
  nutritionPer100g: NutritionValues;
  nutritionPerServing: NutritionValues;
}

export interface PackagedFoodLookupResponse {
  barcode: string;
  source: "OPEN_FOOD_FACTS";
  attribution: "Open Food Facts — ODbL";
  isApproximate: true;
  product: PackagedFoodProduct;
}

const BARCODE_ALLOWED_LENGTHS = new Set([8, 12, 13, 14]);

function isValidBarcodeString(value: string): boolean {
  if (!/^\d+$/.test(value)) return false;
  return BARCODE_ALLOWED_LENGTHS.has(value.length);
}

function isFiniteOrNull(value: unknown): value is number | null {
  if (value === null) return true;
  if (typeof value !== "number") return false;
  return Number.isFinite(value);
}

function isNutritionValues(value: unknown): value is NutritionValues {
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

function isPackagedFoodProduct(
  value: unknown,
  barcode: string
): value is PackagedFoodProduct {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return false;
  }
  const obj = value as Record<string, unknown>;
  return (
    typeof obj.externalId === "string" &&
    obj.externalId === barcode &&
    (obj.name === null || typeof obj.name === "string") &&
    (obj.genericName === null || typeof obj.genericName === "string") &&
    Array.isArray(obj.brands) &&
    obj.brands.every((b: unknown) => typeof b === "string") &&
    (obj.quantity === null || typeof obj.quantity === "string") &&
    (obj.servingSize === null || typeof obj.servingSize === "string") &&
    isNutritionValues(obj.nutritionPer100g) &&
    isNutritionValues(obj.nutritionPerServing)
  );
}

export function isPackagedFoodLookupResponse(
  value: unknown
): value is PackagedFoodLookupResponse {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    return false;
  }
  const obj = value as Record<string, unknown>;
  if (
    typeof obj.barcode !== "string" ||
    !isValidBarcodeString(obj.barcode)
  ) {
    return false;
  }
  return (
    obj.source === "OPEN_FOOD_FACTS" &&
    typeof obj.attribution === "string" &&
    obj.isApproximate === true &&
    isPackagedFoodProduct(obj.product, obj.barcode)
  );
}
