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
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.MappingsUtil.toTokenLogo;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMetadataService {

    private final TokenMetadataRepository tokenMetadataRepository;
    private final TokenLogoRepository tokenLogoRepository;
    private final TokenMetadataValidator tokenMetadataValidator;
    private final Clock clock;

    /**
     * Validates and inserts CIP-26 metadata for one mapping.
     *
     * @return {@link InsertOutcome#INSERTED} on success;
     *         {@link InsertOutcome#PERMANENTLY_SKIPPED} when validation fails
     *         or the database rejects the row with a non-transient error;
     *         {@link InsertOutcome#TRANSIENTLY_FAILED} when the database error
     *         looks recoverable (or is an unfamiliar exception we can't
     *         classify — the conservative default to avoid silent drops).
     */
    @Transactional
    public InsertOutcome insertMapping(Mapping mapping, LocalDateTime updatedAt, String updateBy) {
        TokenMetadata tokenMetadata = MappingsUtil.toTokenMetadata(mapping, updateBy, updatedAt);

        if (!tokenMetadataValidator.validate(tokenMetadata)) {
            // Validator already logged the specific reason at WARN.
            return InsertOutcome.PERMANENTLY_SKIPPED;
        }

        try {
            tokenMetadata.setLastSyncedAt(LocalDateTime.now(clock));
            tokenMetadataRepository.save(tokenMetadata);
            return InsertOutcome.INSERTED;
        } catch (NonTransientDataAccessException e) {
            // Constraint violation, column too narrow, encoding error — won't
            // help to retry. Log loudly so operators can either widen the
            // schema, tighten the validator, or fix upstream data.
            log.error("Permanent save failure for token subject '{}', skipping: {}",
                    tokenMetadata.getSubject(), e.getMessage());
            return InsertOutcome.PERMANENTLY_SKIPPED;
        } catch (TransientDataAccessException e) {
            log.warn("Transient save failure for token subject '{}', will retry next sync: {}",
                    tokenMetadata.getSubject(), e.getMessage());
            return InsertOutcome.TRANSIENTLY_FAILED;
        } catch (Exception e) {
            // Unknown exception type — conservative default is "transient" so
            // we don't silently drop. Recurring failures will show up in logs
            // every sync and need investigation.
            log.error("Unclassified save failure for token subject '{}', will retry next sync: {}",
                    tokenMetadata.getSubject(), e.getMessage(), e);
            return InsertOutcome.TRANSIENTLY_FAILED;
        }
    }

    /**
     * Validates and inserts the logo for one mapping. Same outcome semantics
     * as {@link #insertMapping(Mapping, LocalDateTime, String)}.
     */
    @Transactional
    public InsertOutcome insertLogo(Mapping mapping) {
        TokenLogo tokenLogo = toTokenLogo(mapping);

        if (!tokenMetadataValidator.validateLogo(tokenLogo.getSubject(), tokenLogo.getLogo())) {
            return InsertOutcome.PERMANENTLY_SKIPPED;
        }

        try {
            tokenLogo.setLastSyncedAt(LocalDateTime.now(clock));
            tokenLogoRepository.save(tokenLogo);
            return InsertOutcome.INSERTED;
        } catch (NonTransientDataAccessException e) {
            log.error("Permanent save failure for logo subject '{}', skipping: {}",
                    tokenLogo.getSubject(), e.getMessage());
            return InsertOutcome.PERMANENTLY_SKIPPED;
        } catch (TransientDataAccessException e) {
            log.warn("Transient save failure for logo subject '{}', will retry next sync: {}",
                    tokenLogo.getSubject(), e.getMessage());
            return InsertOutcome.TRANSIENTLY_FAILED;
        } catch (Exception e) {
            log.error("Unclassified save failure for logo subject '{}', will retry next sync: {}",
                    tokenLogo.getSubject(), e.getMessage(), e);
            return InsertOutcome.TRANSIENTLY_FAILED;
        }
    }

}
