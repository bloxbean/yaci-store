package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.Cip26MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.MappingsUtil;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.Cip26MetadataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.MappingsUtil.extractLogo;

@Service
@Slf4j
@RequiredArgsConstructor
public class Cip26MetadataService {

    private final Cip26MetadataRepository cip26MetadataRepository;
    private final Cip26MetadataValidator cip26MetadataValidator;

    /**
     * Validates and inserts CIP-26 metadata for one mapping.
     * <p>
     * Logo is intentionally <em>not</em> set here — see {@link #insertLogo(Mapping)}, which
     * runs a separate validation pass and updates the same row. The two-step flow predates
     * the schema merge and is preserved so the validation paths stay independent (a row
     * with valid metadata but a too-large logo can still be saved with the metadata only).
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
        Cip26Metadata cip26Metadata = MappingsUtil.toCip26Metadata(mapping, updateBy, updatedAt);

        if (!cip26MetadataValidator.validate(cip26Metadata)) {
            // Validator already logged the specific reason at WARN.
            return InsertOutcome.PERMANENTLY_SKIPPED;
        }

        try {
            cip26Metadata.setLastSyncedAt(LocalDateTime.now());
            cip26MetadataRepository.save(cip26Metadata);
            return InsertOutcome.INSERTED;
        } catch (NonTransientDataAccessException e) {
            // Constraint violation, column too narrow, encoding error — won't
            // help to retry. Log loudly so operators can either widen the
            // schema, tighten the validator, or fix upstream data.
            log.error("Permanent save failure for token subject '{}', skipping: {}",
                    cip26Metadata.getSubject(), e.getMessage());
            return InsertOutcome.PERMANENTLY_SKIPPED;
        } catch (TransientDataAccessException e) {
            log.warn("Transient save failure for token subject '{}', will retry next sync: {}",
                    cip26Metadata.getSubject(), e.getMessage());
            return InsertOutcome.TRANSIENTLY_FAILED;
        } catch (Exception e) {
            // Unknown exception type — conservative default is "transient" so
            // we don't silently drop. Recurring failures will show up in logs
            // every sync and need investigation.
            log.error("Unclassified save failure for token subject '{}', will retry next sync: {}",
                    cip26Metadata.getSubject(), e.getMessage(), e);
            return InsertOutcome.TRANSIENTLY_FAILED;
        }
    }

    /**
     * Validates the logo and writes it onto the existing {@link Cip26Metadata} row for
     * this subject. Same outcome semantics as {@link #insertMapping(Mapping, LocalDateTime, String)}.
     * <p>
     * If no metadata row exists for the subject (either because {@code insertMapping}
     * skipped it earlier in this sync, or because of an upstream registry oddity where a
     * logo file exists without a metadata file), the logo write is skipped — orphan logos
     * have no way to surface in the API anyway.
     */
    @Transactional
    public InsertOutcome insertLogo(Mapping mapping) {
        String subject = mapping.subject();
        String logo = extractLogo(mapping);

        if (!cip26MetadataValidator.validateLogo(subject, logo)) {
            return InsertOutcome.PERMANENTLY_SKIPPED;
        }

        try {
            Optional<Cip26Metadata> existing = cip26MetadataRepository.findById(subject);
            if (existing.isEmpty()) {
                // Orphan logo (no metadata row). Skip permanently — the registry has odd
                // state but retrying won't change it; operator should surface this upstream.
                log.warn("Skipping logo for subject '{}': no Cip26Metadata row exists", subject);
                return InsertOutcome.PERMANENTLY_SKIPPED;
            }
            Cip26Metadata row = existing.get();
            row.setLogo(logo);
            row.setLastSyncedAt(LocalDateTime.now());
            cip26MetadataRepository.save(row);
            return InsertOutcome.INSERTED;
        } catch (NonTransientDataAccessException e) {
            log.error("Permanent save failure for logo subject '{}', skipping: {}",
                    subject, e.getMessage());
            return InsertOutcome.PERMANENTLY_SKIPPED;
        } catch (TransientDataAccessException e) {
            log.warn("Transient save failure for logo subject '{}', will retry next sync: {}",
                    subject, e.getMessage());
            return InsertOutcome.TRANSIENTLY_FAILED;
        } catch (Exception e) {
            log.error("Unclassified save failure for logo subject '{}', will retry next sync: {}",
                    subject, e.getMessage(), e);
            return InsertOutcome.TRANSIENTLY_FAILED;
        }
    }

}
