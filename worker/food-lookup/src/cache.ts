// Cache helpers for food lookup results

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
  // Create a stable cache key based on source, lookup type and query
  return `${source}:${lookupType}:${query.toLowerCase().trim()}`;
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