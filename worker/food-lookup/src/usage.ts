// Usage tracking helpers for daily usage and budget tracking

import { Env } from "./db";

export interface UsageDailyEntry {
  usage_date: string;
  external_calls: number;
  cache_hits: number;
  cache_misses: number;
  blocked_calls: number;
  last_updated_at: string;
}

export async function getDailyUsage(
  env: Env,
  date: string
): Promise<UsageDailyEntry | null> {
  const result = await env.DB.prepare(
    "SELECT * FROM usage_daily WHERE usage_date = ?"
  ).bind(date).first<UsageDailyEntry>();
  
  return result || null;
}

export async function incrementExternalCall(
  env: Env,
  date: string
): Promise<void> {
  await env.DB.prepare(
    `INSERT OR REPLACE INTO usage_daily (
      usage_date, external_calls, cache_hits, cache_misses, blocked_calls, last_updated_at
    ) VALUES (?, COALESCE((SELECT external_calls FROM usage_daily WHERE usage_date = ?), 0) + 1, 0, 0, 0, CURRENT_TIMESTAMP)
    ON CONFLICT(usage_date) DO UPDATE SET 
      external_calls = external_calls + 1,
      last_updated_at = CURRENT_TIMESTAMP`
  ).bind(date, date).run();
}

export async function incrementCacheHit(
  env: Env,
  date: string
): Promise<void> {
  await env.DB.prepare(
    `INSERT OR REPLACE INTO usage_daily (
      usage_date, external_calls, cache_hits, cache_misses, blocked_calls, last_updated_at
    ) VALUES (?, 0, COALESCE((SELECT cache_hits FROM usage_daily WHERE usage_date = ?), 0) + 1, 0, 0, CURRENT_TIMESTAMP)
    ON CONFLICT(usage_date) DO UPDATE SET 
      cache_hits = cache_hits + 1,
      last_updated_at = CURRENT_TIMESTAMP`
  ).bind(date, date).run();
}

export async function incrementCacheMiss(
  env: Env,
  date: string
): Promise<void> {
  await env.DB.prepare(
    `INSERT OR REPLACE INTO usage_daily (
      usage_date, external_calls, cache_hits, cache_misses, blocked_calls, last_updated_at
    ) VALUES (?, 0, 0, COALESCE((SELECT cache_misses FROM usage_daily WHERE usage_date = ?), 0) + 1, 0, CURRENT_TIMESTAMP)
    ON CONFLICT(usage_date) DO UPDATE SET 
      cache_misses = cache_misses + 1,
      last_updated_at = CURRENT_TIMESTAMP`
  ).bind(date, date).run();
}

export async function incrementBlockedCall(
  env: Env,
  date: string
): Promise<void> {
  await env.DB.prepare(
    `INSERT OR REPLACE INTO usage_daily (
      usage_date, external_calls, cache_hits, cache_misses, blocked_calls, last_updated_at
    ) VALUES (?, 0, 0, 0, COALESCE((SELECT blocked_calls FROM usage_daily WHERE usage_date = ?), 0) + 1, CURRENT_TIMESTAMP)
    ON CONFLICT(usage_date) DO UPDATE SET 
      blocked_calls = blocked_calls + 1,
      last_updated_at = CURRENT_TIMESTAMP`
  ).bind(date, date).run();
}

export async function getDailyUsageStats(
  env: Env,
  date: string
): Promise<{
  externalCalls: number;
  cacheHits: number;
  cacheMisses: number;
  blockedCalls: number;
}> {
  const result = await env.DB.prepare(
    "SELECT external_calls, cache_hits, cache_misses, blocked_calls FROM usage_daily WHERE usage_date = ?"
  ).bind(date).first<{
    external_calls: number;
    cache_hits: number;
    cache_misses: number;
    blocked_calls: number;
  }>();
  
  if (!result) {
    return {
      externalCalls: 0,
      cacheHits: 0,
      cacheMisses: 0,
      blockedCalls: 0
    };
  }
  
  return {
    externalCalls: result.external_calls,
    cacheHits: result.cache_hits,
    cacheMisses: result.cache_misses,
    blockedCalls: result.blocked_calls
  };
}

export async function isBudgetExceeded(
  env: Env,
  date: string,
  maxExternalCalls: number = 25
): Promise<boolean> {
  const stats = await getDailyUsageStats(env, date);
  return stats.externalCalls >= maxExternalCalls;
}