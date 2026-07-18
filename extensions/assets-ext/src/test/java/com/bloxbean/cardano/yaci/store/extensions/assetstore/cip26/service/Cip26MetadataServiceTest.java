package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.Cip26MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.Cip26MetadataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip26MetadataService")
class Cip26MetadataServiceTest {

    @Mock private Cip26MetadataRepository cip26MetadataRepository;
    @Mock private Cip26MetadataValidator cip26MetadataValidator;

    private Cip26MetadataService service;

    @BeforeEach
    void setUp() {
        service = new Cip26MetadataService(cip26MetadataRepository, cip26MetadataValidator);
    }

    private static Mapping mapping(String subject) {
        // Item record fields: (sequenceNumber, value, signatures)
        return new Mapping(subject, null,
                new Item(1, "Name", null), null, null,
                new Item(1, "data:image/png;base64,iVBOR...", null), "policy",
                new Item(1, "Description", null));
    }

    @Nested
    @DisplayName("insertMapping")
    class InsertMapping {

        @Test
        void returnsInsertedOnHappyPath() {
            when(cip26MetadataValidator.validate(any())).thenReturn(true);

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.INSERTED);
            verify(cip26MetadataRepository).save(any(Cip26Metadata.class));
        }

        @Test
        void returnsPermanentlySkippedWhenValidationFails() {
            // The validator (yaci wrapper around cf-tokens-cip26) rejects the row.
            // Retrying the same row will reject again — must not block the cursor.
            when(cip26MetadataValidator.validate(any())).thenReturn(false);

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
            verify(cip26MetadataRepository, never()).save(any(Cip26Metadata.class));
        }

        @Test
        void returnsPermanentlySkippedOnNonTransientDataAccessException() {
            // Constraint violation, value too long, encoding error. The same insert
            // would fail again — advance past it instead of looping forever.
            when(cip26MetadataValidator.validate(any())).thenReturn(true);
            when(cip26MetadataRepository.save(any(Cip26Metadata.class)))
                    .thenThrow(new DataIntegrityViolationException("value too long for column"));

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
        }

        @Test
        void returnsTransientlyFailedOnTransientDataAccessException() {
            // Lock timeout, lost connection — likely to succeed next time. Block
            // the cursor advance so the next sync retries this entry.
            when(cip26MetadataValidator.validate(any())).thenReturn(true);
            when(cip26MetadataRepository.save(any(Cip26Metadata.class)))
                    .thenThrow(new CannotAcquireLockException("lock timeout"));

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.TRANSIENTLY_FAILED);
        }

        @Test
        void returnsTransientlyFailedOnUnknownException() {
            // Conservative default: anything we can't classify is treated as
            // transient so we don't silently drop. Recurring failures will show
            // up in logs every sync and need human investigation.
            when(cip26MetadataValidator.validate(any())).thenReturn(true);
            when(cip26MetadataRepository.save(any(Cip26Metadata.class)))
                    .thenThrow(new RuntimeException("something unexpected"));

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.TRANSIENTLY_FAILED);
        }
    }

    @Nested
    @DisplayName("insertLogo (updates existing Cip26Metadata row)")
    class InsertLogo {

        @Test
        void updatesLogoOnExistingRow() {
            Cip26Metadata existing = new Cip26Metadata();
            existing.setSubject("subject1");
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(true);
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.of(existing));

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.INSERTED);
            // Logo is now set on the merged-in Cip26Metadata row, not a separate TokenLogo entity.
            verify(cip26MetadataRepository).save(existing);
            assertThat(existing.getLogo()).isEqualTo("data:image/png;base64,iVBOR...");
        }

        @Test
        void permanentlySkipsWhenNoMetadataRowExists() {
            // Orphan logo — the registry has a logo file but no metadata file. Skip
            // permanently because retrying won't change the registry's state and an
            // orphan row would have nowhere to surface in the API.
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(true);
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.empty());

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
            verify(cip26MetadataRepository, never()).save(any(Cip26Metadata.class));
        }

        @Test
        void returnsPermanentlySkippedOnNonTransientDataAccessException() {
            Cip26Metadata existing = new Cip26Metadata();
            existing.setSubject("subject1");
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(true);
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.of(existing));
            when(cip26MetadataRepository.save(any(Cip26Metadata.class)))
                    .thenThrow(new DataIntegrityViolationException("logo too large"));

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
        }

        @Test
        void returnsTransientlyFailedOnTransientDataAccessException() {
            Cip26Metadata existing = new Cip26Metadata();
            existing.setSubject("subject1");
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(true);
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.of(existing));
            when(cip26MetadataRepository.save(any(Cip26Metadata.class)))
                    .thenThrow(new CannotAcquireLockException("lock timeout"));

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.TRANSIENTLY_FAILED);
        }
    }
}
