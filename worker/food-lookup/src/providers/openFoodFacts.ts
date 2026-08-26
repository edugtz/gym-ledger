export interface OpenFoodFactsProviderConfig {
  baseUrl: string;
  userAgent: string;
  timeoutMs: number;
}

export type OpenFoodFactsProviderResult =
  | { kind: "success"; payload: unknown }
  | { kind: "not_found" }
  | { kind: "timeout" }
  | { kind: "rate_limited" }
  | { kind: "unavailable" }
  | { kind: "error" }
  | { kind: "unexpected"; detail: string };

const DEFAULT_BASE_URL = "https://world.openfoodfacts.org";
const DEFAULT_TIMEOUT_MS = 5000;

export function buildOpenFoodFactsProviderConfig(
  userAgent: string,
  baseUrl?: string,
  timeoutMs?: number
): OpenFoodFactsProviderConfig {
  return {
    baseUrl: baseUrl ?? DEFAULT_BASE_URL,
    userAgent,
    timeoutMs: timeoutMs ?? DEFAULT_TIMEOUT_MS,
  };
}

export async function fetchOpenFoodFactsProduct(
  barcode: string,
  config: OpenFoodFactsProviderConfig,
  fetchFn?: typeof fetch
): Promise<OpenFoodFactsProviderResult> {
  const doFetch = fetchFn ?? fetch;
  const url = new URL(
    `/api/v3.6/product/${encodeURIComponent(barcode)}.json`,
    config.baseUrl
  );

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), config.timeoutMs);

  let response: Response;
  try {
    response = await doFetch(url, {
      method: "GET",
      headers: {
        "User-Agent": config.userAgent,
        Accept: "application/json",
      },
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

  if (response.status === 404) {
    return { kind: "not_found" };
  }

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
    return { kind: "unexpected", detail: "invalid_top_level" };
  }

  const obj = parsed as Record<string, unknown>;

  const status = obj.status;
  if (status !== "success" && status !== "success_with_warnings") {
    return { kind: "unexpected", detail: "non_success_status" };
  }

  if (
    obj.result === null ||
    typeof obj.result !== "object" ||
    Array.isArray(obj.result)
  ) {
    return { kind: "unexpected", detail: "missing_result" };
  }

  const result = obj.result as Record<string, unknown>;
  if (typeof result.id !== "string") {
    return { kind: "unexpected", detail: "missing_or_invalid_result_id" };
  }

  if (result.id === "product_not_found") {
    return { kind: "not_found" };
  }

  if (result.id !== "product_found") {
    return { kind: "unexpected", detail: "unknown_result_id" };
  }

  if (
    obj.product === null ||
    typeof obj.product !== "object" ||
    Array.isArray(obj.product)
  ) {
    return { kind: "unexpected", detail: "missing_product" };
  }

  return { kind: "success", payload: obj.product };
}
