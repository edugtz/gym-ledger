-- Migration: cache_budget_foundation
-- Created at: 2025-06-28 10:00:00

-- Create food_lookup_cache table
CREATE TABLE IF NOT EXISTS food_lookup_cache (
  cache_key TEXT PRIMARY KEY,
  source TEXT NOT NULL,
  lookup_type TEXT NOT NULL,
  query TEXT NOT NULL,
  normalized_json TEXT NOT NULL,
  attribution TEXT,
  is_approximate BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME,
  hit_count INTEGER DEFAULT 0,
  last_hit_at DATETIME
);

-- Create usage_daily table
CREATE TABLE IF NOT EXISTS usage_daily (
  usage_date DATE PRIMARY KEY,
  external_calls INTEGER DEFAULT 0,
  cache_hits INTEGER DEFAULT 0,
  cache_misses INTEGER DEFAULT 0,
  blocked_calls INTEGER DEFAULT 0,
  last_updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Create runtime_config table
CREATE TABLE IF NOT EXISTS runtime_config (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);