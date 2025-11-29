# Security Documentation for MomoTerminal

This document provides security guidelines and setup instructions for MomoTerminal, a financial mobile application for Mobile Money transactions.

## Table of Contents

1. [Certificate Pinning Setup](#certificate-pinning-setup)
2. [Security Features](#security-features)
3. [Configuration](#configuration)
4. [Security Best Practices](#security-best-practices)
5. [Vulnerability Reporting](#vulnerability-reporting)

---

## Certificate Pinning Setup

Certificate pinning is a critical security feature that prevents man-in-the-middle attacks by verifying that the server's certificate matches an expected certificate.

### Generating Certificate Pins

1. **Generate pins for your production domain:**

```bash
# For the primary certificate
openssl s_client -connect api.momoterminal.com:443 < /dev/null 2>/dev/null | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64

# For intermediate certificates
openssl s_client -showcerts -connect api.momoterminal.com:443 < /dev/null 2>/dev/null | \
  openssl x509 -outform PEM | \
  openssl x509 -pubkey -noout | \
  openssl pkey -pubin -outform der | \
  openssl dgst -sha256 -binary | \
  openssl enc -base64
```

2. **Add pins to `local.properties`:**

```properties
# Certificate Pinning Configuration
# IMPORTANT: Replace these placeholder values with real pins before production deployment!
CERT_PIN_PRIMARY=sha256/your-primary-pin-here
CERT_PIN_BACKUP=sha256/your-backup-pin-here
CERT_PIN_ROOT_CA=sha256/your-root-ca-pin-here

# Only set to true for development/testing - NEVER in production!
ALLOW_PLACEHOLDER_PINS=false
```

3. **Update pins before certificate expiration:**

   - Certificate pins should be updated at least 30 days before the certificate expires
   - Always include a backup pin for the next certificate in the chain
   - Test pin updates in staging before deploying to production

### Pin Rotation Strategy

1. Include at least 2 pins: current certificate and backup
2. Add new pin at least 30 days before rotation
3. Test with both old and new pins
4. Remove old pin 30 days after successful rotation

---

## Security Features

### 1. Device Security Checks

MomoTerminal performs comprehensive device security checks:

- **Root Detection**: Detects rooted devices that may compromise security
- **Emulator Detection**: Prevents running on emulators in production
- **Debugger Detection**: Detects debugging tools like Frida
- **Instrumentation Detection**: Detects Xposed and similar frameworks

### 2. Screen Security

- Screenshots and screen recording are disabled for sensitive screens
- Content is hidden in the recent apps view
- Implemented using `WindowManager.LayoutParams.FLAG_SECURE`

### 3. Secure Storage

- **EncryptedSharedPreferences**: For API tokens, user credentials
- **SQLCipher**: For encrypted local database (recommended for production)
- **Android Keystore**: For cryptographic key management

### 4. Authentication

- **PIN-based authentication**: 6-digit PIN for login
- **Biometric authentication**: Fingerprint/Face unlock support
- **Session management**: Auto-logout on inactivity
- **Secure token storage**: JWT tokens stored encrypted

### 5. Network Security

- **HTTPS only**: All network traffic encrypted
- **Certificate pinning**: Protection against MITM attacks
- **TLS 1.2+**: Minimum TLS version enforced
- **No cleartext traffic**: Blocked in production builds

---

## Configuration

### Build Configuration

For production builds, ensure the following in `gradle.properties` or environment variables:

```properties
# Release signing (set via environment in CI/CD)
MOMO_KEYSTORE_FILE=/path/to/release.keystore
MOMO_KEYSTORE_PASSWORD=your_keystore_password
MOMO_KEY_ALIAS=your_key_alias
MOMO_KEY_PASSWORD=your_key_password

# Certificate pins (required for production)
CERT_PIN_PRIMARY=sha256/actual-pin-value
CERT_PIN_BACKUP=sha256/backup-pin-value
CERT_PIN_ROOT_CA=sha256/root-ca-pin-value
```

### Network Security Config

The network security configuration is in `app/src/main/res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">api.momoterminal.com</domain>
        <pin-set expiration="2025-12-31">
            <pin digest="SHA-256">YOUR_PRIMARY_PIN</pin>
            <pin digest="SHA-256">YOUR_BACKUP_PIN</pin>
        </pin-set>
    </domain-config>
</network-security-config>
```

---

## Security Best Practices

### For Developers

1. **Never commit secrets** to version control
2. **Use environment variables** for sensitive configuration
3. **Keep dependencies updated** to patch vulnerabilities
4. **Review ProGuard rules** to ensure proper obfuscation
5. **Test security features** on actual devices, not just emulators

### For Production Deployment

1. **Replace all placeholder pins** with real certificate pins
2. **Set `ALLOW_PLACEHOLDER_PINS=false`**
3. **Verify ProGuard/R8 is enabled** for release builds
4. **Test certificate pinning** before deployment
5. **Monitor for certificate expiration** and plan rotations

### For Users/Merchants

1. **Keep the app updated** to receive security patches
2. **Use device lock** (PIN, pattern, or biometrics)
3. **Don't root your device** if using for financial transactions
4. **Report suspicious activity** to support immediately

---

## Vulnerability Reporting

If you discover a security vulnerability in MomoTerminal, please report it responsibly:

### How to Report

1. **Email**: security@momoterminal.com
2. **PGP Key**: Available at https://momoterminal.com/.well-known/security.txt
3. **Include**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### What to Expect

1. **Acknowledgment**: Within 48 hours
2. **Assessment**: Within 7 days
3. **Fix timeline**: Based on severity
4. **Credit**: In our security advisory (if desired)

### Bug Bounty

We offer rewards for valid security vulnerabilities:

- **Critical**: Up to $5,000
- **High**: Up to $2,000
- **Medium**: Up to $500
- **Low**: Up to $100

See our bug bounty policy at https://momoterminal.com/security/bug-bounty

---

## Security Audit History

| Date | Auditor | Scope | Report |
|------|---------|-------|--------|
| Q1 2024 | Internal | Full Application | Available on request |

---

## Contact

For security-related inquiries:

- **Email**: security@momoterminal.com
- **Website**: https://momoterminal.com/security
- **Response time**: Within 48 hours for security matters

---

*Last updated: January 2024*
