import { describe, it, expect, beforeEach, vi } from "vitest";
import {
  buildCacheKey,
  getCacheEntry,
  setCacheEntry,
  incrementCacheHit,
  isCacheEntryExpired,
  parseNormalizedJson,
  type FoodLookupCacheEntry,
} from "./cache";

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

describe("isCacheEntryExpired", () => {
  function makeEntry(overrides: Partial<FoodLookupCacheEntry> = {}): FoodLookupCacheEntry {
    return {
      cache_key: "test",
      source: "usda",
      lookup_type: "generic",
      query: "egg",
      normalized_json: "{}",
      is_approximate: true,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      hit_count: 0,
      ...overrides,
    };
  }

  it("returns false when expires_at is undefined", () => {
    expect(isCacheEntryExpired(makeEntry())).toBe(false);
  });

  it("returns false when expires_at is in the future", () => {
    const future = new Date(Date.now() + 86400000).toISOString();
    expect(isCacheEntryExpired(makeEntry({ expires_at: future }))).toBe(false);
  });

  it("returns true when expires_at is in the past", () => {
    const past = new Date(Date.now() - 1000).toISOString();
    expect(isCacheEntryExpired(makeEntry({ expires_at: past }))).toBe(true);
  });

  it("returns true when expires_at is invalid date string", () => {
    expect(isCacheEntryExpired(makeEntry({ expires_at: "not-a-date" }))).toBe(true);
  });
});

describe("parseNormalizedJson", () => {
  it("parses valid JSON object", () => {
    const result = parseNormalizedJson<{ a: number }>('{"a":1}');
    expect(result).toEqual({ a: 1 });
  });

  it("returns null for invalid JSON", () => {
    expect(parseNormalizedJson("not-json")).toBeNull();
  });

  it("returns null for JSON null", () => {
    expect(parseNormalizedJson("null")).toBeNull();
  });

  it("returns null for JSON string", () => {
    expect(parseNormalizedJson('"hello"')).toBeNull();
  });

  it("returns null for JSON number", () => {
    expect(parseNormalizedJson("42")).toBeNull();
  });

  it("returns parsed object for valid JSON array", () => {
    const result = parseNormalizedJson("[1,2]");
    expect(result).toEqual([1, 2]);
  });
});
