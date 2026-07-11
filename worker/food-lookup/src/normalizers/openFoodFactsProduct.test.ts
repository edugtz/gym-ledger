import { describe, it, expect } from "vitest";
import { normalizeOpenFoodFactsProduct } from "./openFoodFactsProduct";

const completeProduct = {
  code: "3017620422003",
  product_name: "Nutella",
  generic_name: "Hazelnut spread",
  brands: "Ferrero, Nutella",
  brands_tags: ["xx:nutella", "xx:Ferrero"],
  quantity: "400 g",
  serving_size: "15 g",
  nutrition: {
    aggregated_set: {
      nutrients: {
        "energy-kcal": {
          source_per: "100g",
          unit: "kcal",
          value: 539,
        },
        proteins: {
          source_per: "100g",
          unit: "g",
          value: 6.3,
        },
        carbohydrates: {
          source_per: "100g",
          unit: "g",
          value: 57.5,
        },
        fat: {
          source_per: "100g",
          unit: "g",
          value: 30.9,
        },
      },
    },
  },
};

describe("normalizeOpenFoodFactsProduct", () => {
  it("normalizes complete product", () => {
    const result = normalizeOpenFoodFactsProduct(
      completeProduct,
      "3017620422003"
    );
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("3017620422003");
    expect(result!.name).toBe("Nutella");
    expect(result!.genericName).toBe("Hazelnut spread");
    expect(result!.brands).toEqual(["nutella", "Ferrero"]);
    expect(result!.quantity).toBe("400 g");
    expect(result!.servingSize).toBe("15 g");
    expect(result!.nutritionPer100g.caloriesKcal).toBe(539);
    expect(result!.nutritionPer100g.proteinG).toBe(6.3);
    expect(result!.nutritionPer100g.carbohydrateG).toBe(57.5);
    expect(result!.nutritionPer100g.fatG).toBe(30.9);
    expect(result!.nutritionPerServing.caloriesKcal).toBeNull();
    expect(result!.nutritionPerServing.proteinG).toBeNull();
    expect(result!.nutritionPerServing.carbohydrateG).toBeNull();
    expect(result!.nutritionPerServing.fatG).toBeNull();
  });

  it("returns null for null input", () => {
    expect(normalizeOpenFoodFactsProduct(null, "3017620422003")).toBeNull();
  });

  it("returns null for array input", () => {
    expect(
      normalizeOpenFoodFactsProduct([], "3017620422003")
    ).toBeNull();
  });

  it("returns null when provider code mismatches requested barcode", () => {
    const product = { ...completeProduct, code: "9999999999999" };
    expect(
      normalizeOpenFoodFactsProduct(product, "3017620422003")
    ).toBeNull();
  });

  it("handles missing product name by falling back to generic_name", () => {
    const product = { ...completeProduct, product_name: undefined };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.name).toBe("Hazelnut spread");
  });

  it("falls back to generic_name for name", () => {
    const product = {
      ...completeProduct,
      product_name: undefined,
      generic_name: "Spread",
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.name).toBe("Spread");
  });

  it("returns null name when both product_name and generic_name are empty", () => {
    const product = {
      ...completeProduct,
      product_name: "",
      generic_name: "",
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.name).toBeNull();
  });

  it("normalizes brands from brands_tags", () => {
    const product = {
      ...completeProduct,
      brands_tags: ["xx:nutella", "xx:Ferrero"],
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.brands).toEqual(["nutella", "Ferrero"]);
  });

  it("deduplicates brands", () => {
    const product = {
      ...completeProduct,
      brands_tags: ["xx:nutella", "xx:nutella", "xx:Ferrero"],
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.brands).toEqual(["nutella", "Ferrero"]);
  });

  it("falls back to brands string when no tags", () => {
    const product = {
      ...completeProduct,
      brands: "Ferrero",
      brands_tags: [],
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.brands).toEqual(["Ferrero"]);
  });

  it("returns empty brands when no brands data", () => {
    const product = {
      ...completeProduct,
      brands: undefined,
      brands_tags: undefined,
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.brands).toEqual([]);
  });

  it("handles missing quantity", () => {
    const product = { ...completeProduct, quantity: undefined };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.quantity).toBeNull();
  });

  it("handles missing serving_size", () => {
    const product = { ...completeProduct, serving_size: undefined };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.servingSize).toBeNull();
  });

  it("returns null nutrients when nutrition is missing", () => {
    const product = { ...completeProduct, nutrition: undefined };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
    expect(result!.nutritionPer100g.proteinG).toBeNull();
    expect(result!.nutritionPer100g.carbohydrateG).toBeNull();
    expect(result!.nutritionPer100g.fatG).toBeNull();
  });

  it("preserves valid zero values", () => {
    const product = {
      ...completeProduct,
      nutrition: {
        aggregated_set: {
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: 0 },
            proteins: { source_per: "100g", unit: "g", value: 0 },
            carbohydrates: { source_per: "100g", unit: "g", value: 0 },
            fat: { source_per: "100g", unit: "g", value: 0 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBe(0);
    expect(result!.nutritionPer100g.proteinG).toBe(0);
    expect(result!.nutritionPer100g.carbohydrateG).toBe(0);
    expect(result!.nutritionPer100g.fatG).toBe(0);
  });

  it("returns null for NaN nutrient values", () => {
    const product = {
      ...completeProduct,
      nutrition: {
        aggregated_set: {
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: NaN },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
  });

  it("returns null for Infinity nutrient values", () => {
    const product = {
      ...completeProduct,
      nutrition: {
        aggregated_set: {
          nutrients: {
            "energy-kcal": {
              source_per: "100g",
              unit: "kcal",
              value: Infinity,
            },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
  });

  it("returns null for string nutrient values", () => {
    const product = {
      ...completeProduct,
      nutrition: {
        aggregated_set: {
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: "539" },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
  });

  it("does not use kJ as kcal", () => {
    const product = {
      ...completeProduct,
      nutrition: {
        aggregated_set: {
          nutrients: {
            energy: {
              source_per: "100g",
              unit: "kJ",
              value: 2252,
            },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
  });

  it("returns empty string name as null", () => {
    const product = {
      ...completeProduct,
      product_name: "  ",
      generic_name: "",
    };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.name).toBeNull();
  });

  it("returns empty string quantity as null", () => {
    const product = { ...completeProduct, quantity: "" };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.quantity).toBeNull();
  });

  it("barcode remains string in externalId", () => {
    const result = normalizeOpenFoodFactsProduct(
      completeProduct,
      "3017620422003"
    );
    expect(result).not.toBeNull();
    expect(typeof result!.externalId).toBe("string");
    expect(result!.externalId).toBe("3017620422003");
  });

  it("preserves leading zeroes in barcode", () => {
    const product = { ...completeProduct, code: "012345678905" };
    const result = normalizeOpenFoodFactsProduct(product, "012345678905");
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("012345678905");
  });
});
