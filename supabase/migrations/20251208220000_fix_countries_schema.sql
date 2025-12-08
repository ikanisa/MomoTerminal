-- Fix countries table schema by adding missing ussd_template column
-- This migration ensures the table has all required columns before inserting data

DO $$ 
BEGIN
    -- Add ussd_template column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'countries' AND column_name = 'ussd_template'
    ) THEN
        ALTER TABLE countries ADD COLUMN ussd_template VARCHAR(100) NOT NULL DEFAULT '*182*8*1*{merchant}*{amount}#';
    END IF;
END $$;
