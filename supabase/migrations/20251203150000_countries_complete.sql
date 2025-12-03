-- ============================================================================
-- Complete Countries Table with Mobile Money Provider Configuration
-- ============================================================================

DROP TABLE IF EXISTS countries CASCADE;

CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(2) NOT NULL UNIQUE,
    code_alpha3 VARCHAR(3),
    name VARCHAR(100) NOT NULL,
    name_local VARCHAR(100),
    name_french VARCHAR(100),
    currency VARCHAR(3) NOT NULL,
    currency_symbol VARCHAR(10) NOT NULL,
    currency_decimals INTEGER DEFAULT 2,
    phone_prefix VARCHAR(10) NOT NULL,
    phone_length INTEGER DEFAULT 9,
    flag_emoji VARCHAR(10) DEFAULT '',
    primary_language VARCHAR(10) DEFAULT 'en',
    provider_name VARCHAR(100) NOT NULL,
    provider_code VARCHAR(50) NOT NULL,
    provider_color VARCHAR(10) DEFAULT '#FFCC00',
    ussd_base_code VARCHAR(30),
    ussd_send_to_phone VARCHAR(100),
    ussd_pay_merchant VARCHAR(100),
    ussd_notes TEXT,
    has_ussd_support BOOLEAN DEFAULT true,
    has_app_support BOOLEAN DEFAULT false,
    has_qr_support BOOLEAN DEFAULT false,
    requires_pin_prompt BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    is_primary_market BOOLEAN DEFAULT false,
    launch_priority INTEGER DEFAULT 99,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_countries_code ON countries(code);
CREATE INDEX idx_countries_active ON countries(is_active);
CREATE INDEX idx_countries_priority ON countries(launch_priority);

INSERT INTO countries (
    code, name, name_local, name_french,
    currency, currency_symbol, phone_prefix, phone_length,
    flag_emoji, primary_language,
    provider_name, provider_code, provider_color,
    ussd_base_code, ussd_send_to_phone, ussd_pay_merchant,
    has_ussd_support, has_app_support, ussd_notes,
    is_active, is_primary_market, launch_priority
) VALUES
-- PRIMARY LAUNCH (1-5)
('RW', 'Rwanda', 'Rwanda', 'Rwanda', 'RWF', 'FRw', '+250', 9, '🇷🇼', 'rw',
 'MTN MoMo', 'MTN', '#FFCC00', '*182#', '*182*1*1*{phone}*{amount}#', '*182*8*1*{merchant}*{amount}#',
 true, false, 'Option 1-1 for send, Option 8-1 for merchant pay', true, true, 1),

('BI', 'Burundi', 'Burundi', 'Burundi', 'BIF', 'FBu', '+257', 8, '🇧🇮', 'fr',
 'Econet EcoCash', 'ECOCASH', '#00A651', '*151#', '*151*1*1*{phone}*{amount}#', '*151*1*2*{phone}*{amount}#',
 true, false, 'EcoCash distinguishes registered vs. unregistered users', true, true, 2),

('CM', 'Cameroon', 'Cameroun', 'Cameroun', 'XAF', 'FCFA', '+237', 9, '🇨🇲', 'fr',
 'MTN Mobile Money', 'MTN', '#FFCC00', '*126#', '*126*2*{phone}*{amount}#', '*126*4*{merchant}*{amount}#',
 true, false, 'Option 2 for transfer, Option 4 for bills/merchants', true, true, 3),

('MG', 'Madagascar', 'Madagasikara', 'Madagascar', 'MGA', 'Ar', '+261', 9, '🇲🇬', 'fr',
 'Telma MVola', 'MVOLA', '#E31E24', '#111#', '#111*2*{phone}*{amount}#', '#111*4*{merchant}*{amount}#',
 true, true, 'MVola uses # prefix. Option 2 send, Option 4 pay', true, true, 4),

('MU', 'Mauritius', 'Mauritius', 'Maurice', 'MUR', '₨', '+230', 8, '🇲🇺', 'en',
 'my. t money', 'MYT', '#E4002B', NULL, NULL, NULL,
 false, true, 'App-based only. No USSD. QR code payments supported.', true, true, 5),

-- EAST & SOUTHERN AFRICA (6-13)
('TZ', 'Tanzania', 'Tanzania', 'Tanzanie', 'TZS', 'TSh', '+255', 9, '🇹🇿', 'sw',
 'Vodacom M-Pesa', 'VODACOM', '#E60000', '*150*00#', '*150*00*{phone}*{amount}#', '*150*00*{merchant}*{amount}#',
 true, true, 'M-Pesa prompts for PIN after dialing', true, true, 6),

('ZM', 'Zambia', 'Zambia', 'Zambie', 'ZMW', 'ZK', '+260', 9, '🇿🇲', 'en',
 'MTN MoMo', 'MTN', '#FFCC00', '*115#', '*115*2*{phone}*{amount}#', '*115*5*{merchant}*{amount}#',
 true, false, 'Updated USSD to *115# for all MoMo services', true, true, 7),

('CD', 'DR Congo', 'RD Congo', 'République Démocratique du Congo', 'CDF', 'FC', '+243', 9, '🇨🇩', 'fr',
 'Orange Money', 'ORANGE', '#FF6600', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#',
 true, true, 'Option 4 for bill payments', true, true, 8),

('ZW', 'Zimbabwe', 'Zimbabwe', 'Zimbabwe', 'ZWL', 'Z$', '+263', 9, '🇿🇼', 'en',
 'Econet EcoCash', 'ECOCASH', '#00A651', '*151#', '*151*1*1*{phone}*{amount}#', '*151*2*{merchant}*{amount}#',
 true, false, 'PIN-protected after USSD dial', true, false, 10),

('MW', 'Malawi', 'Malawi', 'Malawi', 'MWK', 'MK', '+265', 9, '🇲🇼', 'en',
 'Airtel Money', 'AIRTEL', '#ED1C24', '*211#', '*211*{phone}*{amount}#', '*211*{merchant}*{amount}#',
 true, false, '*211# covers transfers, withdrawals, and bill payments', true, false, 11),

('NA', 'Namibia', 'Namibia', 'Namibie', 'NAD', 'N$', '+264', 9, '🇳🇦', 'en',
 'MTC Money (Maris)', 'MTC', '#0066B3', '*140*682#', '*140*682*{phone}*{amount}#', '*140*682*{merchant}*{amount}#',
 true, false, 'MTC Maris USSD: *140*682# is gateway for all transactions', true, false, 12),

('SC', 'Seychelles', 'Seychelles', 'Seychelles', 'SCR', '₨', '+248', 7, '🇸🇨', 'en',
 'Airtel Money', 'AIRTEL', '#ED1C24', '*202#', '*202*{phone}*{amount}#', '*202*{merchant}*{amount}#',
 true, false, 'Prompts for PIN after dialing', true, false, 13),

-- WEST AFRICA (14-30)
('GH', 'Ghana', 'Ghana', 'Ghana', 'GHS', 'GH₵', '+233', 9, '🇬🇭', 'en',
 'MTN Mobile Money', 'MTN', '#FFCC00', '*170#', '*170*1*1*{phone}*{amount}#', '*170*2*1*{merchant}*{amount}#',
 true, true, 'Option 1-1 for MoMo user, Option 2-1 for MoMoPay merchant', true, false, 14),

('BJ', 'Benin', 'Bénin', 'Bénin', 'XOF', 'CFA', '+229', 8, '🇧🇯', 'fr',
 'MTN Mobile Money', 'MTN', '#FFCC00', '*880#', '*880*1*{phone}*{amount}#', '*880*3*{merchant}*{amount}#',
 true, false, 'Option 1 send, Option 3 payments', true, false, 15),

('BF', 'Burkina Faso', 'Burkina Faso', 'Burkina Faso', 'XOF', 'CFA', '+226', 8, '🇧🇫', 'fr',
 'Orange Money', 'ORANGE', '#FF6600', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#',
 true, false, 'Option 4 for bills', true, false, 16),

('CF', 'Central African Republic', 'Ködörösêse tî Bêafrîka', 'République Centrafricaine', 'XAF', 'FCFA', '+236', 8, '🇨🇫', 'fr',
 'Orange Money', 'ORANGE', '#FF6600', '#150#', '#150*2*{phone}*{amount}#', '#150*4*{merchant}*{amount}#',
 true, false, 'Uses # prefix. Option 2 send, Option 4 pay', true, false, 17),

('TD', 'Chad', 'Tchad', 'Tchad', 'XAF', 'FCFA', '+235', 8, '🇹🇩', 'fr',
 'Airtel Money', 'AIRTEL', '#ED1C24', '*211#', '*211*{phone}*{amount}#', '*211*{merchant}*{amount}#',
 true, false, '*211AgentAmount# for withdrawal, *211BillAmount# for bills', true, false, 18),

('KM', 'Comoros', 'Komori', 'Comores', 'KMF', 'CF', '+269', 7, '🇰🇲', 'fr',
 'Telma/YAZ MVola', 'MVOLA', '#E31E24', '*150*01#', '*150*01*1*1*{phone}*{amount}#', '*150*01*1*2*{merchant}*{amount}#',
 true, false, 'Uses same *150*01# USSD as Madagascar MVola', true, false, 19),

('CG', 'Congo (Republic)', 'Congo-Brazzaville', 'République du Congo', 'XAF', 'FCFA', '+242', 9, '🇨🇬', 'fr',
 'MTN MoMo', 'MTN', '#FFCC00', '*133#', '*133*2*{phone}*{amount}#', '*133*5*{merchant}*{amount}#',
 true, false, 'Option 2 transfer, Option 5 bills', true, false, 20),

('CI', 'Côte d''Ivoire', 'Côte d''Ivoire', 'Côte d''Ivoire', 'XOF', 'CFA', '+225', 10, '🇨🇮', 'fr',
 'Orange Money', 'ORANGE', '#FF6600', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#',
 true, true, 'Option 4 for Pay Bills', true, false, 21),

('DJ', 'Djibouti', 'Jabuuti', 'Djibouti', 'DJF', 'Fdj', '+253', 8, '🇩🇯', 'fr',
 'Djibouti Telecom D-Money', 'DMONEY', '#00A651', '*131#', '*131*{phone}*{amount}#', '*133*{merchant}*{amount}#',
 true, false, '*130# balance, *131# send, *133# bills', true, false, 22),

('GQ', 'Equatorial Guinea', 'Guinea Ecuatorial', 'Guinée Équatoriale', 'XAF', 'FCFA', '+240', 9, '🇬🇶', 'es',
 'GETESA Mobile Money', 'GETESA', '#009639', '*222#', '*222*2*{phone}*{amount}#', '*222*4*{merchant}*{amount}#',
 true, false, 'Option 2 for P2P, Option 4 for payments', true, false, 23),

('GA', 'Gabon', 'Gabon', 'Gabon', 'XAF', 'FCFA', '+241', 9, '🇬🇦', 'fr',
 'Airtel Money', 'AIRTEL', '#ED1C24', '*150#', '*150*2*{phone}*{amount}#', '*150*4*{merchant}*{amount}#',
 true, false, 'Option 2 send, Option 4 pay', true, false, 24),

('GN', 'Guinea', 'Guinée', 'Guinée', 'GNF', 'FG', '+224', 9, '🇬🇳', 'fr',
 'Orange Money', 'ORANGE', '#FF6600', '*144#', '*144*1*{phone}*{amount}#', '*144*4*{merchant}*{amount}#',
 true, false, 'Similar to other Orange Money countries', true, false, 25),

('ML', 'Mali', 'Mali', 'Mali', 'XOF', 'CFA', '+223', 8, '🇲🇱', 'fr',
 'Orange Money', 'ORANGE', '#FF6600', '#144#', '#144#*1*{phone}*{amount}#', '#144#*2*{merchant}*{amount}#',
 true, false, 'Uses #144# code (with # prefix)', true, false, 26),

('MR', 'Mauritania', 'Mauritanie', 'Mauritanie', 'MRU', 'UM', '+222', 8, '🇲🇷', 'ar',
 'Moov Mauritel Money', 'MOOV', '#6F2C91', '*900#', '*900*2*{phone}*{amount}#', '*900*4*{merchant}*{amount}#',
 true, false, 'Option 2 send, Option 4 bills', true, false, 27),

('NE', 'Niger', 'Niger', 'Niger', 'XOF', 'CFA', '+227', 8, '🇳🇪', 'fr',
 'Airtel Money', 'AIRTEL', '#ED1C24', '*400#', '*400*{phone}*{amount}#', '*400*{merchant}*{amount}#',
 true, false, 'Direct format without menu options', true, false, 28),

('SN', 'Senegal', 'Sénégal', 'Sénégal', 'XOF', 'CFA', '+221', 9, '🇸🇳', 'fr',
 'Orange Money', 'ORANGE', '#FF6600', '#144#', '#144*1*{phone}*{amount}#', '#144*2*{merchant}*{amount}#',
 true, true, 'Uses #144# code. Option 1 send, Option 2 pay', true, false, 29),

('TG', 'Togo', 'Togo', 'Togo', 'XOF', 'CFA', '+228', 8, '🇹🇬', 'fr',
 'Togocom T-Money', 'TMONEY', '#00A651', '*145#', '*145*1*{amount}*{phone}#', '*145*3*{merchant}*{amount}#',
 true, false, 'NOTE: Amount entered BEFORE phone number for send', true, false, 30);

-- Trigger for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_countries_updated_at
    BEFORE UPDATE ON countries
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Row Level Security
ALTER TABLE countries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Countries are viewable by authenticated users"
    ON countries FOR SELECT
    TO authenticated
    USING (is_active = true);

CREATE POLICY "Service role has full access to countries"
    ON countries FOR ALL
    TO service_role
    USING (true);
