import { describe, it, expect } from "vitest";
import { isPackagedFoodLookupResponse } from "./packagedFoodLookup";

const validResponse = {
  barcode: "3017620422003",
  source: "OPEN_FOOD_FACTS",
  attribution: "Open Food Facts — ODbL",
  isApproximate: true,
  product: {
    externalId: "3017620422003",
    name: "Nutella",
    genericName: "Hazelnut spread",
    brands: ["Ferrero"],
    quantity: "400 g",
    servingSize: "15 g",
    nutritionPer100g: {
      caloriesKcal: 539,
      proteinG: 6.3,
      carbohydrateG: 57.5,
      fatG: 30.9,
    },
    nutritionPerServing: {
      caloriesKcal: 81,
      proteinG: 0.9,
      carbohydrateG: 8.6,
      fatG: 4.6,
    },
  },
};

describe("isPackagedFoodLookupResponse", () => {
  it("accepts valid response", () => {
    expect(isPackagedFoodLookupResponse(validResponse)).toBe(true);
  });

  it("accepts response with null product fields", () => {
    const response = {
      ...validResponse,
      product: {
        ...validResponse.product,
        name: null,
        genericName: null,
        quantity: null,
        servingSize: null,
        nutritionPer100g: {
          caloriesKcal: null,
          proteinG: null,
          carbohydrateG: null,
          fatG: null,
        },
        nutritionPerServing: {
          caloriesKcal: null,
          proteinG: null,
          carbohydrateG: null,
          fatG: null,
        },
      },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(true);
  });

  it("accepts empty brands array", () => {
    const response = {
      ...validResponse,
      product: { ...validResponse.product, brands: [] },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(true);
  });

  it("rejects null", () => {
    expect(isPackagedFoodLookupResponse(null)).toBe(false);
  });

  it("rejects array", () => {
    expect(isPackagedFoodLookupResponse([])).toBe(false);
  });

  it("rejects empty object", () => {
    expect(isPackagedFoodLookupResponse({})).toBe(false);
  });

  it("rejects wrong source", () => {
    expect(
      isPackagedFoodLookupResponse({ ...validResponse, source: "USDA" })
    ).toBe(false);
  });

  it("rejects non-true isApproximate", () => {
    expect(
      isPackagedFoodLookupResponse({ ...validResponse, isApproximate: false })
    ).toBe(false);
  });

  it("rejects numeric barcode", () => {
    expect(
      isPackagedFoodLookupResponse({ ...validResponse, barcode: 3017620422003 })
    ).toBe(false);
  });

  it("rejects barcode with letters", () => {
    expect(
      isPackagedFoodLookupResponse({ ...validResponse, barcode: "ABC123" })
    ).toBe(false);
  });

  it("rejects barcode with wrong length", () => {
    expect(
      isPackagedFoodLookupResponse({ ...validResponse, barcode: "1234" })
    ).toBe(false);
  });

  it("rejects barcode with 11 digits", () => {
    expect(
      isPackagedFoodLookupResponse({ ...validResponse, barcode: "12345678901" })
    ).toBe(false);
  });

  it("rejects externalId mismatch", () => {
    const response = {
      ...validResponse,
      product: { ...validResponse.product, externalId: "9999999999999" },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(false);
  });

  it("rejects missing product", () => {
    const { product, ...rest } = validResponse;
    expect(isPackagedFoodLookupResponse(rest)).toBe(false);
  });

  it("rejects product as array", () => {
    expect(
      isPackagedFoodLookupResponse({ ...validResponse, product: [] })
    ).toBe(false);
  });

  it("rejects numeric externalId", () => {
    const response = {
      ...validResponse,
      product: { ...validResponse.product, externalId: 123 },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(false);
  });

  it("rejects non-string brands", () => {
    const response = {
      ...validResponse,
      product: { ...validResponse.product, brands: [123] },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(false);
  });

  it("rejects NaN nutrient", () => {
    const response = {
      ...validResponse,
      product: {
        ...validResponse.product,
        nutritionPer100g: {
          caloriesKcal: NaN,
          proteinG: 6.3,
          carbohydrateG: 57.5,
          fatG: 30.9,
        },
      },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(false);
  });

  it("rejects Infinity nutrient", () => {
    const response = {
      ...validResponse,
      product: {
        ...validResponse.product,
        nutritionPer100g: {
          caloriesKcal: Infinity,
          proteinG: 6.3,
          carbohydrateG: 57.5,
          fatG: 30.9,
        },
      },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(false);
  });

  it("rejects string nutrient", () => {
    const response = {
      ...validResponse,
      product: {
        ...validResponse.product,
        nutritionPer100g: {
          caloriesKcal: "539",
          proteinG: 6.3,
          carbohydrateG: 57.5,
          fatG: 30.9,
        },
      },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(false);
  });

  it("rejects missing nutritionPerServing", () => {
    const { nutritionPerServing, ...rest } = validResponse.product;
    expect(
      isPackagedFoodLookupResponse({
        ...validResponse,
        product: rest,
      })
    ).toBe(false);
  });

  it("rejects nutritionPerServing as array", () => {
    const response = {
      ...validResponse,
      product: { ...validResponse.product, nutritionPerServing: [] },
    };
    expect(isPackagedFoodLookupResponse(response)).toBe(false);
  });
});
