import { describe, it, expect, beforeEach, vi } from "vitest";
import {
  getDailyUsage,
  incrementExternalCall,
  incrementCacheHit,
  incrementCacheMiss,
  incrementBlockedCall,
  getDailyUsageStats,
  isBudgetExceeded
} from "./usage";

describe("usage operations", () => {
  let mockDb: any;
  let mockPrepare: any;

  beforeEach(() => {
    mockPrepare = vi.fn();
    mockDb = {
      prepare: mockPrepare
    };
  });

  it("gets daily usage", async () => {
    const mockResult = { 
      usage_date: "2025-06-28", 
      external_calls: 10, 
      cache_hits: 5,
      cache_misses: 3,
      blocked_calls: 2,
      last_updated_at: "2025-06-28T10:00:00Z"
    };
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(mockResult)
    });

    const result = await getDailyUsage({ DB: mockDb } as any, "2025-06-28");
    expect(result).toEqual(mockResult);
  });

  it("increments external call", async () => {
    const mockRun = vi.fn().mockResolvedValue({});
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      run: mockRun
    });

    await incrementExternalCall({ DB: mockDb } as any, "2025-06-28");

    expect(mockRun).toHaveBeenCalled();
  });

  it("increments cache hit", async () => {
    const mockRun = vi.fn().mockResolvedValue({});
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      run: mockRun
    });

    await incrementCacheHit({ DB: mockDb } as any, "2025-06-28");

    expect(mockRun).toHaveBeenCalled();
  });

  it("increments cache miss", async () => {
    const mockRun = vi.fn().mockResolvedValue({});
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      run: mockRun
    });

    await incrementCacheMiss({ DB: mockDb } as any, "2025-06-28");

    expect(mockRun).toHaveBeenCalled();
  });

  it("increments blocked call", async () => {
    const mockRun = vi.fn().mockResolvedValue({});
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      run: mockRun
    });

    await incrementBlockedCall({ DB: mockDb } as any, "2025-06-28");

    expect(mockRun).toHaveBeenCalled();
  });

  it("gets daily usage stats", async () => {
    const mockResult = { 
      external_calls: 10, 
      cache_hits: 5,
      cache_misses: 3,
      blocked_calls: 2
    };
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(mockResult)
    });

    const result = await getDailyUsageStats({ DB: mockDb } as any, "2025-06-28");
    expect(result).toEqual({
      externalCalls: 10,
      cacheHits: 5,
      cacheMisses: 3,
      blockedCalls: 2
    });
  });

  it("checks if budget is exceeded", async () => {
    const mockResult = { 
      external_calls: 150, 
      cache_hits: 5,
      cache_misses: 3,
      blocked_calls: 2
    };
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(mockResult)
    });

    const result = await isBudgetExceeded({ DB: mockDb } as any, "2025-06-28", 100);
    expect(result).toBe(true);
  });
});