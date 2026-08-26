import { describe, it, expect, vi } from "vitest";
import {
  fetchOpenFoodFactsProduct,
  buildOpenFoodFactsProviderConfig,
} from "./openFoodFacts";

const baseConfig = buildOpenFoodFactsProviderConfig(
  "GymLedger/0.1 (test@example.invalid)"
);

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

describe("buildOpenFoodFactsProviderConfig", () => {
  it("uses defaults", () => {
    const config = buildOpenFoodFactsProviderConfig("agent");
    expect(config.baseUrl).toBe("https://world.openfoodfacts.org");
    expect(config.timeoutMs).toBe(5000);
    expect(config.userAgent).toBe("agent");
  });

  it("accepts overrides", () => {
    const config = buildOpenFoodFactsProviderConfig(
      "agent",
      "http://custom",
      3000
    );
    expect(config.baseUrl).toBe("http://custom");
    expect(config.timeoutMs).toBe(3000);
  });
});

describe("fetchOpenFoodFactsProduct", () => {
  it("returns success with product payload", async () => {
    const body = {
      status: "success",
      result: { id: "product_found", name: "Product found" },
      product: { code: "3017620422003", product_name: "Nutella" },
    };
    const mockFetch = mockFetchResponse(200, body);
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetch
    );
    expect(result.kind).toBe("success");
    if (result.kind === "success") {
      expect((result.payload as Record<string, unknown>).product_name).toBe(
        "Nutella"
      );
    }
  });

  it("returns not_found on HTTP 404", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "0000000000000",
      baseConfig,
      mockFetchResponse(404, {})
    );
    expect(result.kind).toBe("not_found");
  });

  it("returns unexpected when result.id is unknown", async () => {
    const body = {
      status: "success",
      result: { id: "unknown_result_id", name: "Product found" },
    };
    const result = await fetchOpenFoodFactsProduct(
      "0000000000000",
      baseConfig,
      mockFetchResponse(200, body)
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns not_found when result.id is product_not_found", async () => {
    const body = {
      status: "success",
      result: { id: "product_not_found", name: "Product not found" },
    };
    const result = await fetchOpenFoodFactsProduct(
      "0000000000000",
      baseConfig,
      mockFetchResponse(200, body)
    );
    expect(result.kind).toBe("not_found");
  });

  it("returns unexpected for fatal/error status response", async () => {
    const body = { status: "error", result: null };
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(200, body)
    );
    expect(result.kind).toBe("unexpected");
  });

  describe("OFF v3 status acceptance", () => {
    it("accepts success + product_found", async () => {
      const body = {
        status: "success",
        result: { id: "product_found", name: "Product found" },
        product: { code: "3017620422003", product_name: "Nutella" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "3017620422003",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("success");
    });

    it("accepts success_with_warnings + product_found (810104461665 -> 0810104461665)", async () => {
      const body = {
        status: "success_with_warnings",
        result: { id: "product_found", name: "Product found" },
        product: { code: "0810104461665", product_name: "Magic Spoon" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "810104461665",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("success");
      if (result.kind === "success") {
        expect((result.payload as Record<string, unknown>).product_name).toBe(
          "Magic Spoon"
        );
      }
    });

    it("does not accept non-success, non-success_with_warnings status", async () => {
      const body = { status: "failure", result: null };
      const result = await fetchOpenFoodFactsProduct(
        "3017620422003",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("unexpected");
    });
  });

  describe("OFF v3 real failure/error shapes", () => {
    it("maps failure + product_not_found to not_found", async () => {
      const body = {
        status: "failure",
        status_verbose: "product not found",
        result: { id: "product_not_found", name: "Product not found" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "0000000000000",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("not_found");
    });

    it("preserves success + product_not_found as not_found", async () => {
      const body = {
        status: "success",
        status_verbose: "product not found",
        result: { id: "product_not_found", name: "Product not found" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "0000000000000",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("not_found");
    });

    it("does not accept failure + product_found", async () => {
      const body = {
        status: "failure",
        result: { id: "product_found", name: "Product found" },
        product: { code: "3017620422003", product_name: "Nutella" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "3017620422003",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("unexpected");
    });

    it("accepts success_with_warnings + product_found with empty errors", async () => {
      const body = {
        status: "success_with_warnings",
        warnings: ["Barcode normalization applied"],
        errors: [],
        result: { id: "product_found", name: "Product found" },
        product: { code: "0810104461665", product_name: "Magic Spoon" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "810104461665",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("success");
    });

    it("does not accept success_with_warnings + product_found with non-empty errors", async () => {
      const body = {
        status: "success_with_warnings",
        warnings: ["Barcode normalization applied"],
        errors: ["fatal_server_error"],
        result: { id: "product_found", name: "Product found" },
        product: { code: "0810104461665", product_name: "Magic Spoon" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "810104461665",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("unexpected");
    });

    it("does not accept success + product_found with non-empty errors", async () => {
      const body = {
        status: "success",
        errors: ["fatal_server_error"],
        result: { id: "product_found", name: "Product found" },
        product: { code: "3017620422003", product_name: "Nutella" },
      };
      const result = await fetchOpenFoodFactsProduct(
        "3017620422003",
        baseConfig,
        mockFetchResponse(200, body)
      );
      expect(result.kind).toBe("unexpected");
    });
  });

  it("returns unexpected when result.id is missing", async () => {
    const body = {
      status: "success",
      result: { name: "Product found" },
    };
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(200, body)
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected when result.id is numeric", async () => {
    const body = {
      status: "success",
      result: { id: 123, name: "Product found" },
    };
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(200, body)
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected when product_found but product is missing", async () => {
    const body = {
      status: "success",
      result: { id: "product_found", name: "Product found" },
    };
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(200, body)
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns rate_limited on 429", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(429, {})
    );
    expect(result.kind).toBe("rate_limited");
  });

  it("returns unavailable on 500", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(500, {})
    );
    expect(result.kind).toBe("unavailable");
  });

  it("returns unavailable on 503", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(503, {})
    );
    expect(result.kind).toBe("unavailable");
  });

  it("returns error on other non-ok status", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(401, {})
    );
    expect(result.kind).toBe("error");
  });

  it("returns unexpected on invalid json", async () => {
    const badFetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.reject(new Error("bad json")),
    }) as never;
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      badFetch
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected on top-level null", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(200, null)
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected on top-level array", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(200, [1, 2, 3])
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns unexpected on non-success status field", async () => {
    const body = { status: "error", result: null };
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(200, body)
    );
    expect(result.kind).toBe("unexpected");
  });

  it("returns timeout on abort", async () => {
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      mockFetchResponse(0, null, { isAbort: true })
    );
    expect(result.kind).toBe("timeout");
  });

  it("returns error on generic fetch failure", async () => {
    const failFetch = vi.fn().mockRejectedValue(new Error("network")) as never;
    const result = await fetchOpenFoodFactsProduct(
      "3017620422003",
      baseConfig,
      failFetch
    );
    expect(result.kind).toBe("error");
  });

  it("sends GET with correct headers", async () => {
    const mockFetch = mockFetchResponse(200, {
      status: "success",
      result: { id: "product_found" },
      product: {},
    });
    await fetchOpenFoodFactsProduct("3017620422003", baseConfig, mockFetch);

    expect(mockFetch).toHaveBeenCalledTimes(1);
    const [url, init] = (mockFetch as ReturnType<typeof vi.fn>).mock.calls[0];

    const requestUrl = new URL(url.toString());
    expect(requestUrl.pathname).toBe(
      "/api/v3.6/product/3017620422003.json"
    );
    expect(init.method).toBe("GET");
    expect(init.headers["User-Agent"]).toBe(
      "GymLedger/0.1 (test@example.invalid)"
    );
    expect(init.headers["Accept"]).toBe("application/json");
    expect(init.body).toBeUndefined();
  });

  it("encodes barcode safely in URL", async () => {
    const mockFetch = mockFetchResponse(200, {
      status: "success",
      result: { id: "product_found" },
      product: {},
    });
    await fetchOpenFoodFactsProduct("3017620422003", baseConfig, mockFetch);

    const [url] = (mockFetch as ReturnType<typeof vi.fn>).mock.calls[0];
    const requestUrl = new URL(url.toString());
    expect(requestUrl.pathname).toBe(
      "/api/v3.6/product/3017620422003.json"
    );
  });

  it("makes exactly one request", async () => {
    const mockFetch = mockFetchResponse(200, {
      status: "success",
      result: { id: "product_found" },
      product: {},
    });
    await fetchOpenFoodFactsProduct("3017620422003", baseConfig, mockFetch);
    expect(mockFetch).toHaveBeenCalledTimes(1);
  });
});
