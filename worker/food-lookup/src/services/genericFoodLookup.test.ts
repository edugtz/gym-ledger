import { describe, it, expect, beforeEach, vi } from "vitest";
import { handleGenericFoodLookup } from "./genericFoodLookup";

function createMockEnv(overrides?: Record<string, unknown>) {
  const mockPrepare = vi.fn();
  const mockDb = { prepare: mockPrepare };

  const env: Record<string, unknown> = {
    DB: mockDb,
    USDA_API_KEY: "test-usda-key",
    ...overrides,
  };
  return { env: env as never, mockPrepare, mockDb };
}

function setupRuntimeConfig(mockPrepare: ReturnType<typeof vi.fn>, values: Record<string, string>) {
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
            cache_key: "usda:generic:egg",
            source: "usda",
            lookup_type: "generic",
            query: "egg",
            normalized_json: JSON.stringify(cachedResponse),
            attribution: "USDA FoodData Central",
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
  usda_provider_enabled: "true",
  cache_enabled: "true",
  cache_ttl_seconds: "86400",
  daily_external_call_budget: "25",
};

const cachedResponse = {
  query: "egg",
  source: "USDA",
  attribution: "USDA FoodData Central",
  isApproximate: true,
  results: [
    {
      externalId: "100",
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

describe("handleGenericFoodLookup", () => {
  describe("cache hit", () => {
    it("returns cached response without calling provider", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheHit(mockPrepare, enabledRuntimeValues, cachedResponse);

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.cached).toBe(true);
        expect(result.response.query).toBe("egg");
        expect(result.response.source).toBe("USDA");
        expect(result.response.results).toHaveLength(1);
      }
    });

    it("treats expired cache as miss", async () => {
      const { env, mockPrepare } = createMockEnv();

      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("FROM food_lookup_cache")) {
          return {
            bind: () => ({
              first: vi.fn().mockResolvedValue({
                cache_key: "usda:generic:egg",
                source: "usda",
                lookup_type: "generic",
                query: "egg",
                normalized_json: JSON.stringify(cachedResponse),
                attribution: "USDA FoodData Central",
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
                enabledRuntimeValues[key] !== undefined ? { value: enabledRuntimeValues[key] } : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(false);
    });

    it("treats malformed cached JSON as miss", async () => {
      const { env, mockPrepare } = createMockEnv();

      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("FROM food_lookup_cache")) {
          return {
            bind: () => ({
              first: vi.fn().mockResolvedValue({
                cache_key: "usda:generic:egg",
                source: "usda",
                lookup_type: "generic",
                query: "egg",
                normalized_json: "not-valid-json",
                attribution: "USDA FoodData Central",
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
                enabledRuntimeValues[key] !== undefined ? { value: enabledRuntimeValues[key] } : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(false);
    });
  });

  describe("gate blocks", () => {
    it("blocks when safe_mode is true", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, {
        ...enabledRuntimeValues,
        safe_mode: "true",
      });

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("lookup_disabled");
      }
    });

    it("blocks when online_lookup_enabled is false", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, {
        ...enabledRuntimeValues,
        safe_mode: "false",
        online_lookup_enabled: "false",
      });

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("lookup_disabled");
      }
    });

    it("blocks when usda_provider_enabled is false", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheMiss(mockPrepare, {
        ...enabledRuntimeValues,
        safe_mode: "false",
        online_lookup_enabled: "true",
        usda_provider_enabled: "false",
      });

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("provider_disabled");
      }
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
                enabledRuntimeValues[key] !== undefined ? { value: enabledRuntimeValues[key] } : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("budget_exceeded");
      }
    });

    it("returns configuration_error when USDA_API_KEY is missing", async () => {
      const { env, mockPrepare } = createMockEnv({ USDA_API_KEY: undefined });
      setupCacheMiss(mockPrepare, enabledRuntimeValues);

      const result = await handleGenericFoodLookup({ env, query: "egg", today });
      expect(result.ok).toBe(false);
      if (!result.ok) {
        expect(result.code).toBe("configuration_error");
      }
    });
  });

  describe("query normalization", () => {
    it("normalizes query to lowercase trimmed for cache", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheHit(mockPrepare, enabledRuntimeValues, {
        ...cachedResponse,
        query: "egg",
      });

      const result = await handleGenericFoodLookup({ env, query: "  EGG  ", today });
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.response.query).toBe("EGG");
      }
    });

    it("collapses repeated internal whitespace", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheHit(mockPrepare, enabledRuntimeValues, {
        ...cachedResponse,
        query: "chicken breast",
      });

      const result = await handleGenericFoodLookup({ env, query: "  Chicken   Breast  ", today });
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.response.query).toBe("Chicken Breast");
      }
    });

    it("preserves casing in display query", async () => {
      const { env, mockPrepare } = createMockEnv();
      setupCacheHit(mockPrepare, enabledRuntimeValues, {
        ...cachedResponse,
        query: "chicken breast",
      });

      const result = await handleGenericFoodLookup({ env, query: "CHICKEN BREAST", today });
      expect(result.ok).toBe(true);
      if (result.ok) {
        expect(result.response.query).toBe("CHICKEN BREAST");
      }
    });

    it("uses same cache key for equivalent whitespace variants", async () => {
      const { env, mockPrepare } = createMockEnv();
      let capturedCacheKey = "";
      const originalSetupCacheHit = setupCacheHit;

      mockPrepare.mockImplementation((sql: string) => {
        if (sql.includes("FROM food_lookup_cache")) {
          return {
            bind: (...args: unknown[]) => {
              capturedCacheKey = args[0] as string;
              return {
                first: vi.fn().mockResolvedValue({
                  cache_key: "usda:generic:chicken breast",
                  source: "usda",
                  lookup_type: "generic",
                  query: "chicken breast",
                  normalized_json: JSON.stringify(cachedResponse),
                  attribution: "USDA FoodData Central",
                  is_approximate: true,
                  expires_at: new Date(Date.now() + 86400000).toISOString(),
                  hit_count: 1,
                }),
                run: vi.fn().mockResolvedValue({}),
              };
            },
          };
        }
        return {
          bind: (...args: unknown[]) => {
            const key = args[0] as string;
            return {
              first: vi.fn().mockResolvedValue(
                enabledRuntimeValues[key] !== undefined ? { value: enabledRuntimeValues[key] } : null
              ),
              run: vi.fn().mockResolvedValue({}),
            };
          },
        };
      });

      await handleGenericFoodLookup({ env, query: "  Chicken   Breast  ", today });
      const key1 = capturedCacheKey;

      await handleGenericFoodLookup({ env, query: "Chicken Breast", today });
      const key2 = capturedCacheKey;

      expect(key1).toBe(key2);
    });
  });
});
