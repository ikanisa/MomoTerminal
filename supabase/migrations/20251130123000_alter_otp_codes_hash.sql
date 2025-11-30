-- Migration: 20251130123000_alter_otp_codes_hash.sql
-- Description: Change otp_codes.code type to TEXT to store SHA-256 hashes
-- Created: 2025-11-30

-- Alter the code column to be TEXT (or VARCHAR(64)) to accommodate the hash
ALTER TABLE otp_codes ALTER COLUMN code TYPE TEXT;

-- Update the comment to reflect the change
COMMENT ON COLUMN otp_codes.code IS 'SHA-256 hash of the 6-digit OTP code';
