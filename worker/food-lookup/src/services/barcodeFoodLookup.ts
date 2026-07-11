import type { Env } from "../db";
import type { PackagedFoodLookupResponse } from "../types/packagedFoodLookup";
import { isPackagedFoodLookupResponse } from "../types/packagedFoodLookup";
import {
  buildCacheKey,
  getCacheEntry,
  setCacheEntry,
  incrementCacheHit as incrementCacheEntryHit,
  isCacheEntryExpired,
  parseNormalizedJson,
} from "../cache";
import {
  getSafeMode,
  getOnlineLookupAvailable,
  getOpenFoodFactsProviderEnabled,
  getBarcodeLookupEnabled,
  getCacheEnabled,
  getCacheTtlSeconds,
  getMaxDailyExternalCalls,
} from "../runtimeConfig";
import {
  incrementCacheHit,
  incrementCacheMiss,
  incrementExternalCall,
  incrementBlockedCall,
  isBudgetExceeded,
} from "../usage";
import {
  fetchOpenFoodFactsProduct,
  buildOpenFoodFactsProviderConfig,
} from "../providers/openFoodFacts";
import { normalizeOpenFoodFactsProduct } from "../normalizers/openFoodFactsProduct";
import type { ErrorCode } from "../errors";

export interface BarcodeFoodLookupDeps {
  env: Env & { OPEN_FOOD_FACTS_USER_AGENT?: string };
  barcode: string;
  today: string;
  providerFn?: typeof fetchOpenFoodFactsProduct;
}

export interface BarcodeFoodLookupSuccess {
  ok: true;
  response: PackagedFoodLookupResponse;
  cached: boolean;
}

export interface BarcodeFoodLookupFailure {
  ok: false;
  code: ErrorCode;
}

export type BarcodeFoodLookupResult =
  | BarcodeFoodLookupSuccess
  | BarcodeFoodLookupFailure;

export async function handleBarcodeFoodLookup(
  deps: BarcodeFoodLookupDeps
): Promise<BarcodeFoodLookupResult> {
  const { env, barcode, today } = deps;
  const providerFn = deps.providerFn ?? fetchOpenFoodFactsProduct;

  const cacheKey = buildCacheKey("open_food_facts", "barcode", barcode);
  const cacheEnabled = await getCacheEnabled(env);

  if (cacheEnabled) {
    const entry = await getCacheEntry(env, cacheKey);
    if (entry) {
      if (!isCacheEntryExpired(entry)) {
        const parsed = parseNormalizedJson(entry.normalized_json);
        if (
          parsed &&
          isPackagedFoodLookupResponse(parsed) &&
          parsed.barcode === barcode &&
          parsed.product.externalId === barcode
        ) {
          await incrementCacheEntryHit(env, cacheKey);
          await incrementCacheHit(env, today);
          return {
            ok: true,
            response: parsed,
            cached: true,
          };
        }
      }
    }
  }

  await incrementCacheMiss(env, today);

  const safeMode = await getSafeMode(env);
  if (safeMode) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "lookup_disabled" };
  }

  const onlineLookupEnabled = await getOnlineLookupAvailable(env);
  if (!onlineLookupEnabled) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "lookup_disabled" };
  }

  const offProviderEnabled = await getOpenFoodFactsProviderEnabled(env);
  if (!offProviderEnabled) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "provider_disabled" };
  }

  const barcodeFeatureEnabled = await getBarcodeLookupEnabled(env);
  if (!barcodeFeatureEnabled) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "feature_disabled" };
  }

  const maxCalls = await getMaxDailyExternalCalls(env);
  const budgetExceeded = await isBudgetExceeded(env, today, maxCalls);
  if (budgetExceeded) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "budget_exceeded" };
  }

  const userAgent = env.OPEN_FOOD_FACTS_USER_AGENT;
  if (!userAgent) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "configuration_error" };
  }

  await incrementExternalCall(env, today);

  const config = buildOpenFoodFactsProviderConfig(userAgent);
  const providerResult = await providerFn(barcode, config);

  if (providerResult.kind === "timeout") {
    return { ok: false, code: "provider_timeout" };
  }
  if (providerResult.kind === "rate_limited") {
    return { ok: false, code: "provider_rate_limited" };
  }
  if (providerResult.kind === "unavailable") {
    return { ok: false, code: "provider_unavailable" };
  }
  if (providerResult.kind === "not_found") {
    return { ok: false, code: "not_found" };
  }
  if (providerResult.kind === "error" || providerResult.kind === "unexpected") {
    return { ok: false, code: "provider_error" };
  }

  const normalized = normalizeOpenFoodFactsProduct(
    providerResult.payload,
    barcode
  );

  if (!normalized) {
    return { ok: false, code: "provider_error" };
  }

  const response: PackagedFoodLookupResponse = {
    barcode,
    source: "OPEN_FOOD_FACTS",
    attribution: "Open Food Facts — ODbL",
    isApproximate: true,
    product: normalized,
  };

  if (cacheEnabled) {
    const ttlSeconds = await getCacheTtlSeconds(env);
    const expiresAt = new Date(Date.now() + ttlSeconds * 1000).toISOString();
    await setCacheEntry(env, {
      cache_key: cacheKey,
      source: "open_food_facts",
      lookup_type: "barcode",
      query: barcode,
      normalized_json: JSON.stringify(response),
      attribution: "Open Food Facts — ODbL",
      is_approximate: true,
      expires_at: expiresAt,
    });
  }

  return { ok: true, response, cached: false };
}
