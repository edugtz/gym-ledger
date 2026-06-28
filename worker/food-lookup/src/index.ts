import { PUBLIC_CONFIG } from "./config";
import { success, error } from "./response";

export interface Env {
  GYMLEDGER_API_KEY?: string;
}

export default {
  async fetch(request: Request, _env: Env): Promise<Response> {
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

    return error("not_found");
  },
};
