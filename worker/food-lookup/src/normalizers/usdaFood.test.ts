import { describe, it, expect } from "vitest";
import { normalizeUsdaFoodItem, normalizeUsdaSearchResponse } from "./usdaFood";

describe("normalizeUsdaFoodItem", () => {
  it("returns null for null input", () => {
    expect(normalizeUsdaFoodItem(null as never)).toBeNull();
  });

  it("returns null when fdcId is missing", () => {
    expect(
      normalizeUsdaFoodItem({ description: "Egg" } as never)
    ).toBeNull();
  });

  it("returns null when description is missing", () => {
    expect(
      normalizeUsdaFoodItem({ fdcId: 123 } as never)
    ).toBeNull();
  });

  it("normalizes a complete USDA food item", () => {
    const item = {
      fdcId: 123456,
      description: "Egg, whole, cooked",
      dataType: "Foundation",
      foodNutrients: [
        { nutrientId: 1008, value: 155 },
        { nutrientId: 1003, value: 12.6 },
        { nutrientId: 1005, value: 1.1 },
        { nutrientId: 1004, value: 10.6 },
      ],
    };

    const result = normalizeUsdaFoodItem(item as never);
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("123456");
    expect(result!.name).toBe("Egg, whole, cooked");
    expect(result!.description).toBe("Egg, whole, cooked");
    expect(result!.dataType).toBe("Foundation");
    expect(result!.nutritionPer100g.caloriesKcal).toBe(155);
    expect(result!.nutritionPer100g.proteinG).toBe(12.6);
    expect(result!.nutritionPer100g.carbohydrateG).toBe(1.1);
    expect(result!.nutritionPer100g.fatG).toBe(10.6);
  });

  it("returns null for missing nutrients", () => {
    const item = {
      fdcId: 100,
      description: "Some food",
      dataType: "SR Legacy",
      foodNutrients: [],
    };

    const result = normalizeUsdaFoodItem(item as never);
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
    expect(result!.nutritionPer100g.proteinG).toBeNull();
    expect(result!.nutritionPer100g.carbohydrateG).toBeNull();
    expect(result!.nutritionPer100g.fatG).toBeNull();
  });

  it("handles partial nutrients", () => {
    const item = {
      fdcId: 200,
      description: "Partial food",
      dataType: "Foundation",
      foodNutrients: [
        { nutrientId: 1008, value: 100 },
        { nutrientId: 1004, value: 5.5 },
      ],
    };

    const result = normalizeUsdaFoodItem(item as never);
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBe(100);
    expect(result!.nutritionPer100g.proteinG).toBeNull();
    expect(result!.nutritionPer100g.carbohydrateG).toBeNull();
    expect(result!.nutritionPer100g.fatG).toBe(5.5);
  });

  it("uses nested nutrient.id when nutrientId is absent", () => {
    const item = {
      fdcId: 300,
      description: "Nested nutrient",
      dataType: "Foundation",
      foodNutrients: [
        { nutrient: { id: 1008, name: "Energy" }, value: 200 },
      ],
    };

    const result = normalizeUsdaFoodItem(item as never);
    expect(result!.nutritionPer100g.caloriesKcal).toBe(200);
  });

  it("defaults dataType to Unknown when missing", () => {
    const item = {
      fdcId: 400,
      description: "No type",
      foodNutrients: [],
    };

    const result = normalizeUsdaFoodItem(item as never);
    expect(result!.dataType).toBe("Unknown");
  });

  it("returns null for non-finite nutrient values", () => {
    const item = {
      fdcId: 500,
      description: "Bad value",
      dataType: "Foundation",
      foodNutrients: [
        { nutrientId: 1008, value: NaN },
      ],
    };

    const result = normalizeUsdaFoodItem(item as never);
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
  });

  it("handles missing foodNutrients array", () => {
    const item = {
      fdcId: 600,
      description: "No nutrients key",
      dataType: "SR Legacy",
    };

    const result = normalizeUsdaFoodItem(item as never);
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
  });
});

describe("normalizeUsdaSearchResponse", () => {
  it("returns empty array for non-array input", () => {
    expect(normalizeUsdaSearchResponse(null)).toEqual([]);
    expect(normalizeUsdaSearchResponse(undefined)).toEqual([]);
    expect(normalizeUsdaSearchResponse("string")).toEqual([]);
  });

  it("filters out invalid items", () => {
    const foods = [
      { fdcId: 1, description: "Valid", dataType: "Foundation", foodNutrients: [] },
      { description: "No fdcId" },
      { fdcId: 2 },
    ];

    const results = normalizeUsdaSearchResponse(foods);
    expect(results).toHaveLength(1);
    expect(results[0].externalId).toBe("1");
  });

  it("normalizes multiple valid items", () => {
    const foods = [
      { fdcId: 1, description: "Food A", dataType: "Foundation", foodNutrients: [] },
      { fdcId: 2, description: "Food B", dataType: "SR Legacy", foodNutrients: [] },
    ];

    const results = normalizeUsdaSearchResponse(foods);
    expect(results).toHaveLength(2);
  });
});
