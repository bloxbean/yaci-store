package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simplified validator to ensure token metadata fields meet CIP-26 specification constraints.
 * This is a simplified version that checks that required fields (name and description) are present,
 * without depending on the cf-metadata-core library.
 */
@Component
@Slf4j
public class TokenMetadataValidator {

    /** CIP-26 maximum logo size in characters (base64-encoded PNG, max 87,400 chars). */
    private static final int MAX_LOGO_LENGTH = 87_400;

    /** CIP-26 maximum name length. */
    private static final int MAX_NAME_LENGTH = 50;

    /** CIP-26 maximum description length. */
    private static final int MAX_DESCRIPTION_LENGTH = 500;

    /** CIP-26 maximum ticker length. */
    private static final int MAX_TICKER_LENGTH = 9;

    /** CIP-26 maximum URL length. */
    private static final int MAX_URL_LENGTH = 250;

    /**
     * Validates that all fields in the TokenMetadata entity comply with CIP-26 specification.
     * Checks that required fields (name and description) are present and that optional fields
     * do not exceed their maximum lengths.
     *
     * @param tokenMetadata the token metadata to validate
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validate(TokenMetadata tokenMetadata) {
        try {
            if (tokenMetadata.getSubject() == null || tokenMetadata.getSubject().isBlank()) {
                log.warn("CIP-26 validation failed: subject is missing");
                return false;
            }

            if (tokenMetadata.getName() == null || tokenMetadata.getName().isBlank()) {
                log.warn("CIP-26 validation failed for subject '{}': name is missing",
                        tokenMetadata.getSubject());
                return false;
            }

            if (tokenMetadata.getDescription() == null || tokenMetadata.getDescription().isBlank()) {
                log.warn("CIP-26 validation failed for subject '{}': description is missing",
                        tokenMetadata.getSubject());
                return false;
            }

            if (tokenMetadata.getName().length() > MAX_NAME_LENGTH) {
                log.warn("CIP-26 validation failed for subject '{}': name exceeds {} characters",
                        tokenMetadata.getSubject(), MAX_NAME_LENGTH);
                return false;
            }

            if (tokenMetadata.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                log.warn("CIP-26 validation failed for subject '{}': description exceeds {} characters",
                        tokenMetadata.getSubject(), MAX_DESCRIPTION_LENGTH);
                return false;
            }

            if (tokenMetadata.getTicker() != null && tokenMetadata.getTicker().length() > MAX_TICKER_LENGTH) {
                log.warn("CIP-26 validation failed for subject '{}': ticker exceeds {} characters",
                        tokenMetadata.getSubject(), MAX_TICKER_LENGTH);
                return false;
            }

            if (tokenMetadata.getUrl() != null && tokenMetadata.getUrl().length() > MAX_URL_LENGTH) {
                log.warn("CIP-26 validation failed for subject '{}': url exceeds {} characters",
                        tokenMetadata.getSubject(), MAX_URL_LENGTH);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error during CIP-26 validation for subject '{}': {}",
                    tokenMetadata.getSubject(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates a logo string according to CIP-26 specification.
     * Logo is optional but if present must comply with CIP-26 constraints (max 87,400 characters).
     *
     * @param subject the token subject (for logging)
     * @param logo the logo string to validate (can be null)
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validateLogo(String subject, String logo) {
        // Logo is optional, so null is valid
        if (logo == null || logo.isEmpty()) {
            return true;
        }

        try {
            if (logo.length() > MAX_LOGO_LENGTH) {
                log.warn("CIP-26 logo validation failed for subject '{}': logo exceeds {} characters",
                        subject, MAX_LOGO_LENGTH);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error during CIP-26 logo validation for subject '{}': {}",
                    subject, e.getMessage(), e);
            return false;
        }
    }
}
