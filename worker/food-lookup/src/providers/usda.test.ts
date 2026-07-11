import { describe, it, expect, vi } from "vitest";
import { searchUsdaGeneric, buildUsdaProviderConfig } from "./usda";

const baseConfig = buildUsdaProviderConfig("test-key");

function mockFetchResponse(
  status: number,
  body: unknown,
  options?: { isAbort?: boolean }
): typeof fetch {
  if (options?.isAbort) {
    return vi.fn().mockRejectedValue(new DOMException("Aborted", "AbortError")) as never;
  }
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  }) as never;
}

describe("buildUsdaProviderConfig", () => {
  it("uses defaults", () => {
    const config = buildUsdaProviderConfig("key");
    expect(config.baseUrl).toBe("https://api.nal.usda.gov/fdc/v1");
    expect(config.timeoutMs).toBe(5000);
    expect(config.pageSize).toBe(10);
    expect(config.apiKey).toBe("key");
  });

  it("accepts overrides", () => {
    const config = buildUsdaProviderConfig("key", "http://custom", 3000, 5);
    expect(config.baseUrl).toBe("http://custom");
    expect(config.timeoutMs).toBe(3000);
    expect(config.pageSize).toBe(5);
  });
});

describe("searchUsdaGeneric", () => {
  it("returns success with normalized results", async () => {
    const usdaBody = {
      foods: [
        {
          fdcId: 100,
          description: "Egg",
          dataType: "Foundation",
          foodNutrients: [
            { nutrientId: 1008, value: 155 },
            { nutrientId: 1003, value: 12.6 },
            { nutrientId: 1005, value: 1.1 },
            { nutrientId: 1004, value: 10.6 },
          ],
        },
      ],
    };

    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(200, usdaBody));
    expect(result.kind).toBe("success");
    if (result.kind === "success") {
      expect(result.results).toHaveLength(1);
      expect(result.results[0].externalId).toBe("100");
      expect(result.results[0].nutritionPer100g.caloriesKcal).toBe(155);
    }
  });

  it("returns timeout on abort", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(0, null, { isAbort: true }));
    expect(result.kind).toBe("timeout");
  });

  it("returns rate_limited on 429", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(429, {}));
    expect(result.kind).toBe("rate_limited");
  });

  it("returns unavailable on 500", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(500, {}));
    expect(result.kind).toBe("unavailable");
  });

  it("returns unavailable on 503", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(503, {}));
    expect(result.kind).toBe("unavailable");
  });

  it("returns error on other non-ok status", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(401, {}));
    expect(result.kind).toBe("error");
  });

  it("returns unexpected on invalid json", async () => {
    const badFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.reject(new Error("bad json")),
    }) as never;

    const result = await searchUsdaGeneric("egg", baseConfig, badFetch);
    expect(result.kind).toBe("unexpected");
  });

  it("returns success with empty results when foods is empty", async () => {
    const result = await searchUsdaGeneric("zzzzz", baseConfig, mockFetchResponse(200, { foods: [] }));
    expect(result.kind).toBe("success");
    if (result.kind === "success") {
      expect(result.results).toHaveLength(0);
    }
  });

  it("returns unexpected on empty object response", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(200, {}));
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected when foods is null", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(200, { foods: null }));
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected when foods is a string", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(200, { foods: "invalid" }));
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected on top-level array", async () => {
    const result = await searchUsdaGeneric("egg", baseConfig, mockFetchResponse(200, [1, 2, 3]));
    expect(result.kind).toBe("unexpected");
  });

  it("returns error on generic fetch failure", async () => {
    const failFetch = vi.fn().mockRejectedValue(new Error("network")) as never;
    const result = await searchUsdaGeneric("egg", baseConfig, failFetch);
    expect(result.kind).toBe("error");
  });

  it("sends POST with api_key in URL query parameter", async () => {
    const mockFetch = mockFetchResponse(200, { foods: [] });
    await searchUsdaGeneric("egg", baseConfig, mockFetch);

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const [url, init] = (mockFetch as ReturnType<typeof vi.fn>).mock.calls[0];

    const requestUrl = new URL(url.toString());
    expect(requestUrl.origin + requestUrl.pathname).toBe("https://api.nal.usda.gov/fdc/v1/foods/search");
    expect(requestUrl.searchParams.get("api_key")).toBe("test-key");

    expect(init.method).toBe("POST");
    expect(init.headers["Api-Key"]).toBeUndefined();
    expect(init.headers["Content-Type"]).toBe("application/json");

    const body = JSON.parse(init.body);
    expect(body.query).toBe("egg");
    expect(body.pageSize).toBe(10);
    expect(body.dataType).toEqual(["Foundation", "SR Legacy", "Survey (FNDDS)"]);
  });

  it("safely encodes API key with special characters in URL", async () => {
    const specialConfig = buildUsdaProviderConfig("key/with+special=chars&");
    const mockFetch = mockFetchResponse(200, { foods: [] });
    await searchUsdaGeneric("egg", specialConfig, mockFetch);

    const [url] = (mockFetch as ReturnType<typeof vi.fn>).mock.calls[0];
    const requestUrl = new URL(url.toString());
    expect(requestUrl.searchParams.get("api_key")).toBe("key/with+special=chars&");
    expect(requestUrl.search).toContain("api_key=key%2Fwith%2Bspecial%3Dchars%26");
  });
});
