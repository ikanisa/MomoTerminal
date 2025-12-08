-- Countries table for MomoTerminal
-- Each country has exactly one mobile money provider

CREATE TABLE IF NOT EXISTS countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(2) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    name_local VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    currency_symbol VARCHAR(10) NOT NULL,
    phone_prefix VARCHAR(5) NOT NULL,
    phone_length INTEGER NOT NULL DEFAULT 9,
    flag_emoji VARCHAR(10) DEFAULT '',
    provider_name VARCHAR(50) NOT NULL,
    provider_code VARCHAR(20) NOT NULL,
    provider_color VARCHAR(7) DEFAULT '#FFCC00',
    ussd_template VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Add ussd_template column if table exists but column doesn't
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'countries' AND column_name = 'ussd_template'
    ) THEN
        ALTER TABLE countries ADD COLUMN ussd_template VARCHAR(100) NOT NULL DEFAULT '*182*8*1*{merchant}*{amount}#';
    END IF;
END $$;

-- Index for active countries lookup
CREATE INDEX IF NOT EXISTS idx_countries_active ON countries(is_active) WHERE is_active = true;

-- Insert initial countries
INSERT INTO countries (code, name, name_local, currency, currency_symbol, phone_prefix, phone_length, flag_emoji, provider_name, provider_code, provider_color, ussd_template) VALUES
('RW', 'Rwanda', 'Rwanda', 'RWF', 'FRw', '+250', 9, '🇷🇼', 'MTN MoMo', 'MTN', '#FFCC00', '*182*8*1*{merchant}*{amount}#'),
('CD', 'DR Congo', 'RD Congo', 'CDF', 'FC', '+243', 9, '🇨🇩', 'Vodacom M-Pesa', 'VODACOM', '#E60000', '*150*1*1*{merchant}*{amount}#'),
('BI', 'Burundi', 'Burundi', 'BIF', 'FBu', '+257', 8, '🇧🇮', 'Lumicash', 'LUMICASH', '#00A651', '*150*1*{merchant}*{amount}#'),
('TZ', 'Tanzania', 'Tanzania', 'TZS', 'TSh', '+255', 9, '🇹🇿', 'Vodacom M-Pesa', 'VODACOM', '#E60000', '*150*00#{merchant}*{amount}#'),
('ZM', 'Zambia', 'Zambia', 'ZMW', 'ZK', '+260', 9, '🇿🇲', 'MTN MoMo', 'MTN', '#FFCC00', '*303*{merchant}*{amount}#'),
('GH', 'Ghana', 'Ghana', 'GHS', 'GH₵', '+233', 9, '🇬🇭', 'MTN MoMo', 'MTN', '#FFCC00', '*170*1*1*{merchant}*{amount}#'),
('SN', 'Senegal', 'Sénégal', 'XOF', 'CFA', '+221', 9, '🇸🇳', 'Orange Money', 'ORANGE', '#FF6600', '*144*1*{merchant}*{amount}#'),
('CI', 'Ivory Coast', 'Côte d''Ivoire', 'XOF', 'CFA', '+225', 10, '🇨🇮', 'Orange Money', 'ORANGE', '#FF6600', '*144*1*{merchant}*{amount}#'),
('CM', 'Cameroon', 'Cameroun', 'XAF', 'FCFA', '+237', 9, '🇨🇲', 'MTN MoMo', 'MTN', '#FFCC00', '*126*1*{merchant}*{amount}#'),
('BF', 'Burkina Faso', 'Burkina Faso', 'XOF', 'CFA', '+226', 8, '🇧🇫', 'Orange Money', 'ORANGE', '#FF6600', '*144*1*{merchant}*{amount}#'),
('ML', 'Mali', 'Mali', 'XOF', 'CFA', '+223', 8, '🇲🇱', 'Orange Money', 'ORANGE', '#FF6600', '*144*1*{merchant}*{amount}#'),
('NE', 'Niger', 'Niger', 'XOF', 'CFA', '+227', 8, '🇳🇪', 'Airtel Money', 'AIRTEL', '#ED1C24', '*555*1*{merchant}*{amount}#'),
('BJ', 'Benin', 'Bénin', 'XOF', 'CFA', '+229', 8, '🇧🇯', 'MTN MoMo', 'MTN', '#FFCC00', '*880*1*{merchant}*{amount}#'),
('TG', 'Togo', 'Togo', 'XOF', 'CFA', '+228', 8, '🇹🇬', 'T-Money', 'TMONEY', '#FF6B00', '*145*1*{merchant}*{amount}#'),
('MW', 'Malawi', 'Malawi', 'MWK', 'MK', '+265', 9, '🇲🇼', 'Airtel Money', 'AIRTEL', '#ED1C24', '*510*{merchant}*{amount}#'),
('ZW', 'Zimbabwe', 'Zimbabwe', 'ZWL', 'Z$', '+263', 9, '🇿🇼', 'EcoCash', 'ECOCASH', '#00A651', '*151*2*{merchant}*{amount}#')
ON CONFLICT (code) DO NOTHING;

-- Enable RLS
ALTER TABLE countries ENABLE ROW LEVEL SECURITY;

-- Allow read access to all authenticated users
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'countries' 
        AND policyname = 'Countries are viewable by authenticated users'
    ) THEN
        CREATE POLICY "Countries are viewable by authenticated users" ON countries
            FOR SELECT TO authenticated USING (true);
    END IF;
END $$;

-- Allow read access to anon users (for app startup)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies 
        WHERE tablename = 'countries' 
        AND policyname = 'Countries are viewable by anon'
    ) THEN
        CREATE POLICY "Countries are viewable by anon" ON countries
            FOR SELECT TO anon USING (is_active = true);
    END IF;
END $$;
