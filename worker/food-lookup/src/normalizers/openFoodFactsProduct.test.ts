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
      per: "100g",
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
          per: "100g",
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
          per: "100g",
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
          per: "100g",
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
          per: "100g",
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
          per: "100g",
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

describe("normalizeOpenFoodFactsProduct GTIN identity (Issue 2)", () => {
  it("accepts exact requested/provider match", () => {
    const product = { code: "3017620422003", product_name: "Nutella" };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("3017620422003");
  });

  it("accepts UPC-12 requested vs zero-prefixed EAN-13 provider (810104461665)", () => {
    const product = { code: "0810104461665", product_name: "Magic Spoon" };
    const result = normalizeOpenFoodFactsProduct(product, "810104461665");
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("810104461665");
  });

  it("accepts reverse representation (0810104461665 requested vs 810104461665)", () => {
    const product = { code: "810104461665", product_name: "Magic Spoon" };
    const result = normalizeOpenFoodFactsProduct(product, "0810104461665");
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("0810104461665");
  });

  it("accepts GTIN-14 equivalent padding (681131077637)", () => {
    const product = { code: "0681131077637", product_name: "Product" };
    const result = normalizeOpenFoodFactsProduct(product, "681131077637");
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("681131077637");
  });

  it("accepts GTIN-8 vs GTIN-14 equivalent padding", () => {
    const product = { code: "00000012345670", product_name: "Product" };
    const result = normalizeOpenFoodFactsProduct(product, "12345670");
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("12345670");
  });

  it("rejects genuinely different provider code", () => {
    const product = { code: "9999999999999", product_name: "Product" };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).toBeNull();
  });

  it("rejects malformed (non-digit) provider code", () => {
    const product = { code: "ABC123", product_name: "Product" };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).toBeNull();
  });

  it("rejects malformed (wrong length) provider code", () => {
    const product = { code: "123", product_name: "Product" };
    const result = normalizeOpenFoodFactsProduct(product, "3017620422003");
    expect(result).toBeNull();
  });

  it("preserves exact requested barcode in externalId and does not normalize it", () => {
    const product = { code: "0810104461665", product_name: "Magic Spoon" };
    const result = normalizeOpenFoodFactsProduct(product, "810104461665");
    expect(result).not.toBeNull();
    expect(result!.externalId).toBe("810104461665");
  });
});

describe("normalizeOpenFoodFactsProduct aggregated nutrition semantics (Issue 3)", () => {
  it("maps aggregated_set.per=100g even when nutrient source_per=serving (7503038959614)", () => {
    const product = {
      code: "7503038959614",
      product_name: "Product",
      nutrition: {
        aggregated_set: {
          per: "100g",
          nutrients: {
            "energy-kcal": { source_per: "serving", unit: "kcal", value: 500 },
            proteins: { source_per: "serving", unit: "g", value: 10 },
            carbohydrates: { source_per: "serving", unit: "g", value: 20 },
            fat: { source_per: "serving", unit: "g", value: 5 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "7503038959614");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBe(500);
    expect(result!.nutritionPer100g.proteinG).toBe(10);
    expect(result!.nutritionPer100g.carbohydrateG).toBe(20);
    expect(result!.nutritionPer100g.fatG).toBe(5);
    // The aggregate is normalized to 100g; source_per=serving must NOT cause
    // the same values to be presented as serving nutrition (A).
    expect(result!.nutritionPerServing.caloriesKcal).toBeNull();
    expect(result!.nutritionPerServing.proteinG).toBeNull();
    expect(result!.nutritionPerServing.carbohydrateG).toBeNull();
    expect(result!.nutritionPerServing.fatG).toBeNull();
  });

  it("maps aggregated_set.per=100g with normal source_per=100g", () => {
    const product = {
      code: "7503038959614",
      product_name: "Product",
      nutrition: {
        aggregated_set: {
          per: "100g",
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: 500 },
            proteins: { source_per: "100g", unit: "g", value: 10 },
            carbohydrates: { source_per: "100g", unit: "g", value: 20 },
            fat: { source_per: "100g", unit: "g", value: 5 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "7503038959614");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBe(500);
    expect(result!.nutritionPer100g.proteinG).toBe(10);
    expect(result!.nutritionPer100g.carbohydrateG).toBe(20);
    expect(result!.nutritionPer100g.fatG).toBe(5);
    // No fabricated serving nutrition: there is no separate serving-valued
    // structure (B).
    expect(result!.nutritionPerServing.caloriesKcal).toBeNull();
    expect(result!.nutritionPerServing.proteinG).toBeNull();
    expect(result!.nutritionPerServing.carbohydrateG).toBeNull();
    expect(result!.nutritionPerServing.fatG).toBeNull();
  });

  it("does not map aggregated_set.per=100ml to nutritionPer100g (7502223775763)", () => {
    const product = {
      code: "7502223775763",
      product_name: "Liquid",
      nutrition: {
        aggregated_set: {
          per: "100ml",
          nutrients: {
            "energy-kcal": { source_per: "100ml", unit: "kcal", value: 45 },
            proteins: { source_per: "100ml", unit: "g", value: 0.1 },
            carbohydrates: { source_per: "100ml", unit: "g", value: 9 },
            fat: { source_per: "100ml", unit: "g", value: 0.2 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "7502223775763");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
    expect(result!.nutritionPer100g.proteinG).toBeNull();
    expect(result!.nutritionPer100g.carbohydrateG).toBeNull();
    expect(result!.nutritionPer100g.fatG).toBeNull();
    // per=100ml must not fabricate aggregate nutrition into either field (C).
    expect(result!.nutritionPerServing.caloriesKcal).toBeNull();
    expect(result!.nutritionPerServing.proteinG).toBeNull();
    expect(result!.nutritionPerServing.carbohydrateG).toBeNull();
    expect(result!.nutritionPerServing.fatG).toBeNull();
  });

  it("leaves missing aggregated nutrients null without fabricating zeros", () => {
    const product = {
      code: "7503038959614",
      product_name: "Product",
      nutrition: {
        aggregated_set: {
          per: "100g",
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: 500 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "7503038959614");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBe(500);
    expect(result!.nutritionPer100g.proteinG).toBeNull();
    expect(result!.nutritionPer100g.carbohydrateG).toBeNull();
    expect(result!.nutritionPer100g.fatG).toBeNull();
  });

  it("rejects negative aggregated nutrient values", () => {
    const product = {
      code: "7503038959614",
      product_name: "Product",
      nutrition: {
        aggregated_set: {
          per: "100g",
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: -10 },
            proteins: { source_per: "100g", unit: "g", value: 10 },
            carbohydrates: { source_per: "100g", unit: "g", value: 20 },
            fat: { source_per: "100g", unit: "g", value: 5 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "7503038959614");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
    expect(result!.nutritionPer100g.proteinG).toBe(10);
  });

  it("rejects non-finite aggregated nutrient values", () => {
    const product = {
      code: "7503038959614",
      product_name: "Product",
      nutrition: {
        aggregated_set: {
          per: "100g",
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: NaN },
            proteins: { source_per: "100g", unit: "g", value: 10 },
            carbohydrates: { source_per: "100g", unit: "g", value: 20 },
            fat: { source_per: "100g", unit: "g", value: 5 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "7503038959614");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
    expect(result!.nutritionPer100g.proteinG).toBe(10);
  });

  it("does not fabricate nutritionPer100g when aggregated_set.per is absent", () => {
    const product = {
      code: "7503038959614",
      product_name: "Product",
      nutrition: {
        aggregated_set: {
          nutrients: {
            "energy-kcal": { source_per: "100g", unit: "kcal", value: 500 },
          },
        },
      },
    };
    const result = normalizeOpenFoodFactsProduct(product, "7503038959614");
    expect(result).not.toBeNull();
    expect(result!.nutritionPer100g.caloriesKcal).toBeNull();
  });
});
