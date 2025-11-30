package com.momoterminal.security

import android.util.Log
import com.momoterminal.BuildConfig
import okhttp3.CertificatePinner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Configuration for SSL certificate pinning to prevent MITM attacks.
 * 
 * Certificate pinning validates that the server's SSL certificate matches
 * known pins, providing an additional layer of security beyond standard
 * certificate chain validation.
 * 
 * ## Configuration
 * 
 * Certificate pins should be configured at build time via `gradle.properties` or
 * `local.properties`:
 * 
 * ```properties
 * # In gradle.properties or local.properties (not committed to source control)
 * CERT_PIN_PRIMARY=sha256/your-primary-pin-base64-here=
 * CERT_PIN_BACKUP=sha256/your-backup-pin-base64-here=
 * CERT_PIN_ROOT_CA=sha256/your-root-ca-pin-base64-here=
 * ```
 * 
 * ## Generating Certificate Pins
 * 
 * Use the following command to generate a pin from your server's certificate:
 * 
 * ```bash
 * openssl s_client -connect lhbowpbcpwoiparwnwgt.supabase.co:443 -servername lhbowpbcpwoiparwnwgt.supabase.co 2>/dev/null | \
 *   openssl x509 -pubkey -noout | \
 *   openssl pkey -pubin -outform der | \
 *   openssl dgst -sha256 -binary | \
 *   openssl enc -base64
 * ```
 * 
 * You can also use the following to get the full certificate chain and generate pins
 * for intermediate and root certificates:
 * 
 * ```bash
 * openssl s_client -connect lhbowpbcpwoiparwnwgt.supabase.co:443 -showcerts 2>/dev/null | \
 *   openssl x509 -pubkey -noout | \
 *   openssl pkey -pubin -outform der | \
 *   openssl dgst -sha256 -binary | \
 *   openssl enc -base64
 * ```
 * 
 * @see <a href="https://owasp.org/www-community/controls/Certificate_and_Public_Key_Pinning">OWASP Certificate Pinning</a>
 */
@Singleton
class CertificatePinnerConfig @Inject constructor() {

    companion object {
        private const val TAG = "CertificatePinnerConfig"
        
        // API domains to pin (using Supabase)
        private const val API_DOMAIN = "lhbowpbcpwoiparwnwgt.supabase.co"
        private const val BACKUP_API_DOMAIN = "*.supabase.co"
        
        // Placeholder pin pattern for detection
        private const val PLACEHOLDER_PATTERN = "sha256/[A]{43}="
        
        /**
         * Certificate pins configured via BuildConfig fields.
         * 
         * These are set at build time from gradle.properties or local.properties.
         * If not configured, placeholder values are used which will be detected
         * and warned about in release builds.
         * 
         * Default placeholder pins (MUST be replaced before production):
         * - PRIMARY_PIN: sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
         * - BACKUP_PIN: sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=
         * - ROOT_CA_PIN: sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=
         */
        private val PRIMARY_PIN: String
            get() = BuildConfig.CERT_PIN_PRIMARY
        
        private val BACKUP_PIN: String
            get() = BuildConfig.CERT_PIN_BACKUP
        
        private val ROOT_CA_PIN: String
            get() = BuildConfig.CERT_PIN_ROOT_CA
        
        // Default placeholder pins - these MUST be replaced before production
        private const val DEFAULT_PRIMARY_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        private const val DEFAULT_BACKUP_PIN = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        private const val DEFAULT_ROOT_CA_PIN = "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
    }

    /**
     * Creates a CertificatePinner for the configured domains.
     * 
     * In debug builds, certificate pinning is disabled to allow development
     * with local servers and debugging proxies.
     * 
     * In release builds, strict certificate pinning is enforced with multiple
     * pins to support certificate rotation. If placeholder pins are detected
     * in release builds, a warning is logged and pinning is still enabled
     * (which will cause connection failures - this is intentional to prevent
     * insecure production deployments).
     */
    fun createCertificatePinner(): CertificatePinner {
        return if (BuildConfig.DEBUG) {
            // Disable pinning in debug for development flexibility
            CertificatePinner.Builder().build()
        } else {
            validatePinsForProduction()
            buildProductionPinner()
        }
    }
    
    /**
     * Validates that certificate pins have been properly configured for production.
     * 
     * @throws SecurityException in release builds if placeholder pins are detected
     *         and ALLOW_PLACEHOLDER_PINS is not set to true
     */
    private fun validatePinsForProduction() {
        val usingPlaceholderPins = isPlaceholderPin(PRIMARY_PIN) || 
                                    isPlaceholderPin(BACKUP_PIN) || 
                                    isPlaceholderPin(ROOT_CA_PIN)
        
        if (usingPlaceholderPins) {
            val message = """
                |⚠️ SECURITY WARNING: Placeholder certificate pins detected in RELEASE build!
                |
                |Certificate pinning is configured with placeholder values. This will cause
                |all HTTPS connections to fail, which is the expected security behavior.
                |
                |To fix this issue, configure real certificate pins in gradle.properties:
                |
                |  CERT_PIN_PRIMARY=sha256/<your-primary-pin>=
                |  CERT_PIN_BACKUP=sha256/<your-backup-pin>=
                |  CERT_PIN_ROOT_CA=sha256/<your-root-ca-pin>=
                |
                |Generate pins using:
                |  openssl s_client -connect lhbowpbcpwoiparwnwgt.supabase.co:443 | \
                |    openssl x509 -pubkey -noout | \
                |    openssl pkey -pubin -outform der | \
                |    openssl dgst -sha256 -binary | \
                |    openssl enc -base64
            """.trimMargin()
            
            Log.e(TAG, message)
            
            // Check if placeholder pins are explicitly allowed (for testing only)
            val allowPlaceholders = BuildConfig.ALLOW_PLACEHOLDER_PINS
            
            if (!allowPlaceholders) {
                throw SecurityException(
                    "Production build detected with placeholder certificate pins. " +
                    "Configure CERT_PIN_PRIMARY, CERT_PIN_BACKUP, and CERT_PIN_ROOT_CA " +
                    "in gradle.properties before releasing to production."
                )
            }
        }
    }
    
    /**
     * Checks if the given pin is a placeholder value.
     */
    private fun isPlaceholderPin(pin: String): Boolean {
        // Check for the common placeholder patterns
        // Placeholder pins are defined with single repeated uppercase letters like AAA...=, BBB...=, CCC...=
        return pin == DEFAULT_PRIMARY_PIN ||
               pin == DEFAULT_BACKUP_PIN ||
               pin == DEFAULT_ROOT_CA_PIN ||
               // Match any single uppercase letter repeated 43 times (placeholder pattern)
               pin.matches(Regex("sha256/([A-Z])\\1{42}="))
    }
    
    /**
     * Checks if the current configuration is using placeholder pins.
     * Useful for build-time checks and CI/CD pipelines.
     */
    fun isUsingPlaceholderPins(): Boolean {
        return isPlaceholderPin(PRIMARY_PIN) || 
               isPlaceholderPin(BACKUP_PIN) || 
               isPlaceholderPin(ROOT_CA_PIN)
    }

    /**
     * Builds a CertificatePinner for production use with multiple pins.
     * 
     * Multiple pins are configured to:
     * 1. Allow certificate rotation without app updates
     * 2. Provide fallback if a certificate is compromised
     * 3. Pin both leaf and intermediate/root certificates
     */
    private fun buildProductionPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            // Primary API domain pins
            .add(API_DOMAIN, PRIMARY_PIN)
            .add(API_DOMAIN, BACKUP_PIN)
            .add(API_DOMAIN, ROOT_CA_PIN)
            // Wildcard domain for subdomains
            .add(BACKUP_API_DOMAIN, PRIMARY_PIN)
            .add(BACKUP_API_DOMAIN, BACKUP_PIN)
            .add(BACKUP_API_DOMAIN, ROOT_CA_PIN)
            .build()
    }

    /**
     * Validates if a domain is configured for certificate pinning.
     */
    fun isDomainPinned(domain: String): Boolean {
        return domain.endsWith("supabase.co")
    }

    /**
     * Returns the list of pinned domains.
     */
    fun getPinnedDomains(): List<String> {
        return listOf(API_DOMAIN, BACKUP_API_DOMAIN)
    }
    
    /**
     * Returns information about the current pin configuration for debugging.
     * Only returns masked values for security.
     */
    fun getPinConfigInfo(): Map<String, String> {
        return mapOf(
            "primary_pin_configured" to (!isPlaceholderPin(PRIMARY_PIN)).toString(),
            "backup_pin_configured" to (!isPlaceholderPin(BACKUP_PIN)).toString(),
            "root_ca_pin_configured" to (!isPlaceholderPin(ROOT_CA_PIN)).toString(),
            "api_domain" to API_DOMAIN,
            "backup_domain" to BACKUP_API_DOMAIN
        )
    }
}
