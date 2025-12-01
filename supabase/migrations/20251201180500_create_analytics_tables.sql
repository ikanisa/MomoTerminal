-- Migration: Create analytics and monitoring tables
-- Description: Analytics events and error logging
-- Created: 2025-12-01

-- ============================================
-- Analytics Events Table
-- ============================================
CREATE TABLE IF NOT EXISTS public.analytics_events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    
    -- Event details
    event_name VARCHAR(100) NOT NULL,
    event_category VARCHAR(50),
    event_action VARCHAR(100),
    event_label VARCHAR(200),
    event_value NUMERIC,
    event_properties JSONB DEFAULT '{}'::jsonb,
    
    -- Context
    screen_name VARCHAR(100),
    previous_screen VARCHAR(100),
    session_id VARCHAR(100),
    session_duration_ms INT,
    
    -- User context
    user_type VARCHAR(50),
    is_new_user BOOLEAN,
    
    -- Technical context
    app_version VARCHAR(20),
    os_version VARCHAR(50),
    device_model VARCHAR(100),
    network_type VARCHAR(20),
    
    -- Location (if available)
    country VARCHAR(2),
    city VARCHAR(100),
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Partitioning helper
    created_date DATE GENERATED ALWAYS AS (DATE(created_at)) STORED
);

-- Indexes for analytics queries
CREATE INDEX idx_analytics_event_name ON public.analytics_events(event_name);
CREATE INDEX idx_analytics_category ON public.analytics_events(event_category);
CREATE INDEX idx_analytics_user_id ON public.analytics_events(user_id);
CREATE INDEX idx_analytics_device_id ON public.analytics_events(device_id);
CREATE INDEX idx_analytics_created ON public.analytics_events(created_at DESC);
CREATE INDEX idx_analytics_session ON public.analytics_events(session_id);
CREATE INDEX idx_analytics_screen ON public.analytics_events(screen_name);
CREATE INDEX idx_analytics_date ON public.analytics_events(created_date);

-- Enable RLS
ALTER TABLE public.analytics_events ENABLE ROW LEVEL SECURITY;

-- RLS Policies (users can only insert their own events, service role reads all)
CREATE POLICY "Users can log own events" 
    ON public.analytics_events
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role can read all events" 
    ON public.analytics_events
    FOR SELECT
    TO service_role
    USING (true);

-- ============================================
-- Error Logs Table
-- ============================================
CREATE TABLE IF NOT EXISTS public.error_logs (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    device_id UUID REFERENCES devices(id) ON DELETE SET NULL,
    
    -- Error classification
    error_type VARCHAR(100) NOT NULL,
    error_code VARCHAR(50),
    severity VARCHAR(20) CHECK (severity IN ('low', 'medium', 'high', 'critical')) DEFAULT 'medium',
    
    -- Error details
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    error_context JSONB DEFAULT '{}'::jsonb,
    
    -- Location in code
    component VARCHAR(100),
    function_name VARCHAR(100),
    file_path VARCHAR(200),
    line_number INT,
    
    -- Screen context
    screen_name VARCHAR(100),
    user_action VARCHAR(200),
    
    -- Technical context
    app_version VARCHAR(20),
    os_version VARCHAR(50),
    device_model VARCHAR(100),
    network_state VARCHAR(20),
    available_memory_mb INT,
    battery_level INT,
    
    -- Resolution tracking
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES auth.users(id),
    resolution_notes TEXT,
    
    -- Frequency tracking
    occurrence_count INT DEFAULT 1,
    first_occurred_at TIMESTAMPTZ DEFAULT NOW(),
    last_occurred_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Metadata
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Partitioning helper
    created_date DATE GENERATED ALWAYS AS (DATE(created_at)) STORED
);

-- Indexes
CREATE INDEX idx_error_logs_type ON public.error_logs(error_type);
CREATE INDEX idx_error_logs_severity ON public.error_logs(severity);
CREATE INDEX idx_error_logs_user_id ON public.error_logs(user_id);
CREATE INDEX idx_error_logs_device_id ON public.error_logs(device_id);
CREATE INDEX idx_error_logs_created ON public.error_logs(created_at DESC);
CREATE INDEX idx_error_logs_unresolved ON public.error_logs(is_resolved, severity) 
    WHERE is_resolved = FALSE;
CREATE INDEX idx_error_logs_component ON public.error_logs(component);
CREATE INDEX idx_error_logs_date ON public.error_logs(created_date);

-- Enable RLS
ALTER TABLE public.error_logs ENABLE ROW LEVEL SECURITY;

-- RLS Policies
CREATE POLICY "Users can log own errors" 
    ON public.error_logs
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role can manage all errors" 
    ON public.error_logs
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Auto-update trigger
CREATE TRIGGER update_error_logs_updated_at
    BEFORE UPDATE ON public.error_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();

-- ============================================
-- Analytics Summary Tables (Materialized Views)
-- ============================================

-- Daily transaction summary
CREATE MATERIALIZED VIEW IF NOT EXISTS public.daily_transaction_summary AS
SELECT
    user_id,
    DATE(timestamp) AS transaction_date,
    COUNT(*) AS total_transactions,
    SUM(amount) AS total_amount,
    AVG(amount) AS average_amount,
    COUNT(*) FILTER (WHERE status = 'SENT') AS successful_count,
    COUNT(*) FILTER (WHERE status = 'FAILED') AS failed_count,
    COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_count,
    COUNT(DISTINCT provider) AS providers_used,
    MIN(timestamp) AS first_transaction_at,
    MAX(timestamp) AS last_transaction_at
FROM public.transactions
GROUP BY user_id, DATE(timestamp);

-- Create index on materialized view
CREATE UNIQUE INDEX idx_daily_summary_user_date 
    ON public.daily_transaction_summary(user_id, transaction_date);

-- Comments
COMMENT ON TABLE public.analytics_events IS 'User behavior and app usage tracking';
COMMENT ON TABLE public.error_logs IS 'Application error and crash reporting';
COMMENT ON MATERIALIZED VIEW public.daily_transaction_summary IS 'Pre-aggregated daily transaction metrics';
