export type ErrorCode =
  | "bad_request"
  | "unauthorized"
  | "forbidden"
  | "not_found"
  | "method_not_allowed"
  | "online_lookup_disabled"
  | "provider_disabled"
  | "budget_exceeded"
  | "provider_timeout"
  | "provider_error"
  | "internal_error";

export const ERROR_MESSAGES: Record<ErrorCode, string> = {
  bad_request: "Bad request",
  unauthorized: "Unauthorized",
  forbidden: "Forbidden",
  not_found: "Route not found",
  method_not_allowed: "Method not allowed",
  online_lookup_disabled: "Online lookup is disabled",
  provider_disabled: "Provider is disabled",
  budget_exceeded: "Budget exceeded",
  provider_timeout: "Provider request timed out",
  provider_error: "Provider returned an error",
  internal_error: "Internal server error",
};

export function httpStatusFor(code: ErrorCode): number {
  switch (code) {
    case "bad_request":
      return 400;
    case "unauthorized":
      return 401;
    case "forbidden":
      return 403;
    case "not_found":
      return 404;
    case "method_not_allowed":
      return 405;
    case "online_lookup_disabled":
    case "provider_disabled":
    case "budget_exceeded":
      return 424;
    case "provider_timeout":
      return 504;
    case "provider_error":
    case "internal_error":
    default:
      return 500;
  }
}
