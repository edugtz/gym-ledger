import { Env } from "./db";

export interface FoodLookupCacheEntry {
  cache_key: string;
  source: string;
  lookup_type: string;
  query: string;
  normalized_json: string;
  attribution?: string;
  is_approximate: boolean;
  created_at: string;
  updated_at: string;
  expires_at?: string;
  hit_count: number;
  last_hit_at?: string;
}

export function buildCacheKey(
  source: string,
  lookupType: string,
  query: string
): string {
  return `${source}:${lookupType}:${query.toLowerCase().trim()}`;
}

export function isCacheEntryExpired(
  entry: FoodLookupCacheEntry
): boolean {
  if (!entry.expires_at) {
    return false;
  }
  const expiresAt = new Date(entry.expires_at);
  if (isNaN(expiresAt.getTime())) {
    return true;
  }
  return expiresAt.getTime() <= Date.now();
}

export function parseNormalizedJson<T>(
  json: string
): T | null {
  try {
    const parsed = JSON.parse(json);
    if (parsed === null || typeof parsed !== "object") {
      return null;
    }
    return parsed as T;
  } catch {
    return null;
  }
}

export async function getCacheEntry(
  env: Env,
  cacheKey: string
): Promise<FoodLookupCacheEntry | null> {
  const result = await env.DB.prepare(
    "SELECT * FROM food_lookup_cache WHERE cache_key = ?"
  ).bind(cacheKey).first<FoodLookupCacheEntry>();

  return result || null;
}

export async function setCacheEntry(
  env: Env,
  entry: Omit<FoodLookupCacheEntry, "created_at" | "updated_at" | "hit_count" | "last_hit_at">
): Promise<void> {
  const now = new Date().toISOString();

  await env.DB.prepare(
    `INSERT OR REPLACE INTO food_lookup_cache (
      cache_key, source, lookup_type, query, normalized_json, attribution, 
      is_approximate, created_at, updated_at, expires_at, hit_count, last_hit_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    entry.cache_key,
    entry.source,
    entry.lookup_type,
    entry.query,
    entry.normalized_json,
    entry.attribution || null,
    entry.is_approximate,
    now,
    now,
    entry.expires_at || null,
    0,
    null
  ).run();
}

export async function incrementCacheHit(
  env: Env,
  cacheKey: string
): Promise<void> {
  const now = new Date().toISOString();

  await env.DB.prepare(
    `UPDATE food_lookup_cache 
     SET hit_count = hit_count + 1, last_hit_at = ? 
     WHERE cache_key = ?`
  ).bind(now, cacheKey).run();
}
