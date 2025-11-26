package com.momoterminal.security

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
 */
@Singleton
class CertificatePinnerConfig @Inject constructor() {

    companion object {
        // API domains to pin
        private const val API_DOMAIN = "api.momoterminal.com"
        private const val BACKUP_API_DOMAIN = "*.momoterminal.com"
        
        // Production certificate pins (SHA-256)
        // IMPORTANT: Replace these placeholder pins with actual production certificate pins
        // before deploying to production. Without real pins, certificate pinning will fail.
        // 
        // To generate pins, use:
        // openssl s_client -connect api.momoterminal.com:443 | \
        //   openssl x509 -pubkey -noout | \
        //   openssl pkey -pubin -outform der | \
        //   openssl dgst -sha256 -binary | \
        //   openssl enc -base64
        // 
        // TODO: Replace with actual certificate pins before production deployment
        private const val PRIMARY_PIN = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        private const val BACKUP_PIN = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        
        // Additional backup pins (root CA pins for certificate rotation)
        // TODO: Replace with actual root CA pins
        private const val ROOT_CA_PIN = "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC="
    }

    /**
     * Creates a CertificatePinner for the configured domains.
     * 
     * In debug builds, certificate pinning is disabled to allow development
     * with local servers and debugging proxies.
     * 
     * In release builds, strict certificate pinning is enforced with multiple
     * pins to support certificate rotation.
     */
    fun createCertificatePinner(): CertificatePinner {
        return if (BuildConfig.DEBUG) {
            // Disable pinning in debug for development flexibility
            CertificatePinner.Builder().build()
        } else {
            buildProductionPinner()
        }
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
        return domain.endsWith("momoterminal.com")
    }

    /**
     * Returns the list of pinned domains.
     */
    fun getPinnedDomains(): List<String> {
        return listOf(API_DOMAIN, BACKUP_API_DOMAIN)
    }
}
