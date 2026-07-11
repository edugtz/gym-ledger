export type ErrorCode =
  | "bad_request"
  | "unauthorized"
  | "forbidden"
  | "not_found"
  | "method_not_allowed"
  | "invalid_query"
  | "invalid_barcode"
  | "lookup_disabled"
  | "online_lookup_disabled"
  | "provider_disabled"
  | "feature_disabled"
  | "budget_exceeded"
  | "provider_timeout"
  | "provider_rate_limited"
  | "provider_unavailable"
  | "provider_error"
  | "configuration_error"
  | "internal_error";

export const ERROR_MESSAGES: Record<ErrorCode, string> = {
  bad_request: "Bad request",
  unauthorized: "Unauthorized",
  forbidden: "Forbidden",
  not_found: "Not found",
  method_not_allowed: "Method not allowed",
  invalid_query: "Invalid query",
  invalid_barcode: "Invalid barcode",
  lookup_disabled: "Lookup is disabled",
  online_lookup_disabled: "Online lookup is disabled",
  provider_disabled: "Provider is disabled",
  feature_disabled: "Feature is disabled",
  budget_exceeded: "Daily budget exceeded",
  provider_timeout: "Provider request timed out",
  provider_rate_limited: "Provider rate limited",
  provider_unavailable: "Provider unavailable",
  provider_error: "Provider error",
  configuration_error: "Configuration error",
  internal_error: "Internal server error",
};

export function httpStatusFor(code: ErrorCode): number {
  switch (code) {
    case "bad_request":
    case "invalid_query":
    case "invalid_barcode":
      return 400;
    case "unauthorized":
      return 401;
    case "forbidden":
      return 403;
    case "not_found":
      return 404;
    case "method_not_allowed":
      return 405;
    case "budget_exceeded":
    case "provider_rate_limited":
      return 429;
    case "lookup_disabled":
    case "online_lookup_disabled":
    case "provider_disabled":
    case "feature_disabled":
    case "configuration_error":
      return 503;
    case "provider_timeout":
      return 504;
    case "provider_unavailable":
      return 503;
    case "provider_error":
      return 502;
    case "internal_error":
    default:
      return 500;
  }
}
