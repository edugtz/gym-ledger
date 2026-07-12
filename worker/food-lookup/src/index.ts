import { MIN_QUERY_LENGTH } from "./config";
import { success, error } from "./response";
import { Env as DBEnv } from "./db";
import { validateApiKey } from "./auth";
import { handleGenericFoodLookup } from "./services/genericFoodLookup";
import { handleBarcodeFoodLookup } from "./services/barcodeFoodLookup";
import { normalizeAndValidateBarcode } from "./barcode";
import {
  getSafeMode,
  getOnlineLookupAvailable,
  getUsdaProviderEnabled,
  getOpenFoodFactsProviderEnabled,
  getGenericFoodSearchEnabled,
  getBarcodeLookupEnabled,
} from "./runtimeConfig";

export interface Env extends DBEnv {
  GYMLEDGER_API_KEY?: string;
  USDA_API_KEY?: string;
  OPEN_FOOD_FACTS_USER_AGENT?: string;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    const { pathname } = url;
    const method = request.method;

    if (pathname === "/v1/health") {
      if (method !== "GET") {
        return error("method_not_allowed");
      }
      return success({ status: "ok" });
    }

    if (pathname === "/v1/config") {
      if (method !== "GET") {
        return error("method_not_allowed");
      }
      const config = {
        onlineLookupAvailable: await getOnlineLookupAvailable(env),
        providers: {
          usda: await getUsdaProviderEnabled(env),
          openFoodFacts: await getOpenFoodFactsProviderEnabled(env),
        },
        features: {
          genericFoodSearch: await getGenericFoodSearchEnabled(env),
          barcodeLookup: await getBarcodeLookupEnabled(env),
        },
        minQueryLength: MIN_QUERY_LENGTH,
        safeMode: await getSafeMode(env),
      };
      return success(config);
    }

    if (pathname === "/v1/foods/generic") {
      if (method !== "GET") {
        return error("method_not_allowed");
      }

      const q = url.searchParams.get("q") ?? "";
      const trimmed = q.trim();

      if (!trimmed || trimmed.length < MIN_QUERY_LENGTH) {
        return error("invalid_query");
      }

      if (!validateApiKey(request, env)) {
        return error("unauthorized");
      }

      const today = new Date().toISOString().slice(0, 10);
      const result = await handleGenericFoodLookup({ env, query: trimmed, today });

      if (result.ok) {
        return success(result.response);
      }

      return error(result.code);
    }

    const barcodePrefix = "/v1/foods/barcode/";
    if (pathname.startsWith(barcodePrefix)) {
      if (method !== "GET") {
        return error("method_not_allowed");
      }

      const remainder = pathname.slice(barcodePrefix.length);
      if (remainder.length === 0 || remainder.includes("/")) {
        return error("invalid_barcode");
      }

      let decoded: string;
      try {
        decoded = decodeURIComponent(remainder);
      } catch {
        return error("invalid_barcode");
      }

      if (decoded !== remainder) {
        return error("invalid_barcode");
      }

      const barcodeResult = normalizeAndValidateBarcode(decoded);
      if (!barcodeResult.ok) {
        return error("invalid_barcode");
      }

      if (!validateApiKey(request, env)) {
        return error("unauthorized");
      }

      const today = new Date().toISOString().slice(0, 10);
      const result = await handleBarcodeFoodLookup({
        env,
        barcode: barcodeResult.barcode,
        today,
      });

      if (result.ok) {
        return success(result.response);
      }

      return error(result.code);
    }

    return error("not_found");
  },
};
