import { PUBLIC_CONFIG } from "./config";
import { success, error } from "./response";
import { Env as DBEnv } from "./db";
import { handleGenericFoodLookup } from "./services/genericFoodLookup";
import { handleBarcodeFoodLookup } from "./services/barcodeFoodLookup";
import { normalizeAndValidateBarcode } from "./barcode";

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
      return success(PUBLIC_CONFIG);
    }

    if (pathname === "/v1/foods/generic") {
      if (method !== "GET") {
        return error("method_not_allowed");
      }

      const q = url.searchParams.get("q") ?? "";
      const trimmed = q.trim();

      if (!trimmed || trimmed.length < PUBLIC_CONFIG.minQueryLength) {
        return error("invalid_query");
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
