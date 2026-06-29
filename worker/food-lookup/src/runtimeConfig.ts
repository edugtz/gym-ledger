// Runtime configuration helpers with safe fallbacks

import { Env } from "./db";

export interface RuntimeConfigEntry {
  key: string;
  value: string;
  updated_at: string;
}

// Default configuration values with safe fallbacks
const DEFAULT_CONFIG = {
  // Safe mode defaults to true to prevent unintended provider calls
  safe_mode: "true",

  // Provider behavior remains disabled by default
  online_lookup_enabled: "false",
  usda_provider_enabled: "false",
  open_food_facts_provider_enabled: "false",

  // Feature flags disabled by default
  generic_food_search_enabled: "false",
  barcode_lookup_enabled: "false",

  // Conservative daily budget
  daily_external_call_budget: "25",

  // Cache behavior
  cache_enabled: "true",
  cache_ttl_seconds: "86400", // 24 hours
};

export async function getRuntimeConfig(
  env: Env,
  key: string,
  defaultValue?: string
): Promise<string> {
  const result = await env.DB.prepare(
    "SELECT value FROM runtime_config WHERE key = ?"
  ).bind(key).first<{ value: string }>();
  
  if (result) {
    return result.value;
  }
  
  // Return default value if not found in DB
  const dbDefault = DEFAULT_CONFIG[key as keyof typeof DEFAULT_CONFIG];
  return defaultValue !== undefined ? defaultValue : dbDefault || "";
}

export async function setRuntimeConfig(
  env: Env,
  key: string,
  value: string
): Promise<void> {
  const now = new Date().toISOString();
  
  await env.DB.prepare(
    `INSERT OR REPLACE INTO runtime_config (key, value, updated_at) VALUES (?, ?, ?)`
  ).bind(key, value, now).run();
}

export async function getSafeMode(env: Env): Promise<boolean> {
  const value = await getRuntimeConfig(env, "safe_mode", DEFAULT_CONFIG.safe_mode);
  return value === "true";
}

export async function getOnlineLookupAvailable(env: Env): Promise<boolean> {
  const value = await getRuntimeConfig(env, "online_lookup_enabled", DEFAULT_CONFIG.online_lookup_enabled);
  return value === "true";
}

export async function getUsdaProviderEnabled(env: Env): Promise<boolean> {
  const value = await getRuntimeConfig(env, "usda_provider_enabled", DEFAULT_CONFIG.usda_provider_enabled);
  return value === "true";
}

export async function getOpenFoodFactsProviderEnabled(env: Env): Promise<boolean> {
  const value = await getRuntimeConfig(env, "open_food_facts_provider_enabled", DEFAULT_CONFIG.open_food_facts_provider_enabled);
  return value === "true";
}

export async function getMaxDailyExternalCalls(env: Env): Promise<number> {
  const value = await getRuntimeConfig(env, "daily_external_call_budget", DEFAULT_CONFIG.daily_external_call_budget);
  return parseInt(value, 10) || 25;
}