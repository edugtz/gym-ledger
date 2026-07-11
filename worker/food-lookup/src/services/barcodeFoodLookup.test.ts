import { describe, it, expect, vi } from "vitest";
import { handleBarcodeFoodLookup } from "./barcodeFoodLookup";
import type { OpenFoodFactsProviderResult } from "../providers/openFoodFacts";

function createMockEnv(overrides?: Record<string, unknown>) {
  const mockPrepare = vi.fn();
  const mockDb = { prepare: mockPrepare };
  const env: Record<string, unknown> = {
    DB: mockDb,
    OPEN_FOOD_FACTS_USER_AGENT: "GymLedger/0.1 (test@example.invalid)",
    ...overrides,
  };
  return { env: env as never, mockPrepare, mockDb };
}

function setupRuntimeConfig(
  mockPrepare: ReturnType<typeof vi.fn>,
  values: Record<string, string>
) {
  mockPrepare.mockImplementation((sql: string) => {
    return {
      bind: (...args: unknown[]) => {
        const key = args[0] as string;
        return {
          first: vi.fn().mockResolvedValue(
            values[key] !== undefined ? { value: values[key] } : null
          ),
          run: vi.fn().mockResolvedValue({}),
        };
      },
    };
  });
}

function setupCacheHit(
  mockPrepare: ReturnType<typeof vi.fn>,
  runtimeValues: Record<string, string>,
  cachedResponse: object
) {
  mockPrepare.mockImplementation((sql: string) => {
    if (sql.includes("FROM food_lookup_cache")) {
      return {
        bind: () => ({
          first: vi.fn().mockResolvedValue({
            cache_key: "open_food_facts:barcode:3017620422003",
            source: "open_food_facts",
            lookup_type: "barcode",
            query: "3017620422003",
            normalized_json: JSON.stringify(cachedResponse),
            attribution: "Open Food Facts — ODbL",
            is_approximate: true,
            expires_at: new Date(Date.now() + 86400000).toISOString(),
            hit_count: 1,
          }),
          run: vi.fn().mockResolvedValue({}),
        }),
      };
    }
    return {
      bind: (...args: unknown[]) => {
        const key = args[0] as string;
        return {
          first: vi.fn().mockResolvedValue(
            runtimeValues[key] !== undefined ? { value: runtimeValues[key] } : null
          ),
          run: vi.fn().mockResolvedValue({}),
        };
      },
    };
  });
}

function setupCacheMiss(
  mockPrepare: ReturnType<typeof vi.fn>,
  runtimeValues: Record<string, string>
) {
  mockPrepare.mockImplementation((sql: string) => {
    if (sql.includes("FROM food_lookup_cache")) {
      return {
        bind: () => ({
          first: vi.fn().mockResolvedValue(null),
          run: vi.fn().mockResolvedValue({}),
        }),
      };
    }
    return {
      bind: (...args: unknown[]) => {
        const key = args[0] as string;
        return {
          first: vi.fn().mockResolvedValue(
            runtimeValues[key] !== undefined ? { value: runtimeValues[key] } : null
          ),
          run: vi.fn().mockResolvedValue({}),
        };
      },
    };
  });
}

const today = "2026-07-11";

const enabledRuntimeValues: Record<string, string> = {
  safe_mode: "false",
  online_lookup_enabled: "true",
  open_food_facts_provider_enabled: "true",
  barcode_lookup_enabled: "true",
  cache_enabled: "true",
  cache_ttl_seconds: "86400",
  daily_external_call_budget: "25",
};

const cachedResponse = {
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

function makeProviderSuccess(
  product: Record<string, unknown>
): OpenFoodFactsProviderResult {
  return { kind: "success", payload: product };
}

const providerProduct = {
  code: "3017620422003",
  product_name: "Nutella",
  generic_name: "Hazelnut spread",
  brands: "Ferrero",
  brands_tags: ["xx:Ferrero"],
  quantity: "400 g",
  serving_size: "15 g",
  nutrition: {
    aggregated_set: {
      nutrients: {
        "energy-kcal": { source_per: "100g", unit: "kcal", value: 539 },
        proteins: { source_per: "100g", unit: "g", value: 6.3 },
        carbohydrates: { source_per: "100g", unit: "g", value: 57.5 },
        fat: { source_per: "100g", unit: "g", value: 30.9 },
        "energy-kcal_serving": {
          source_per: "serving",
          unit: "kcal",
          value: 81,
        },
        proteins_serving: { source_per: "serving", unit: "g", value: 0.9 },
        carbohydrates_serving: {
          source_per: "serving",
          unit: "g",
          value: 8.6,
        },
        fat_serving: { source_per: "serving", unit: "g", value: 4.6 },
      },
    },
  },
};

describe("handleBarcodeFoodLookup", () => {
  describe("cache hit", () => {
    it("returns cached response without calling provider", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheHit(mockPrepare, enabledRuntimeValues, cachedResponse);
      const providerFn = vi.fn();

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.cached).toBe(true);
        expect(result.response.barcode).toBe("3017620422003");
        expect(result.response.source).toBe("OPEN_FOOD_FACTS");
      }
      expect(providerFn).not.toHaveBeenCalled();
    });

    it("treats expired cache as miss", async () => {
      const { env, mockPrepare } = createMockEnv();
      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("FROM food_lookup_cache")) {
          return {
            bind: () => ({
              first: vi.fn().mockResolvedValue({
                cache_key: "open_food_facts:barcode:3017620422003",
                source: "open_food_facts",
                lookup_type: "barcode",
                query: "3017620422003",
                normalized_json: JSON.stringify(cachedResponse),
                attribution: "Open Food Facts — ODbL",
                is_approximate: true,
                expires_at: new Date(Date.now() - 1000).toISOString(),
                hit_count: 1,
              }),
              run: vi.fn().mockResolvedValue({}),
            }),
          };
        }
        return {
          bind: (...args: unknown[]) => {
            const key = args[0] as string;
            return {
              first: vi.fn().mockResolvedValue(
                enabledRuntimeValues[key] !== undefined
                  ? { value: enabledRuntimeValues[key] }
                  : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });
      const providerFn = vi
        .fn()
        .mockResolvedValue({ kind: "not_found" as const });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("not_found");
      }
      expect(providerFn).toHaveBeenCalledTimes(1);
    });

    it("treats malformed cached JSON as miss", async () => {
      const { env, mockPrepare } = createMockEnv();
      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("FROM food_lookup_cache")) {
          return {
            bind: () => ({
              first: vi.fn().mockResolvedValue({
                cache_key: "open_food_facts:barcode:3017620422003",
                source: "open_food_facts",
                lookup_type: "barcode",
                query: "3017620422003",
                normalized_json: "not-valid-json",
                attribution: "Open Food Facts — ODbL",
                is_approximate: true,
                expires_at: new Date(Date.now() + 86400000).toISOString(),
                hit_count: 1,
              }),
              run: vi.fn().mockResolvedValue({}),
            }),
          };
        }
        return {
          bind: (...args: unknown[]) => {
            const key = args[0] as string;
            return {
              first: vi.fn().mockResolvedValue(
                enabledRuntimeValues[key] !== undefined
                  ? { value: enabledRuntimeValues[key] }
                  : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });
      const providerFn = vi
        .fn()
        .mockResolvedValue({ kind: "not_found" as const });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("not_found");
      }
      expect(providerFn).toHaveBeenCalledTimes(1);
    });

    it("treats invalid barcode format in cache as miss", async () => {
      const { env, mockPrepare } = createMockEnv();
      const invalidCachedResponse = {
        ...cachedResponse,
        barcode: "ABC123",
        product: { ...cachedResponse.product, externalId: "ABC123" },
      };
      setupCacheHit(mockPrepare, enabledRuntimeValues, invalidCachedResponse);
      const providerFn = vi
        .fn()
        .mockResolvedValue({ kind: "not_found" as const });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("not_found");
      }
      expect(providerFn).toHaveBeenCalledTimes(1);
    });

    it("treats mismatched externalId in cache as miss", async () => {
      const { env, mockPrepare } = createMockEnv();
      const mismatchedResponse = {
        ...cachedResponse,
        product: { ...cachedResponse.product, externalId: "9999999999999" },
      };
      setupCacheHit(mockPrepare, enabledRuntimeValues, mismatchedResponse);
      const providerFn = vi
        .fn()
        .mockResolvedValue({ kind: "not_found" as const });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("not_found");
      }
      expect(providerFn).toHaveBeenCalledTimes(1);
    });

    it("treats barcode mismatch in cache as miss", async () => {
      const { env, mockPrepare } = createMockEnv();
      const mismatchedResponse = {
        ...cachedResponse,
        barcode: "9999999999999",
        product: { ...cachedResponse.product, externalId: "9999999999999" },
      };
      setupCacheHit(mockPrepare, enabledRuntimeValues, mismatchedResponse);
      const providerFn = vi
        .fn()
        .mockResolvedValue({ kind: "not_found" as const });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("not_found");
      }
      expect(providerFn).toHaveBeenCalledTimes(1);
    });
  });

  describe("gate blocks", () => {
    it("blocks when safe_mode is true", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, {
        ...enabledRuntimeValues,
        safe_mode: "true",
      });
      const providerFn = vi.fn();

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("lookup_disabled");
      }
      expect(providerFn).not.toHaveBeenCalled();
    });

    it("blocks when online_lookup_enabled is false", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, {
        ...enabledRuntimeValues,
        safe_mode: "false",
        online_lookup_enabled: "false",
      });
      const providerFn = vi.fn();

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("lookup_disabled");
      }
      expect(providerFn).not.toHaveBeenCalled();
    });

    it("blocks when open_food_facts_provider_enabled is false", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, {
        ...enabledRuntimeValues,
        safe_mode: "false",
        online_lookup_enabled: "true",
        open_food_facts_provider_enabled: "false",
      });
      const providerFn = vi.fn();

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("provider_disabled");
      }
      expect(providerFn).not.toHaveBeenCalled();
    });

    it("blocks when barcode_lookup_enabled is false", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, {
        ...enabledRuntimeValues,
        safe_mode: "false",
        online_lookup_enabled: "true",
        open_food_facts_provider_enabled: "true",
        barcode_lookup_enabled: "false",
      });
      const providerFn = vi.fn();

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("feature_disabled");
      }
      expect(providerFn).not.toHaveBeenCalled();
    });

    it("blocks when budget is exhausted", async () => {
      const { env, mockPrepare } = createMockEnv();
      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("FROM food_lookup_cache")) {
          return {
            bind: () => ({
              first: vi.fn().mockResolvedValue(null),
              run: vi.fn().mockResolvedValue({}),
            }),
          };
        }
        if (sql.includes("FROM usage_daily")) {
          return {
            bind: () => ({
              first: vi.fn().mockResolvedValue({
                external_calls: 25,
                cache_hits: 0,
                cache_misses: 0,
                blocked_calls: 0,
              }),
              run: vi.fn().mockResolvedValue({}),
            }),
          };
        }
        return {
          bind: (...args: unknown[]) => {
            const key = args[0] as string;
            return {
              first: vi.fn().mockResolvedValue(
                enabledRuntimeValues[key] !== undefined
                  ? { value: enabledRuntimeValues[key] }
                  : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });
      const providerFn = vi.fn();

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("budget_exceeded");
      }
      expect(providerFn).not.toHaveBeenCalled();
    });

    it("returns configuration_error when OPEN_FOOD_FACTS_USER_AGENT is missing", async () => {
      const { env, mockPrepare } = createMockEnv({
        OPEN_FOOD_FACTS_USER_AGENT: undefined,
      });
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn();

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("configuration_error");
      }
      expect(providerFn).not.toHaveBeenCalled();
    });
  });

  describe("provider calls", () => {
    it("calls provider exactly once on success", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue(
        makeProviderSuccess(providerProduct)
      );

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(true);
      expect(providerFn).toHaveBeenCalledTimes(1);
    });

    it("maps provider timeout to provider_timeout", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue({ kind: "timeout" });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("provider_timeout");
      }
    });

    it("maps provider rate_limited to provider_rate_limited", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue({ kind: "rate_limited" });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("provider_rate_limited");
      }
    });

    it("maps provider unavailable to provider_unavailable", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue({ kind: "unavailable" });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("provider_unavailable");
      }
    });

    it("maps provider not_found to not_found", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue({ kind: "not_found" });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("not_found");
      }
    });

    it("maps provider error to provider_error", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue({ kind: "error" });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("provider_error");
      }
    });

    it("maps provider unexpected to provider_error", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi
        .fn()
        .mockResolvedValue({ kind: "unexpected", detail: "bad" });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("provider_error");
      }
    });

    it("caches normalized response on success", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue(
        makeProviderSuccess(providerProduct)
      );

      let cachedJson = "";
      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("INSERT OR REPLACE")) {
          return {
            bind: (...args: unknown[]) => {
              cachedJson = args[4] as string;
              return { run: vi.fn().mockResolvedValue({}) };
            },
          };
        }
        return {
          bind: (...args: unknown[]) => {
            const key = args[0] as string;
            return {
              first: vi.fn().mockResolvedValue(
                enabledRuntimeValues[key] !== undefined
                  ? { value: enabledRuntimeValues[key] }
                  : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });

      const result = await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });
      expect(result.ok).toBe(true);

      const cached = JSON.parse(cachedJson);
      expect(cached.source).toBe("OPEN_FOOD_FACTS");
      expect(cached.barcode).toBe("3017620422003");
    });

    it("does not cache raw provider payload", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, enabledRuntimeValues);
      const providerFn = vi.fn().mockResolvedValue(
        makeProviderSuccess(providerProduct)
      );

      let cachedJson = "";
      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("INSERT OR REPLACE")) {
          return {
            bind: (...args: unknown[]) => {
              cachedJson = args[4] as string;
              return { run: vi.fn().mockResolvedValue({}) };
            },
          };
        }
        return {
          bind: (...args: unknown[]) => {
            const key = args[0] as string;
            return {
              first: vi.fn().mockResolvedValue(
                enabledRuntimeValues[key] !== undefined
                  ? { value: enabledRuntimeValues[key] }
                  : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });

      await handleBarcodeFoodLookup({
        env,
        barcode: "3017620422003",
        today,
        providerFn,
      });

      expect(cachedJson).not.toContain("aggregated_set");
      expect(cachedJson).not.toContain("source_per");
    });
  });
});
