import { describe, it, expect, beforeEach, vi } from "vitest";
import {
  getRuntimeConfig,
  setRuntimeConfig,
  getSafeMode,
  getOnlineLookupAvailable,
  getUsdaProviderEnabled,
  getOpenFoodFactsProviderEnabled,
  getGenericFoodSearchEnabled,
  getBarcodeLookupEnabled,
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

  it("gets barcode lookup enabled with default", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getBarcodeLookupEnabled({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("gets barcode lookup enabled from database", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "true" })
    });

    const result = await getBarcodeLookupEnabled({ DB: mockDb } as any);
    expect(result).toBe(true);
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

  it("gets generic food search enabled with default false", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null)
    });

    const result = await getGenericFoodSearchEnabled({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("gets generic food search enabled from database as true", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "true" })
    });

    const result = await getGenericFoodSearchEnabled({ DB: mockDb } as any);
    expect(result).toBe(true);
  });

  it("gets generic food search enabled from database as false", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "false" })
    });

    const result = await getGenericFoodSearchEnabled({ DB: mockDb } as any);
    expect(result).toBe(false);
  });

  it("daily_external_call_budget '0' resolves to 0", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "0" })
    });

    const result = await getMaxDailyExternalCalls({ DB: mockDb } as any);
    expect(result).toBe(0);
  });

  it("daily_external_call_budget positive integer resolves exactly", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "10" })
    });

    const result = await getMaxDailyExternalCalls({ DB: mockDb } as any);
    expect(result).toBe(10);
  });

  it("daily_external_call_budget negative integer resolves to 0", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "-5" })
    });

    const result = await getMaxDailyExternalCalls({ DB: mockDb } as any);
    expect(result).toBe(0);
  });

  it("daily_external_call_budget malformed string resolves to 25", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "abc" })
    });

    const result = await getMaxDailyExternalCalls({ DB: mockDb } as any);
    expect(result).toBe(25);
  });

  it("daily_external_call_budget empty string resolves to 25", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "" })
    });

    const result = await getMaxDailyExternalCalls({ DB: mockDb } as any);
    expect(result).toBe(25);
  });

  it("daily_external_call_budget decimal resolves to 25", async () => {
    mockPrepare.mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue({ value: "3.5" })
    });

    const result = await getMaxDailyExternalCalls({ DB: mockDb } as any);
    expect(result).toBe(25);
  });
});
