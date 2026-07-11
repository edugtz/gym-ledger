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
