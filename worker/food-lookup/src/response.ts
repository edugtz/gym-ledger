import { ErrorCode, ERROR_MESSAGES, httpStatusFor } from "./errors";

export interface SuccessResponse<T> {
  ok: true;
  data: T;
}

export interface ErrorBody {
  code: ErrorCode;
  message: string;
}

export interface ErrorResponse {
  ok: false;
  error: ErrorBody;
}

export function success<T>(data: T, init?: ResponseInit): Response {
  const body: SuccessResponse<T> = { ok: true, data };
  return Response.json(body, { status: 200, ...init });
}

export function error(code: ErrorCode, message?: string, init?: ResponseInit): Response {
  const body: ErrorResponse = {
    ok: false,
    error: {
      code,
      message: message ?? ERROR_MESSAGES[code],
    },
  };
  return Response.json(body, { status: httpStatusFor(code), ...init });
}
