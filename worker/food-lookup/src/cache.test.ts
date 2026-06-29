import { describe, it, expect, beforeEach, vi } from "vitest";
import { buildCacheKey, getCacheEntry, setCacheEntry, incrementCacheHit } from "./cache";

describe("buildCacheKey", () => {
  it("creates a stable cache key from source, lookup type and query", () => {
    const key = buildCacheKey("usda", "generic", "chicken breast");
    expect(key).toBe("usda:generic:chicken breast");
  });

  it("handles case insensitivity", () => {
    const key1 = buildCacheKey("USDA", "GENERIC", "CHICKEN BREAST");
    const key2 = buildCacheKey("usda", "generic", "chicken breast");
    expect(key1.toLowerCase()).toBe(key2.toLowerCase());
  });

  it("trims whitespace", () => {
    const key1 = buildCacheKey("usda", "generic", "  chicken breast  ");
    const key2 = buildCacheKey("usda", "generic", "chicken breast");
    expect(key1).toBe(key2);
  });
});

describe("cache operations", () => {
  let mockDb: any;
  let mockPrepare: any;

  beforeEach(() => {
    mockPrepare = vi.fn();
    mockDb = {
      prepare: mockPrepare
    };
  });

  it("gets cache entry", async () => {
    const mockResult = { cache_key: "test:key", source: "usda", lookup_type: "generic", query: "chicken" };
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(mockResult)
    });

    const result = await getCacheEntry({ DB: mockDb } as any, "test:key");
    expect(result).toEqual(mockResult);
  });

  it("sets cache entry", async () => {
    const mockRun = vi.fn().mockResolvedValue({});
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      run: mockRun
    });

    await setCacheEntry({ DB: mockDb } as any, {
      cache_key: "test:key",
      source: "usda",
      lookup_type: "generic",
      query: "chicken",
      normalized_json: "{}",
      is_approximate: false
    });

    expect(mockRun).toHaveBeenCalled();
  });

  it("increments cache hit", async () => {
    const mockRun = vi.fn().mockResolvedValue({});
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      run: mockRun
    });

    await incrementCacheHit({ DB: mockDb } as any, "test:key");

    expect(mockRun).toHaveBeenCalled();
  });
});