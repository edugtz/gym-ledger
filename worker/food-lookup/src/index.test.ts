import { describe, it, expect, vi } from "vitest";
import worker from "./index";

interface SuccessBody {
  ok: true;
  data: Record<string, unknown>;
}

interface ErrorBody {
  ok: false;
  error: { code: string; message: string };
}

type ResponseBody = SuccessBody | ErrorBody;

function makeRequest(
  path: string,
  method = "GET",
  headers?: Record<string, string>
): Request {
  return new Request(`http://localhost:8787${path}`, { method, headers });
}

const mockEnv = {
  DB: {} as D1Database,
};

describe("GET /v1/health", () => {
  it("returns ok true with status", async () => {
    const res = await worker.fetch(makeRequest("/v1/health"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(200);
    expect(body.ok).toBe(true);
    expect((body as SuccessBody).data.status).toBe("ok");
  });

  it("returns method_not_allowed for POST", async () => {
    const res = await worker.fetch(makeRequest("/v1/health", "POST"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(405);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("method_not_allowed");
  });
});

describe("GET /v1/config", () => {
  it("returns safe public config", async () => {
    const res = await worker.fetch(makeRequest("/v1/config"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(200);
    expect(body.ok).toBe(true);

    const data = (body as SuccessBody).data;
    expect(data).toHaveProperty("onlineLookupAvailable");
    expect(data).toHaveProperty("providers");
    expect(data).toHaveProperty("features");
    expect(data).toHaveProperty("minQueryLength", 3);
    expect(data).toHaveProperty("safeMode", true);

    const providers = data.providers as Record<string, boolean>;
    expect(providers.usda).toBe(false);
    expect(providers.openFoodFacts).toBe(false);

    const features = data.features as Record<string, boolean>;
    expect(features.genericFoodSearch).toBe(false);
    expect(features.barcodeLookup).toBe(false);
  });

  it("returns method_not_allowed for POST", async () => {
    const res = await worker.fetch(makeRequest("/v1/config", "POST"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(405);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("method_not_allowed");
  });
});

describe("GET /v1/foods/generic", () => {
  it("returns method_not_allowed for POST", async () => {
    const res = await worker.fetch(makeRequest("/v1/foods/generic", "POST"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(405);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("method_not_allowed");
  });

  it("returns invalid_query for missing q", async () => {
    const res = await worker.fetch(makeRequest("/v1/foods/generic"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_query");
  });

  it("returns invalid_query for empty q", async () => {
    const res = await worker.fetch(makeRequest("/v1/foods/generic?q="), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_query");
  });

  it("returns invalid_query for too-short q", async () => {
    const res = await worker.fetch(makeRequest("/v1/foods/generic?q=ab"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_query");
  });

  it("returns invalid_query for whitespace-only q", async () => {
    const res = await worker.fetch(makeRequest("/v1/foods/generic?q=%20%20"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_query");
  });

  it("returns lookup_disabled under safe defaults", async () => {
    const mockPrepare = vi.fn().mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null),
      run: vi.fn().mockResolvedValue({}),
    });
    const envWithDb = {
      DB: { prepare: mockPrepare } as unknown as D1Database,
    };

    const res = await worker.fetch(
      makeRequest("/v1/foods/generic?q=egg"),
      envWithDb
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(503);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("lookup_disabled");
  });
});

describe("unknown routes", () => {
  it("returns not_found", async () => {
    const res = await worker.fetch(makeRequest("/unknown"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(404);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("not_found");
  });

  it("returns not_found for nested unknown routes", async () => {
    const res = await worker.fetch(makeRequest("/v1/unknown"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(404);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("not_found");
  });
});

describe("response shape", () => {
  it("success responses have { ok: true, data }", async () => {
    const res = await worker.fetch(makeRequest("/v1/health"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(body).toHaveProperty("ok", true);
    expect(body).toHaveProperty("data");
    expect(body).not.toHaveProperty("error");
  });

  it("error responses have { ok: false, error: { code, message } }", async () => {
    const res = await worker.fetch(makeRequest("/nope"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(body).toHaveProperty("ok", false);
    expect(body).toHaveProperty("error");

    const err = (body as ErrorBody).error;
    expect(err).toHaveProperty("code");
    expect(err).toHaveProperty("message");
    expect(typeof err.code).toBe("string");
    expect(typeof err.message).toBe("string");
  });
});

describe("GET /v1/foods/barcode/:barcode", () => {
  it("returns method_not_allowed for POST", async () => {
    const res = await worker.fetch(
      makeRequest("/v1/foods/barcode/3017620422003", "POST"),
      mockEnv
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(405);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("method_not_allowed");
  });

  it("returns invalid_barcode for missing barcode", async () => {
    const res = await worker.fetch(
      makeRequest("/v1/foods/barcode/"),
      mockEnv
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_barcode");
  });

  it("returns invalid_barcode for trailing slash", async () => {
    const res = await worker.fetch(
      makeRequest("/v1/foods/barcode/3017620422003/"),
      mockEnv
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_barcode");
  });

  it("returns invalid_barcode for nested segment", async () => {
    const res = await worker.fetch(
      makeRequest("/v1/foods/barcode/3017620422003/extra"),
      mockEnv
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_barcode");
  });

  it("returns invalid_barcode for non-numeric barcode", async () => {
    const res = await worker.fetch(
      makeRequest("/v1/foods/barcode/ABC123"),
      mockEnv
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_barcode");
  });

  it("returns invalid_barcode for too-short barcode", async () => {
    const res = await worker.fetch(
      makeRequest("/v1/foods/barcode/1234"),
      mockEnv
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(400);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("invalid_barcode");
  });

  it("returns lookup_disabled under safe defaults", async () => {
    const mockPrepare = vi.fn().mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null),
      run: vi.fn().mockResolvedValue({}),
    });
    const envWithDb = {
      DB: { prepare: mockPrepare } as unknown as D1Database,
    };

    const res = await worker.fetch(
      makeRequest("/v1/foods/barcode/3017620422003"),
      envWithDb
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(503);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("lookup_disabled");
  });

  it("existing USDA route remains unchanged", async () => {
    const mockPrepare = vi.fn().mockReturnValue({
      bind: vi.fn().mockReturnThis(),
      first: vi.fn().mockResolvedValue(null),
      run: vi.fn().mockResolvedValue({}),
    });
    const envWithDb = {
      DB: { prepare: mockPrepare } as unknown as D1Database,
    };

    const res = await worker.fetch(
      makeRequest("/v1/foods/generic?q=egg"),
      envWithDb
    );
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(503);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("lookup_disabled");
  });

  it("health and config remain unchanged", async () => {
    const health = await worker.fetch(makeRequest("/v1/health"), mockEnv);
    const healthBody = (await health.json()) as ResponseBody;
    expect(healthBody.ok).toBe(true);

    const config = await worker.fetch(makeRequest("/v1/config"), mockEnv);
    const configBody = (await config.json()) as ResponseBody;
    expect(configBody.ok).toBe(true);
  });

  it("unknown route still returns not_found", async () => {
    const res = await worker.fetch(makeRequest("/unknown"), mockEnv);
    const body = (await res.json()) as ResponseBody;

    expect(res.status).toBe(404);
    expect(body.ok).toBe(false);
    expect((body as ErrorBody).error.code).toBe("not_found");
  });
});

describe("authentication", () => {
  const envWithKey = {
    DB: {} as D1Database,
    GYMLEDGER_API_KEY: "test-secret-key",
  };

  const mockDbEnv = {
    DB: { prepare: vi.fn().mockReturnValue({}) } as unknown as D1Database,
    GYMLEDGER_API_KEY: "test-secret-key",
  };

  describe("public routes remain public when GYMLEDGER_API_KEY is configured", () => {
    it("health returns 200 without X-GymLedger-Key", async () => {
      const res = await worker.fetch(makeRequest("/v1/health"), envWithKey);
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(200);
      expect(body.ok).toBe(true);
    });

    it("config returns 200 without X-GymLedger-Key", async () => {
      const res = await worker.fetch(makeRequest("/v1/config"), envWithKey);
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(200);
      expect(body.ok).toBe(true);
    });
  });

  describe("generic route authentication", () => {
    it("returns unauthorized when key is configured but header is missing", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/generic?q=egg"),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(401);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("unauthorized");
    });

    it("returns unauthorized when key is configured but header is wrong", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/generic?q=egg", "GET", { "X-GymLedger-Key": "wrong-key" }),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(401);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("unauthorized");
    });

    it("returns invalid_query before auth when query is invalid", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/generic?q="),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(400);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("invalid_query");
    });

    it("returns invalid_query before auth when query is too short", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/generic?q=ab"),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(400);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("invalid_query");
    });

    it("proceeds to service when correct key is provided", async () => {
      const mockPrepare = vi.fn().mockReturnValue({
        bind: vi.fn().mockReturnThis(),
        first: vi.fn().mockResolvedValue(null),
        run: vi.fn().mockResolvedValue({}),
      });
      const envWithDbKey = {
        DB: { prepare: mockPrepare } as unknown as D1Database,
        GYMLEDGER_API_KEY: "test-secret-key",
      };

      const res = await worker.fetch(
        makeRequest("/v1/foods/generic?q=egg", "GET", { "X-GymLedger-Key": "test-secret-key" }),
        envWithDbKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(503);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("lookup_disabled");
    });

    it("unauthorized request does not invoke provider service", async () => {
      const mockPrepare = vi.fn().mockReturnValue({
        bind: vi.fn().mockReturnThis(),
        first: vi.fn().mockResolvedValue(null),
        run: vi.fn().mockResolvedValue({}),
      });
      const envWithDbKey = {
        DB: { prepare: mockPrepare } as unknown as D1Database,
        GYMLEDGER_API_KEY: "test-secret-key",
      };

      await worker.fetch(
        makeRequest("/v1/foods/generic?q=egg"),
        envWithDbKey
      );

      expect(mockPrepare).not.toHaveBeenCalled();
    });
  });

  describe("barcode route authentication", () => {
    it("returns unauthorized when key is configured but header is missing", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/barcode/3017620422003"),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(401);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("unauthorized");
    });

    it("returns unauthorized when key is configured but header is wrong", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/barcode/3017620422003", "GET", { "X-GymLedger-Key": "wrong-key" }),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(401);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("unauthorized");
    });

    it("returns invalid_barcode before auth when barcode is invalid", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/barcode/1234"),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(400);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("invalid_barcode");
    });

    it("returns invalid_barcode for trailing slash before auth", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/barcode/3017620422003/"),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(400);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("invalid_barcode");
    });

    it("returns invalid_barcode for nested segment before auth", async () => {
      const res = await worker.fetch(
        makeRequest("/v1/foods/barcode/3017620422003/extra"),
        envWithKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(400);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("invalid_barcode");
    });

    it("proceeds to service when correct key is provided", async () => {
      const mockPrepare = vi.fn().mockReturnValue({
        bind: vi.fn().mockReturnThis(),
        first: vi.fn().mockResolvedValue(null),
        run: vi.fn().mockResolvedValue({}),
      });
      const envWithDbKey = {
        DB: { prepare: mockPrepare } as unknown as D1Database,
        GYMLEDGER_API_KEY: "test-secret-key",
      };

      const res = await worker.fetch(
        makeRequest("/v1/foods/barcode/3017620422003", "GET", { "X-GymLedger-Key": "test-secret-key" }),
        envWithDbKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(503);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("lookup_disabled");
    });

    it("unauthorized request does not invoke provider service", async () => {
      const mockPrepare = vi.fn().mockReturnValue({
        bind: vi.fn().mockReturnThis(),
        first: vi.fn().mockResolvedValue(null),
        run: vi.fn().mockResolvedValue({}),
      });
      const envWithDbKey = {
        DB: { prepare: mockPrepare } as unknown as D1Database,
        GYMLEDGER_API_KEY: "test-secret-key",
      };

      await worker.fetch(
        makeRequest("/v1/foods/barcode/3017620422003"),
        envWithDbKey
      );

      expect(mockPrepare).not.toHaveBeenCalled();
    });
  });

  describe("no-key local development fallback", () => {
    it("allows request when GYMLEDGER_API_KEY is not configured", async () => {
      const mockPrepare = vi.fn().mockReturnValue({
        bind: vi.fn().mockReturnThis(),
        first: vi.fn().mockResolvedValue(null),
        run: vi.fn().mockResolvedValue({}),
      });
      const envNoKey = {
        DB: { prepare: mockPrepare } as unknown as D1Database,
      };

      const res = await worker.fetch(
        makeRequest("/v1/foods/generic?q=egg"),
        envNoKey
      );
      const body = (await res.json()) as ResponseBody;
      expect(res.status).toBe(503);
      expect(body.ok).toBe(false);
      expect((body as ErrorBody).error.code).toBe("lookup_disabled");
    });
  });
});
