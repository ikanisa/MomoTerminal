# Production Certificate Pins

**Generated:** December 1, 2025  
**Domain:** lhbowpbcpwoiparwnwgt.supabase.co

## Primary Certificate Pin

```
sha256/PzfKSv758ttsdJwUCkGhW/oxG9Wk1Y4N+NMkB5I7RXc=
```

## How to Use

### Step 1: Update network_security_config.xml

Replace the commented section in `app/src/main/res/xml/network_security_config.xml`:

```xml
<domain-config cleartextTrafficPermitted="false">
    <domain includeSubdomains="true">lhbowpbcpwoiparwnwgt.supabase.co</domain>
    <pin-set expiration="2026-12-01">
        <pin digest="SHA-256">PzfKSv758ttsdJwUCkGhW/oxG9Wk1Y4N+NMkB5I7RXc=</pin>
        <!-- Add backup pin from CA or alternative certificate -->
    </pin-set>
</domain-config>
```

### Step 2: Update build.gradle.kts (Optional)

If using programmatic pinning via OkHttp, update the pins in `app/build.gradle.kts`:

```kotlin
val certPinPrimary = "sha256/PzfKSv758ttsdJwUCkGhW/oxG9Wk1Y4N+NMkB5I7RXc="
```

## Important Notes

⚠️ **Certificate Rotation Planning:**
- Supabase certificates may rotate during renewal
- Always maintain at least 2 pins (primary + backup)
- Monitor certificate expiration dates
- Test pin updates in staging before production

⚠️ **Backup Pin Recommendation:**
- Get the intermediate CA pin or root CA pin as backup
- This ensures connectivity during certificate rotation
- Contact Supabase support for their certificate chain details

## Pin Expiration Monitoring

Set up monitoring for:
1. Certificate expiration (typically 90 days for Let's Encrypt)
2. Pin expiration date in network_security_config.xml
3. Alert 30 days before expiration

## Emergency Pin Update Procedure

If pins become invalid:

1. **Immediate**: Use remote config to disable pinning temporarily
2. **Quick Fix**: Release hotfix with updated pins
3. **Communication**: Notify users if app update required

## Verification

Test the pins work correctly:

```bash
# Verify connection with pinning
curl --pinnedpubkey "sha256//PzfKSv758ttsdJwUCkGhW/oxG9Wk1Y4N+NMkB5I7RXc=" \
  https://lhbowpbcpwoiparwnwgt.supabase.co
```

## Re-generating Pins

If you need to regenerate pins later:

```bash
openssl s_client -connect lhbowpbcpwoiparwnwgt.supabase.co:443 \
  -servername lhbowpbcpwoiparwnwgt.supabase.co </dev/null 2>/dev/null \
  | openssl x509 -pubkey -noout \
  | openssl pkey -pubin -outform der \
  | openssl dgst -sha256 -binary \
  | base64
```

## Additional Resources

- [Android Network Security Config](https://developer.android.com/training/articles/security-config)
- [OkHttp Certificate Pinning](https://square.github.io/okhttp/features/https/)
- [Supabase SSL/TLS Documentation](https://supabase.com/docs)
