import type { GenericFoodResult } from "../types/foodLookup";
import { normalizeUsdaSearchResponse } from "../normalizers/usdaFood";

export interface UsdaSearchResponse {
  foods?: unknown;
}

export interface UsdaProviderConfig {
  baseUrl: string;
  apiKey: string;
  timeoutMs: number;
  pageSize: number;
}

export type UsdaProviderError =
  | { kind: "timeout" }
  | { kind: "rate_limited" }
  | { kind: "unavailable" }
  | { kind: "error" }
  | { kind: "unexpected"; detail: string };

export interface UsdaProviderSuccess {
  kind: "success";
  results: GenericFoodResult[];
}

export type UsdaProviderResult = UsdaProviderSuccess | UsdaProviderError;

const DEFAULT_BASE_URL = "https://api.nal.usda.gov/fdc/v1";
const DEFAULT_TIMEOUT_MS = 5000;
const DEFAULT_PAGE_SIZE = 10;

export function buildUsdaProviderConfig(
  apiKey: string,
  baseUrl?: string,
  timeoutMs?: number,
  pageSize?: number
): UsdaProviderConfig {
  return {
    baseUrl: baseUrl ?? DEFAULT_BASE_URL,
    apiKey,
    timeoutMs: timeoutMs ?? DEFAULT_TIMEOUT_MS,
    pageSize: pageSize ?? DEFAULT_PAGE_SIZE,
  };
}

export async function searchUsdaGeneric(
  query: string,
  config: UsdaProviderConfig,
  fetchFn?: typeof fetch
): Promise<UsdaProviderResult> {
  const doFetch = fetchFn ?? fetch;
  const endpoint = new URL(`${config.baseUrl}/foods/search`);
  endpoint.searchParams.set("api_key", config.apiKey);

  const body = JSON.stringify({
    query,
    dataType: ["Foundation", "SR Legacy", "Survey (FNDDS)"],
    pageSize: config.pageSize,
  });

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), config.timeoutMs);

  let response: Response;
  try {
    response = await doFetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body,
      signal: controller.signal,
    });
  } catch (err: unknown) {
    clearTimeout(timeoutId);
    if (err instanceof DOMException && err.name === "AbortError") {
      return { kind: "timeout" };
    }
    return { kind: "error" };
  }
  clearTimeout(timeoutId);

  if (response.status === 429) {
    return { kind: "rate_limited" };
  }

  if (response.status >= 500 && response.status < 600) {
    return { kind: "unavailable" };
  }

  if (!response.ok) {
    return { kind: "error" };
  }

  let parsed: unknown;
  try {
    parsed = await response.json();
  } catch {
    return { kind: "unexpected", detail: "invalid_json" };
  }

  if (
    parsed === null ||
    typeof parsed !== "object" ||
    Array.isArray(parsed)
  ) {
    return { kind: "unexpected", detail: "invalid_response_shape" };
  }

  const obj = parsed as Record<string, unknown>;
  if (!Array.isArray(obj.foods)) {
    return { kind: "unexpected", detail: "invalid_response_shape" };
  }

  const results = normalizeUsdaSearchResponse(obj.foods);
  return { kind: "success", results };
}
