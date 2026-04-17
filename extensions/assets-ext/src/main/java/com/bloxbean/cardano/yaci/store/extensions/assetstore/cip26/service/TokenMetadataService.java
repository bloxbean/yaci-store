package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenLogo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenLogoRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenMetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.MappingsUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.TokenMetadataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.MappingsUtil.toTokenLogo;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMetadataService {

    private final TokenMetadataRepository tokenMetadataRepository;
    private final TokenLogoRepository tokenLogoRepository;
    private final TokenMetadataValidator tokenMetadataValidator;

    /**
     * Inserts mapping metadata into the database.
     * Validates metadata before insertion.
     *
     * @return true if successfully inserted, false if validation failed or error occurred
     */
    @Transactional
    public boolean insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy) {
        TokenMetadata tokenMetadata = MappingsUtil.toTokenMetadata(mapping, updateBy, updatedAt);

        if (!tokenMetadataValidator.validate(tokenMetadata)) {
            log.warn("Skipping token metadata for subject '{}' - validation failed", tokenMetadata.getSubject());
            return false;
        }

        try {
            tokenMetadata.setLastSyncedAt(java.time.LocalDateTime.now());
            tokenMetadataRepository.save(tokenMetadata);
            return true;
        } catch (Exception e) {
            log.error("Failed to save token metadata for subject '{}': {}", tokenMetadata.getSubject(), e.getMessage());
            return false;
        }
    }

    /**
     * Inserts logo data into the database.
     * Validates logo according to CIP-26 before insertion.
     *
     * @return true if successfully inserted, false if validation failed or error occurred
     */
    @Transactional
    public boolean insertLogo(Mapping mapping) {
        TokenLogo tokenLogo = toTokenLogo(mapping);

        if (!tokenMetadataValidator.validateLogo(tokenLogo.getSubject(), tokenLogo.getLogo())) {
            log.warn("Skipping logo for subject '{}' - validation failed", tokenLogo.getSubject());
            return false;
        }

        try {
            tokenLogo.setLastSyncedAt(java.time.LocalDateTime.now());
            tokenLogoRepository.save(tokenLogo);
            return true;
        } catch (Exception e) {
            log.error("Failed to save logo for subject '{}': {}", tokenLogo.getSubject(), e.getMessage());
            return false;
        }
    }

}
