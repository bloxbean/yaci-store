package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.metadatatools.core.cip26.MetadataCreator;
import org.cardanofoundation.metadatatools.core.cip26.ValidationError;
import org.cardanofoundation.metadatatools.core.cip26.ValidationField;
import org.cardanofoundation.metadatatools.core.cip26.ValidationResult;
import org.cardanofoundation.metadatatools.core.cip26.model.Metadata;
import org.cardanofoundation.metadatatools.core.cip26.model.MetadataProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates token metadata fields against the CIP-26 specification using the
 * <a href="https://github.com/cardano-foundation/cf-token-metadata-registry">cf-tokens-cip26</a> library.
 */
@Component
@Slf4j
public class TokenMetadataValidator {

    /**
     * Validates that all fields in the TokenMetadata entity comply with CIP-26 specification.
     *
     * @param tokenMetadata the token metadata to validate
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validate(TokenMetadata tokenMetadata) {
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

    /**
     * Validates a logo string according to CIP-26 specification.
     *
     * @param subject the token subject (for logging)
     * @param logo    the logo string to validate (can be null)
     * @return true if valid according to CIP-26, false otherwise
     */
    public boolean validateLogo(String subject, String logo) {
        if (logo == null || logo.isEmpty()) {
            return true;
        }

        try {
            Metadata metadata = new Metadata();
            metadata.setSubject(subject);
            metadata.addProperty(ValidationField.NAME, new MetadataProperty<>("dummy"));
            metadata.addProperty(ValidationField.DESCRIPTION, new MetadataProperty<>("dummy"));
            metadata.addProperty(ValidationField.LOGO, new MetadataProperty<>(logo));

            ValidationResult validationResult = MetadataCreator.validateMetadata(metadata);

            if (!validationResult.isValid()) {
                List<ValidationError> logoErrors = validationResult.getValidationErrorsForField(ValidationField.LOGO);
                List<ValidationError> subjectErrors = validationResult.getValidationErrorsForField(ValidationField.SUBJECT);

                if (!logoErrors.isEmpty() || !subjectErrors.isEmpty()) {
                    List<ValidationError> relevantErrors = new ArrayList<>(logoErrors);
                    relevantErrors.addAll(subjectErrors);

                    String errorMessages = relevantErrors.stream()
                            .map(error -> error.getField() + ": " + error.getMessage())
                            .collect(Collectors.joining(", "));

                    log.warn("CIP-26 logo validation failed for subject '{}': {}",
                            subject, errorMessages);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            log.error("Error during CIP-26 logo validation for subject '{}': {}",
                    subject, e.getMessage(), e);
            return false;
        }
    }

    private Metadata convertToMetadata(TokenMetadata tokenMetadata) {
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
