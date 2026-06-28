export interface Env {
  GYMLEDGER_API_KEY?: string;
}

export function validateApiKey(request: Request, env: Env): boolean {
  const configured = env.GYMLEDGER_API_KEY;
  if (!configured) {
    return true;
  }
  const provided = request.headers.get("X-GymLedger-Key");
  return provided === configured;
}
