package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.metadatatools.core.cip26.MetadataCreator;
import org.cardanofoundation.metadatatools.core.cip26.ValidationField;
import org.cardanofoundation.metadatatools.core.cip26.ValidationResult;
import org.cardanofoundation.metadatatools.core.cip26.model.Metadata;
import org.cardanofoundation.metadatatools.core.cip26.model.MetadataProperty;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Validates token metadata fields against the CIP-26 specification using the
 * <a href="https://github.com/cardano-foundation/cf-token-metadata-registry">cf-tokens-cip26</a> library.
 */
@Component
@Slf4j
public class Cip26MetadataValidator {

    /**
     * Validates that all fields in the Cip26Metadata entity comply with CIP-26 specification.
     *
     * @param tokenMetadata the token metadata to validate
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validate(Cip26Metadata tokenMetadata) {
        try {
            Metadata cip26Metadata = convertToMetadata(tokenMetadata);
            ValidationResult validationResult = MetadataCreator.validateMetadata(cip26Metadata);

            if (!validationResult.isValid()) {
                String errorMessages = validationResult.getValidationErrors().stream()
                        .map(error -> error.getField() + ": " + error.getMessage())
                        .collect(Collectors.joining(", "));
                log.warn("CIP-26 validation failed for subject '{}': {}",
                        tokenMetadata.getSubject(), errorMessages);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error during CIP-26 validation for subject '{}': {}",
                    tokenMetadata.getSubject(), e.getMessage(), e);
            return false;
        }
    }

    // CIP-26 logo size cap: ~87,400 base64 chars → 65,536 decoded bytes (64 KiB).
    // The decoded size is what matters; base64 is a 4:3 expansion, so we check the
    // decoded byte length directly rather than the encoded length.
    private static final int LOGO_MAX_DECODED_BYTES = 65_536;

    /**
     * Validates a logo string according to CIP-26 specification.
     *
     * <p>Two checks: must be valid base64, and the decoded payload must not exceed
     * {@value #LOGO_MAX_DECODED_BYTES} bytes. Image format (PNG/SVG/etc.) is
     * intentionally NOT checked — CIP-26 currently recommends PNG but the spec may
     * broaden, and our validator should not be stricter than upstream
     * cf-tokens-cip26 on a format dimension.
     *
     * <p>The earlier implementation built a fake {@code Metadata} with {@code "dummy"}
     * placeholder name/description fields just to bypass the upstream full-record
     * validator's required-field checks, then post-filtered errors back to LOGO/SUBJECT.
     * That coupled this method to internal validator quirks (e.g., dummy-name acceptance)
     * and produced misleading log lines (a SUBJECT error reported under "logo failed").
     *
     * @param subject the token subject (for logging)
     * @param logo    the logo string to validate (can be null)
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validateLogo(String subject, String logo) {
        if (logo == null || logo.isEmpty()) {
            return true;
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(logo);
        } catch (IllegalArgumentException e) {
            log.warn("CIP-26 logo validation failed for subject '{}': not valid base64",
                    subject);
            return false;
        }

        if (decoded.length > LOGO_MAX_DECODED_BYTES) {
            log.warn("CIP-26 logo validation failed for subject '{}': decoded size {} > {} bytes",
                    subject, decoded.length, LOGO_MAX_DECODED_BYTES);
            return false;
        }

        return true;
    }

    private Metadata convertToMetadata(Cip26Metadata tokenMetadata) {
        Metadata metadata = new Metadata();

        if (tokenMetadata.getSubject() != null) {
            metadata.setSubject(tokenMetadata.getSubject());
        }
        if (tokenMetadata.getName() != null) {
            metadata.addProperty(ValidationField.NAME, new MetadataProperty<>(tokenMetadata.getName()));
        }
        if (tokenMetadata.getDescription() != null) {
            metadata.addProperty(ValidationField.DESCRIPTION, new MetadataProperty<>(tokenMetadata.getDescription()));
        }
        if (tokenMetadata.getTicker() != null) {
            metadata.addProperty(ValidationField.TICKER, new MetadataProperty<>(tokenMetadata.getTicker()));
        }
        if (tokenMetadata.getDecimals() != null) {
            metadata.addProperty(ValidationField.DECIMALS, new MetadataProperty<>(tokenMetadata.getDecimals().intValue()));
        }
        if (tokenMetadata.getUrl() != null) {
            metadata.addProperty(ValidationField.URL, new MetadataProperty<>(tokenMetadata.getUrl()));
        }

        return metadata;
    }
}
