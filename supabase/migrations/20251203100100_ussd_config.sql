-- USSD Configuration Table
-- Stores mobile money USSD codes for each country/provider

CREATE TABLE IF NOT EXISTS ussd_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    country_code VARCHAR(2) NOT NULL,
    country_name VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    base_code VARCHAR(20) NOT NULL,
    send_to_phone VARCHAR(100) NOT NULL,  -- Template with {phone}, {amount}
    pay_merchant VARCHAR(100) NOT NULL,   -- Template with {merchant}, {amount}
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(country_code, provider)
);

-- Insert all USSD configurations
INSERT INTO ussd_configs (country_code, country_name, provider, currency, base_code, send_to_phone, pay_merchant) VALUES
-- Primary Launch Countries
('RW', 'Rwanda', 'MTN', 'RWF', '*182#', '*182*1*1*{phone}*{amount}#', '*182*8*1*{merchant}*{amount}#'),
('CD', 'DR Congo', 'Orange', 'CDF', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#'),
('BI', 'Burundi', 'EcoCash', 'BIF', '*151#', '*151*1*1*{phone}*{amount}#', '*151*1*2*{merchant}*{amount}#'),
('TZ', 'Tanzania', 'Vodacom M-Pesa', 'TZS', '*150*00#', '*150*00*{phone}*{amount}#', '*150*00*{merchant}*{amount}#'),
('ZM', 'Zambia', 'MTN', 'ZMW', '*115#', '*115*2*{phone}*{amount}#', '*115*5*{merchant}*{amount}#'),

-- West Africa - French Speaking
('SN', 'Senegal', 'Orange', 'XOF', '#144#', '#144*1*{phone}*{amount}#', '#144*2*{merchant}*{amount}#'),
('CI', 'Côte d''Ivoire', 'Orange', 'XOF', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#'),
('ML', 'Mali', 'Orange', 'XOF', '#144#', '#144#*1*{phone}*{amount}#', '#144#*2*{merchant}*{amount}#'),
('BF', 'Burkina Faso', 'Orange', 'XOF', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#'),
('NE', 'Niger', 'Airtel', 'XOF', '*400#', '*400*{phone}*{amount}#', '*400*{merchant}*{amount}#'),
('BJ', 'Benin', 'MTN', 'XOF', '*880#', '*880*1*{phone}*{amount}#', '*880*3*{merchant}*{amount}#'),
('TG', 'Togo', 'T-Money', 'XOF', '*145#', '*145*1*{amount}*{phone}#', '*145*3*{merchant}*{amount}#'),
('GN', 'Guinea', 'Orange', 'GNF', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#'),
('MR', 'Mauritania', 'Moov', 'MRU', '*900#', '*900*2*{phone}*{amount}#', '*900*4*{merchant}*{amount}#'),

-- Central Africa - French Speaking
('CM', 'Cameroon', 'MTN', 'XAF', '*126#', '*126*2*{phone}*{amount}#', '*126*4*{merchant}*{amount}#'),
('GA', 'Gabon', 'Airtel', 'XAF', '*150#', '*150*2*{phone}*{amount}#', '*150*4*{merchant}*{amount}#'),
('CG', 'Congo', 'MTN', 'XAF', '*133#', '*133*2*{phone}*{amount}#', '*133*5*{merchant}*{amount}#'),
('CF', 'Central African Rep.', 'Orange', 'XAF', '#150#', '#150*2*{phone}*{amount}#', '#150*4*{merchant}*{amount}#'),
('TD', 'Chad', 'Airtel', 'XAF', '*211#', '*211*{phone}*{amount}#', '*211*{merchant}*{amount}#'),
('GQ', 'Equatorial Guinea', 'GETESA', 'XAF', '*222#', '*222*2*{phone}*{amount}#', '*222*4*{merchant}*{amount}#'),

-- East/Southern Africa - English Speaking
('MW', 'Malawi', 'Airtel', 'MWK', '*211#', '*211*{phone}*{amount}#', '*211*{merchant}*{amount}#'),
('ZW', 'Zimbabwe', 'EcoCash', 'ZWL', '*151#', '*151*1*1*{phone}*{amount}#', '*151*2*{merchant}*{amount}#'),
('NA', 'Namibia', 'MTC', 'NAD', '*140*682#', '*140*682*{phone}*{amount}#', '*140*682*{merchant}*{amount}#'),

-- Ghana (secondary market)
('GH', 'Ghana', 'MTN', 'GHS', '*170#', '*170*1*1*{phone}*{amount}#', '*170*2*1*{merchant}*{amount}#'),

-- Island Nations
('MG', 'Madagascar', 'Telma MVola', 'MGA', '#111#', '#111*2*{phone}*{amount}#', '#111*4*{merchant}*{amount}#'),
('KM', 'Comoros', 'MVola', 'KMF', '*150*01#', '*150*01*1*1*{phone}*{amount}#', '*150*01*1*2*{merchant}*{amount}#'),
('SC', 'Seychelles', 'Airtel', 'SCR', '*202#', '*202*{phone}*{amount}#', '*202*{merchant}*{amount}#'),
('DJ', 'Djibouti', 'D-Money', 'DJF', '*131#', '*131*{phone}*{amount}#', '*133*{merchant}*{amount}#')

ON CONFLICT (country_code, provider) DO UPDATE SET
    country_name = EXCLUDED.country_name,
    currency = EXCLUDED.currency,
    base_code = EXCLUDED.base_code,
    send_to_phone = EXCLUDED.send_to_phone,
    pay_merchant = EXCLUDED.pay_merchant,
    updated_at = NOW();

-- Index for fast lookups
CREATE INDEX IF NOT EXISTS idx_ussd_configs_country ON ussd_configs(country_code);

-- Allow public read access (no auth required for USSD configs)
ALTER TABLE ussd_configs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read USSD configs" ON ussd_configs FOR SELECT USING (true);
