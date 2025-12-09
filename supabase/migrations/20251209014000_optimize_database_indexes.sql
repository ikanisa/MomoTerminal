-- Migration: Database Performance Optimization
-- Description: Add indexes for frequently queried columns
-- Created: 2025-12-09
-- Phase: 2 - Major Fixes

-- =====================================================
-- INDEXES FOR user_profiles TABLE
-- =====================================================

-- Index for fetching profile by phone number (used in auth lookups)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_profiles_phone_number 
  ON user_profiles(phone_number);

-- Index for fetching profile by MoMo phone (payment lookups)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_profiles_momo_phone 
  ON user_profiles(momo_phone) 
  WHERE momo_phone IS NOT NULL;

-- Index for finding profiles by country (analytics)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_profiles_country 
  ON user_profiles(country_code, momo_country_code);

-- =====================================================
-- INDEXES FOR wallets TABLE
-- =====================================================

-- Index for active wallets with balance (common query)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wallets_active_balance 
  ON wallets(user_id, balance) 
  WHERE status = 'ACTIVE';

-- Index for wallet by currency
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wallets_currency 
  ON wallets(currency, status);

-- =====================================================
-- INDEXES FOR wallet_ledger TABLE
-- =====================================================

-- Composite index for user transaction history (most common query)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wallet_ledger_wallet_created 
  ON wallet_ledger(wallet_id, created_at DESC);

-- Index for transaction lookup by type and date
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wallet_ledger_type_date 
  ON wallet_ledger(type, created_at DESC);

-- Index for finding transactions by reference (refunds, reversals)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wallet_ledger_reference 
  ON wallet_ledger(reference_id) 
  WHERE reference_id IS NOT NULL;

-- =====================================================
-- INDEXES FOR transactions TABLE
-- =====================================================

-- Composite index for user transaction history (SMS sync)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_user_timestamp 
  ON transactions(user_id, timestamp DESC);

-- Index for finding pending transactions
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_pending 
  ON transactions(user_id, status) 
  WHERE status = 'PENDING';

-- Index for transaction lookup by provider
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_provider 
  ON transactions(provider, provider_type, created_at DESC);

-- Index for duplicate detection (sync optimization)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_transactions_dedup 
  ON transactions(user_id, transaction_id) 
  WHERE transaction_id IS NOT NULL;

-- =====================================================
-- INDEXES FOR vending_orders TABLE (if exists)
-- =====================================================

-- Check if vending_orders table exists before creating indexes
DO $$
BEGIN
  IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'vending_orders') THEN
    -- Index for user orders
    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vending_orders_user 
      ON vending_orders(user_id, created_at DESC);
    
    -- Index for machine orders
    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vending_orders_machine 
      ON vending_orders(machine_id, status, created_at DESC);
    
    -- Index for pending orders
    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vending_orders_pending 
      ON vending_orders(status, created_at DESC) 
      WHERE status IN ('PENDING', 'PROCESSING');
  END IF;
END $$;

-- =====================================================
-- INDEXES FOR vending_machines TABLE (if exists)
-- =====================================================

DO $$
BEGIN
  IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'vending_machines') THEN
    -- Index for active machines by location
    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vending_machines_active 
      ON vending_machines(status, location) 
      WHERE status = 'ACTIVE';
    
    -- Index for machine inventory
    CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_vending_machines_stock 
      ON vending_machines(id, updated_at DESC);
  END IF;
END $$;

-- =====================================================
-- VACUUM AND ANALYZE
-- =====================================================

-- Update table statistics for query planner
ANALYZE user_profiles;
ANALYZE wallets;
ANALYZE wallet_ledger;
ANALYZE transactions;

-- Vacuum to reclaim storage and update visibility map
-- (Run VACUUM FULL during maintenance window if needed)

-- =====================================================
-- COMMENTS
-- =====================================================

COMMENT ON INDEX idx_user_profiles_phone_number IS 'Optimize auth lookups by phone number';
COMMENT ON INDEX idx_user_profiles_momo_phone IS 'Optimize payment lookups by MoMo phone';
COMMENT ON INDEX idx_wallets_active_balance IS 'Optimize balance queries for active wallets';
COMMENT ON INDEX idx_wallet_ledger_wallet_created IS 'Optimize transaction history queries';
COMMENT ON INDEX idx_transactions_user_timestamp IS 'Optimize SMS transaction sync queries';

-- =====================================================
-- MONITORING QUERIES
-- =====================================================

-- Check index usage:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan DESC;

-- Check table sizes:
-- SELECT tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
-- FROM pg_tables
-- WHERE schemaname = 'public'
-- ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Find slow queries:
-- SELECT query, mean_exec_time, calls
-- FROM pg_stat_statements
-- ORDER BY mean_exec_time DESC
-- LIMIT 10;
