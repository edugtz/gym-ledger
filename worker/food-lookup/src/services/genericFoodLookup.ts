import type { Env } from "../db";
import type { GenericFoodLookupResponse } from "../types/foodLookup";
import { isGenericFoodLookupResponse } from "../types/foodLookup";
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
  getUsdaProviderEnabled,
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
import { searchUsdaGeneric, buildUsdaProviderConfig } from "../providers/usda";
import type { ErrorCode } from "../errors";

export interface GenericFoodLookupDeps {
  env: Env & { USDA_API_KEY?: string };
  query: string;
  today: string;
}

export interface GenericFoodLookupSuccess {
  ok: true;
  response: GenericFoodLookupResponse;
  cached: boolean;
}

export interface GenericFoodLookupFailure {
  ok: false;
  code: ErrorCode;
}

export type GenericFoodLookupResult =
  | GenericFoodLookupSuccess
  | GenericFoodLookupFailure;

function collapseWhitespace(raw: string): string {
  return raw.trim().replace(/\s+/g, " ");
}

function normalizeDisplayQuery(raw: string): string {
  return collapseWhitespace(raw);
}

function normalizeCacheQuery(raw: string): string {
  return collapseWhitespace(raw).toLowerCase();
}

export async function handleGenericFoodLookup(
  deps: GenericFoodLookupDeps
): Promise<GenericFoodLookupResult> {
  const { env, query, today } = deps;
  const displayQuery = normalizeDisplayQuery(query);
  const cacheQuery = normalizeCacheQuery(query);

  const cacheKey = buildCacheKey("usda", "generic", cacheQuery);
  const cacheEnabled = await getCacheEnabled(env);

  if (cacheEnabled) {
    const entry = await getCacheEntry(env, cacheKey);
    if (entry) {
      if (!isCacheEntryExpired(entry)) {
        const parsed = parseNormalizedJson(entry.normalized_json);
        if (parsed && isGenericFoodLookupResponse(parsed)) {
          await incrementCacheEntryHit(env, cacheKey);
          await incrementCacheHit(env, today);
          return {
            ok: true,
            response: { ...parsed, query: displayQuery },
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

  const usdaProviderEnabled = await getUsdaProviderEnabled(env);
  if (!usdaProviderEnabled) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "provider_disabled" };
  }

  const maxCalls = await getMaxDailyExternalCalls(env);
  const budgetExceeded = await isBudgetExceeded(env, today, maxCalls);
  if (budgetExceeded) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "budget_exceeded" };
  }

  const apiKey = env.USDA_API_KEY;
  if (!apiKey) {
    await incrementBlockedCall(env, today);
    return { ok: false, code: "configuration_error" };
  }

  await incrementExternalCall(env, today);

  const config = buildUsdaProviderConfig(apiKey);
  const providerResult = await searchUsdaGeneric(cacheQuery, config);

  if (providerResult.kind === "timeout") {
    return { ok: false, code: "provider_timeout" };
  }
  if (providerResult.kind === "rate_limited") {
    return { ok: false, code: "provider_rate_limited" };
  }
  if (providerResult.kind === "unavailable") {
    return { ok: false, code: "provider_unavailable" };
  }
  if (providerResult.kind === "error" || providerResult.kind === "unexpected") {
    return { ok: false, code: "provider_error" };
  }

  const { results } = providerResult;

  if (results.length === 0) {
    return { ok: false, code: "not_found" };
  }

  const response: GenericFoodLookupResponse = {
    query: displayQuery,
    source: "USDA",
    attribution: "USDA FoodData Central",
    isApproximate: true,
    results,
  };

  if (cacheEnabled) {
    const ttlSeconds = await getCacheTtlSeconds(env);
    const expiresAt = new Date(Date.now() + ttlSeconds * 1000).toISOString();
    await setCacheEntry(env, {
      cache_key: cacheKey,
      source: "usda",
      lookup_type: "generic",
      query: cacheQuery,
      normalized_json: JSON.stringify(response),
      attribution: "USDA FoodData Central",
      is_approximate: true,
      expires_at: expiresAt,
    });
  }

  return { ok: true, response, cached: false };
}
