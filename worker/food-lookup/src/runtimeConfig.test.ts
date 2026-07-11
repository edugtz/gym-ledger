import { describe, it, expect, beforeEach, vi } from "vitest";
import {
  getRuntimeConfig,
  setRuntimeConfig,
  getSafeMode,
  getOnlineLookupAvailable,
  getUsdaProviderEnabled,
  getOpenFoodFactsProviderEnabled,
  getMaxDailyExternalCalls,
  getCacheEnabled,
  getCacheTtlSeconds,
} from "./runtimeConfig";

describe("runtime config operations", () => {
  let mockDb: any;
  let mockPrepare: any;

  beforeEach(() => {
    mockPrepare = vi.fn();
    mockDb = {
      prepare: mockPrepare
    };
  });

  it("gets runtime config with default value", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getRuntimeConfig({ DB: mockDb } as any, "safe_mode", "false");
    expect(result).toBe("false");
  });

  it("gets runtime config with database value", async () => {
    const mockResult = { value: "true" };
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(mockResult)
    });

    const result = await getRuntimeConfig({ DB: mockDb } as any, "safe_mode");
    expect(result).toBe("true");
  });

  it("sets runtime config", async () => {
    const mockRun = vi.fn().mockResolvedValue({});
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      run: mockRun
    });

    await setRuntimeConfig({ DB: mockDb } as any, "safe_mode", "true");

    expect(mockRun).toHaveBeenCalled();
  });

  it("gets safe mode with default", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getSafeMode({ DB: mockDb } as any);
    expect(result).toBe(true);
  });

  it("gets safe mode from database", async () => {
    const mockResult = { value: "false" };
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(mockResult)
    });

    const result = await getSafeMode({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("gets online lookup available with default", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getOnlineLookupAvailable({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("gets usda provider enabled with default", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getUsdaProviderEnabled({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("gets open food facts provider enabled with default", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getOpenFoodFactsProviderEnabled({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("gets max daily external calls with default", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getMaxDailyExternalCalls({ DB: mockDb } as any);
    expect(result).toBe(25);
  });

  it("gets daily_external_call_budget default value", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getRuntimeConfig({ DB: mockDb } as any, "daily_external_call_budget");
    expect(result).toBe("25");
  });

  it("gets cache enabled with default true", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getCacheEnabled({ DB: mockDb } as any);
    expect(result).toBe(true);
  });

  it("gets cache enabled from database", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "false" })
    });

    const result = await getCacheEnabled({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("gets cache ttl seconds with default", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getCacheTtlSeconds({ DB: mockDb } as any);
    expect(result).toBe(86400);
  });

  it("gets cache ttl seconds from database", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "3600" })
    });

    const result = await getCacheTtlSeconds({ DB: mockDb } as any);
    expect(result).toBe(3600);
  });

  it("returns 86400 for invalid cache ttl", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "not-a-number" })
    });

    const result = await getCacheTtlSeconds({ DB: mockDb } as any);
    expect(result).toBe(86400);
  });
});
