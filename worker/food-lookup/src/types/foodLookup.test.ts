import { describe, it, expect } from "vitest";
import { isGenericFoodLookupResponse } from "./foodLookup";

const validDto = {
  query: "egg",
  source: "USDA",
  attribution: "USDA FoodData Central",
  isApproximate: true,
  results: [
    {
      externalId: "123456",
      name: "Egg, whole, cooked",
      description: "Egg, whole, cooked",
      dataType: "Foundation",
      nutritionPer100g: {
        caloriesKcal: 155,
        proteinG: 12.6,
        carbohydrateG: 1.1,
        fatG: 10.6,
      },
    },
  ],
};

describe("isGenericFoodLookupResponse", () => {
  it("accepts a valid cached DTO", () => {
    expect(isGenericFoodLookupResponse(validDto)).toBe(true);
  });

  it("accepts null nutrient values", () => {
    const dto = {
      ...validDto,
      results: [
        {
          ...validDto.results[0],
          nutritionPer100g: {
            caloriesKcal: null,
            proteinG: null,
            carbohydrateG: null,
            fatG: null,
          },
        },
      ],
    };
    expect(isGenericFoodLookupResponse(dto)).toBe(true);
  });

  it("rejects {}", () => {
    expect(isGenericFoodLookupResponse({})).toBe(false);
  });

  it("rejects []", () => {
    expect(isGenericFoodLookupResponse([])).toBe(false);
  });

  it("rejects unrelated object", () => {
    expect(isGenericFoodLookupResponse({ foo: "bar" })).toBe(false);
  });

  it("rejects missing results", () => {
    const dto = { ...validDto, results: undefined };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects non-array results", () => {
    const dto = { ...validDto, results: "not-array" };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects malformed result missing externalId", () => {
    const dto = {
      ...validDto,
      results: [
        {
          name: "Egg",
          description: "Egg",
          dataType: "Foundation",
          nutritionPer100g: {
            caloriesKcal: 155,
            proteinG: 12.6,
            carbohydrateG: 1.1,
            fatG: 10.6,
          },
        },
      ],
    };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects malformed result missing name", () => {
    const dto = {
      ...validDto,
      results: [
        {
          externalId: "123",
          description: "Egg",
          dataType: "Foundation",
          nutritionPer100g: {
            caloriesKcal: 155,
            proteinG: 12.6,
            carbohydrateG: 1.1,
            fatG: 10.6,
          },
        },
      ],
    };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects invalid nutrient type (string instead of number)", () => {
    const dto = {
      ...validDto,
      results: [
        {
          ...validDto.results[0],
          nutritionPer100g: {
            caloriesKcal: "155",
            proteinG: 12.6,
            carbohydrateG: 1.1,
            fatG: 10.6,
          },
        },
      ],
    };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects NaN nutrient value", () => {
    const dto = {
      ...validDto,
      results: [
        {
          ...validDto.results[0],
          nutritionPer100g: {
            caloriesKcal: NaN,
            proteinG: 12.6,
            carbohydrateG: 1.1,
            fatG: 10.6,
          },
        },
      ],
    };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects Infinity nutrient value", () => {
    const dto = {
      ...validDto,
      results: [
        {
          ...validDto.results[0],
          nutritionPer100g: {
            caloriesKcal: Infinity,
            proteinG: 12.6,
            carbohydrateG: 1.1,
            fatG: 10.6,
          },
        },
      ],
    };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects source !== USDA", () => {
    const dto = { ...validDto, source: "Other" };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects isApproximate !== true", () => {
    const dto = { ...validDto, isApproximate: false };
    expect(isGenericFoodLookupResponse(dto)).toBe(false);
  });

  it("rejects null input", () => {
    expect(isGenericFoodLookupResponse(null)).toBe(false);
  });

  it("rejects undefined input", () => {
    expect(isGenericFoodLookupResponse(undefined)).toBe(false);
  });

  it("rejects string input", () => {
    expect(isGenericFoodLookupResponse("validDto")).toBe(false);
  });

  it("rejects number input", () => {
    expect(isGenericFoodLookupResponse(42)).toBe(false);
  });
});
