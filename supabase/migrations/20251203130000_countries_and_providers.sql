-- Countries and Mobile Money Providers Configuration
-- Each country has ONE authorized mobile money provider

-- Countries table with their single authorized provider
CREATE TABLE IF NOT EXISTS countries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(2) NOT NULL UNIQUE,           -- ISO 3166-1 alpha-2
    name VARCHAR(100) NOT NULL,
    name_local VARCHAR(100) NOT NULL,
    currency VARCHAR(3) NOT NULL,              -- ISO 4217
    currency_symbol VARCHAR(10) NOT NULL,
    phone_prefix VARCHAR(10) NOT NULL,
    language VARCHAR(5) NOT NULL,              -- Primary language code
    provider_name VARCHAR(50) NOT NULL,        -- Single authorized provider
    provider_display_name VARCHAR(100) NOT NULL,
    provider_color VARCHAR(7) NOT NULL,        -- Hex color for UI
    is_active BOOLEAN DEFAULT true,
    is_primary_market BOOLEAN DEFAULT false,   -- Primary launch countries
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insert all supported countries with their single authorized provider
INSERT INTO countries (code, name, name_local, currency, currency_symbol, phone_prefix, language, provider_name, provider_display_name, provider_color, is_primary_market) VALUES
-- Primary Launch Countries
('RW', 'Rwanda', 'Rwanda', 'RWF', 'FRw', '+250', 'rw', 'MTN', 'MTN MoMo', '#FFCC00', true),
('CD', 'DR Congo', 'RD Congo', 'CDF', 'FC', '+243', 'fr', 'ORANGE', 'Orange Money', '#FF6600', true),
('BI', 'Burundi', 'Burundi', 'BIF', 'FBu', '+257', 'fr', 'ECOCASH', 'EcoCash', '#00A651', true),
('TZ', 'Tanzania', 'Tanzania', 'TZS', 'TSh', '+255', 'sw', 'VODACOM', 'M-Pesa', '#E60000', true),
('ZM', 'Zambia', 'Zambia', 'ZMW', 'ZK', '+260', 'en', 'MTN', 'MTN MoMo', '#FFCC00', true),

-- West Africa - French Speaking
('SN', 'Senegal', 'Sénégal', 'XOF', 'CFA', '+221', 'fr', 'ORANGE', 'Orange Money', '#FF6600', false),
('CI', 'Ivory Coast', 'Côte d''Ivoire', 'XOF', 'CFA', '+225', 'fr', 'ORANGE', 'Orange Money', '#FF6600', false),
('CM', 'Cameroon', 'Cameroun', 'XAF', 'FCFA', '+237', 'fr', 'MTN', 'MTN MoMo', '#FFCC00', false),
('ML', 'Mali', 'Mali', 'XOF', 'CFA', '+223', 'fr', 'ORANGE', 'Orange Money', '#FF6600', false),
('BF', 'Burkina Faso', 'Burkina Faso', 'XOF', 'CFA', '+226', 'fr', 'ORANGE', 'Orange Money', '#FF6600', false),
('NE', 'Niger', 'Niger', 'XOF', 'CFA', '+227', 'fr', 'AIRTEL', 'Airtel Money', '#ED1C24', false),
('BJ', 'Benin', 'Bénin', 'XOF', 'CFA', '+229', 'fr', 'MTN', 'MTN MoMo', '#FFCC00', false),
('TG', 'Togo', 'Togo', 'XOF', 'CFA', '+228', 'fr', 'TMONEY', 'T-Money', '#FF6B00', false),
('GN', 'Guinea', 'Guinée', 'GNF', 'FG', '+224', 'fr', 'ORANGE', 'Orange Money', '#FF6600', false),
('TD', 'Chad', 'Tchad', 'XAF', 'FCFA', '+235', 'fr', 'AIRTEL', 'Airtel Money', '#ED1C24', false),
('CF', 'Central African Republic', 'République Centrafricaine', 'XAF', 'FCFA', '+236', 'fr', 'ORANGE', 'Orange Money', '#FF6600', false),
('GA', 'Gabon', 'Gabon', 'XAF', 'FCFA', '+241', 'fr', 'AIRTEL', 'Airtel Money', '#ED1C24', false),
('CG', 'Congo', 'Congo-Brazzaville', 'XAF', 'FCFA', '+242', 'fr', 'MTN', 'MTN MoMo', '#FFCC00', false),

-- East/Southern Africa - English Speaking
('GH', 'Ghana', 'Ghana', 'GHS', 'GH₵', '+233', 'en', 'MTN', 'MTN MoMo', '#FFCC00', false),
('MW', 'Malawi', 'Malawi', 'MWK', 'MK', '+265', 'en', 'AIRTEL', 'Airtel Money', '#ED1C24', false),
('ZW', 'Zimbabwe', 'Zimbabwe', 'ZWL', 'Z$', '+263', 'en', 'ECOCASH', 'EcoCash', '#00A651', false),
('MZ', 'Mozambique', 'Moçambique', 'MZN', 'MT', '+258', 'pt', 'VODACOM', 'M-Pesa', '#E60000', false),
('BW', 'Botswana', 'Botswana', 'BWP', 'P', '+267', 'en', 'ORANGE', 'Orange Money', '#FF6600', false),
('NA', 'Namibia', 'Namibia', 'NAD', 'N$', '+264', 'en', 'MTC', 'MTC MoMo', '#0066B3', false),
('LS', 'Lesotho', 'Lesotho', 'LSL', 'L', '+266', 'en', 'VODACOM', 'M-Pesa', '#E60000', false),
('SZ', 'Eswatini', 'Eswatini', 'SZL', 'E', '+268', 'en', 'MTN', 'MTN MoMo', '#FFCC00', false),
('LR', 'Liberia', 'Liberia', 'LRD', 'L$', '+231', 'en', 'ORANGE', 'Orange Money', '#FF6600', false),
('SL', 'Sierra Leone', 'Sierra Leone', 'SLL', 'Le', '+232', 'en', 'ORANGE', 'Orange Money', '#FF6600', false),
('GM', 'Gambia', 'Gambia', 'GMD', 'D', '+220', 'en', 'AFRICELL', 'Africell Money', '#00A0DF', false),

-- Island Nations
('MG', 'Madagascar', 'Madagascar', 'MGA', 'Ar', '+261', 'fr', 'MVOLA', 'MVola', '#E31937', false),
('KM', 'Comoros', 'Comores', 'KMF', 'CF', '+269', 'fr', 'MVOLA', 'MVola', '#E31937', false),
('SC', 'Seychelles', 'Seychelles', 'SCR', 'SCR', '+248', 'en', 'AIRTEL', 'Airtel Money', '#ED1C24', false)

ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    name_local = EXCLUDED.name_local,
    currency = EXCLUDED.currency,
    currency_symbol = EXCLUDED.currency_symbol,
    phone_prefix = EXCLUDED.phone_prefix,
    language = EXCLUDED.language,
    provider_name = EXCLUDED.provider_name,
    provider_display_name = EXCLUDED.provider_display_name,
    provider_color = EXCLUDED.provider_color,
    is_primary_market = EXCLUDED.is_primary_market,
    updated_at = NOW();

-- Index for fast lookups
CREATE INDEX IF NOT EXISTS idx_countries_code ON countries(code);
CREATE INDEX IF NOT EXISTS idx_countries_active ON countries(is_active);
CREATE INDEX IF NOT EXISTS idx_countries_primary ON countries(is_primary_market);

-- Allow public read access (no auth required for country configs)
ALTER TABLE countries ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read countries" ON countries FOR SELECT USING (true);

-- Update merchants table to support separate profile country and mobile money country
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS profile_country_code VARCHAR(2) DEFAULT 'RW';
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS momo_country_code VARCHAR(2) DEFAULT 'RW';

-- Function to get country details
CREATE OR REPLACE FUNCTION get_country_by_code(country_code VARCHAR)
RETURNS TABLE (
    code VARCHAR,
    name VARCHAR,
    currency VARCHAR,
    currency_symbol VARCHAR,
    phone_prefix VARCHAR,
    provider_name VARCHAR,
    provider_display_name VARCHAR,
    provider_color VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.code::VARCHAR,
        c.name::VARCHAR,
        c.currency::VARCHAR,
        c.currency_symbol::VARCHAR,
        c.phone_prefix::VARCHAR,
        c.provider_name::VARCHAR,
        c.provider_display_name::VARCHAR,
        c.provider_color::VARCHAR
    FROM countries c
    WHERE c.code = country_code AND c.is_active = true;
END;
$$ LANGUAGE plpgsql;

-- Function to get all active countries
CREATE OR REPLACE FUNCTION get_active_countries()
RETURNS TABLE (
    code VARCHAR,
    name VARCHAR,
    name_local VARCHAR,
    currency VARCHAR,
    currency_symbol VARCHAR,
    phone_prefix VARCHAR,
    language VARCHAR,
    provider_name VARCHAR,
    provider_display_name VARCHAR,
    provider_color VARCHAR,
    is_primary_market BOOLEAN
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.code::VARCHAR,
        c.name::VARCHAR,
        c.name_local::VARCHAR,
        c.currency::VARCHAR,
        c.currency_symbol::VARCHAR,
        c.phone_prefix::VARCHAR,
        c.language::VARCHAR,
        c.provider_name::VARCHAR,
        c.provider_display_name::VARCHAR,
        c.provider_color::VARCHAR,
        c.is_primary_market
    FROM countries c
    WHERE c.is_active = true
    ORDER BY c.is_primary_market DESC, c.name ASC;
END;
$$ LANGUAGE plpgsql;
