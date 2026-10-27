package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import java.time.ZoneOffset;
import java.time.Instant;
import java.time.Clock;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.Cip26MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.Cip26MetadataValidator;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.Cip26Validation;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReason;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReport;
import org.mockito.ArgumentCaptor;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.validation.ValidationError;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip26MetadataService")
class Cip26MetadataServiceTest {

    @Mock private Cip26MetadataRepository cip26MetadataRepository;
    @Mock private Cip26MetadataValidator cip26MetadataValidator;
    @Mock private InvalidTokenRecorder invalidTokenRecorder;

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 12, 0);

    private Cip26MetadataService service;

    @BeforeEach
    void setUp() {
        service = new Cip26MetadataService(cip26MetadataRepository, cip26MetadataValidator, invalidTokenRecorder, CLOCK);
    }

    private InvalidTokenReport reported() {
        ArgumentCaptor<InvalidTokenReport> report = ArgumentCaptor.forClass(InvalidTokenReport.class);
        verify(invalidTokenRecorder).report(report.capture());
        return report.getValue();
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
            when(cip26MetadataValidator.validate(any(), any())).thenReturn(Cip26Validation.of(List.of()));

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.INSERTED);
            verify(cip26MetadataRepository).save(any(Cip26Metadata.class));
        }

        @Test
        void storesTheValidationErrorsWithTheRow() {
            ValidationError error = ValidationError.mandatory("CIP-26", null, "name", "missing_value", "name field is missing a mandatory value");
            when(cip26MetadataValidator.validate(any(), any())).thenReturn(Cip26Validation.of(List.of(error)));

            InsertOutcome outcome = service.insertMapping(mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.INSERTED);
            org.mockito.ArgumentCaptor<Cip26Metadata> saved = org.mockito.ArgumentCaptor.forClass(Cip26Metadata.class);
            verify(cip26MetadataRepository).save(saved.capture());
            assertThat(saved.getValue().getValidationErrors()).containsExactly(error);
        }

        @Test
        void stampsTheRowWithTheTimeOfTheClock() {
            when(cip26MetadataValidator.validate(any(), any())).thenReturn(Cip26Validation.of(List.of()));

            service.insertMapping(mapping("subject1"), LocalDateTime.of(2020, 1, 1, 0, 0), "author@test.com");

            org.mockito.ArgumentCaptor<Cip26Metadata> saved = org.mockito.ArgumentCaptor.forClass(Cip26Metadata.class);
            verify(cip26MetadataRepository).save(saved.capture());
            assertThat(saved.getValue().getLastSyncedAt()).isEqualTo(NOW);
        }

        @Test
        void returnsPermanentlySkippedWhenTheEntryIsDroppedAsUnstorable() {
            // The validator drops an entry whose subject cannot be a key (it is not CIP-26 metadata at all).
            // Retrying the same row will drop it again — must not block the cursor.
            when(cip26MetadataValidator.validate(any(), any()))
                    .thenReturn(Cip26Validation.dropped(InvalidTokenReason.UNUSABLE_SUBJECT, "the subject is missing or blank"));

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
            verify(cip26MetadataRepository, never()).save(any(Cip26Metadata.class));
            assertThat(reported()).satisfies(report -> {
                assertThat(report.reasonCode()).isEqualTo("unusable_subject");
                assertThat(report.origin()).isEqualTo("subject1.json");
            });
        }

        @Test
        void recordsAnUnexpectedValidationFailureAsAProcessingFailureNotAsAnUnusableSubject() {
            // the subject may be fine: something the validator did not foresee threw
            when(cip26MetadataValidator.validate(any(), any())).thenReturn(Cip26Validation.dropped(
                    InvalidTokenReason.PROCESSING_FAILURE, "validation failed unexpectedly: boom"));

            assertThat(service.insertMapping(mapping("subject1"), LocalDateTime.now(), "author@test.com"))
                    .isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
            assertThat(reported()).satisfies(report -> {
                assertThat(report.standard()).isEqualTo("CIP-26");
                assertThat(report.reasonCode()).isEqualTo("processing_failure");
                assertThat(report.reason()).contains("boom");
            });
        }

        @Test
        void returnsPermanentlySkippedOnNonTransientDataAccessException() {
            // Constraint violation, value too long, encoding error. The same insert
            // would fail again — advance past it instead of looping forever.
            when(cip26MetadataValidator.validate(any(), any())).thenReturn(Cip26Validation.of(List.of()));
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
            when(cip26MetadataValidator.validate(any(), any())).thenReturn(Cip26Validation.of(List.of()));
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
            when(cip26MetadataValidator.validate(any(), any())).thenReturn(Cip26Validation.of(List.of()));
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
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(Cip26Validation.of(List.of()));
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.of(existing));

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.INSERTED);
            // Logo is now set on the merged-in Cip26Metadata row, not a separate TokenLogo entity.
            verify(cip26MetadataRepository).save(existing);
            assertThat(existing.getLogo()).isEqualTo("data:image/png;base64,iVBOR...");
            assertThat(existing.getLastSyncedAt()).isEqualTo(NOW);
        }

        @Test
        void addsTheLogoErrorsToTheRowAndKeepsItsOtherErrors() {
            ValidationError nameError = ValidationError.mandatory("CIP-26", null, "name", "missing_value", "name field is missing a mandatory value");
            ValidationError oldLogoError = ValidationError.optional("CIP-26", null, "logo", "too_large", "old");
            ValidationError logoError = ValidationError.optional("CIP-26", null, "logo", "invalid_base64", "logo is not valid base64");
            Cip26Metadata existing = new Cip26Metadata();
            existing.setSubject("subject1");
            existing.setValidationErrors(List.of(nameError, oldLogoError));
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(Cip26Validation.of(List.of(logoError)));
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.of(existing));

            service.insertLogo(mapping("subject1"));

            // the logo is kept as written, and its errors replace the earlier logo errors
            assertThat(existing.getLogo()).isEqualTo("data:image/png;base64,iVBOR...");
            assertThat(existing.getValidationErrors()).containsExactly(nameError, logoError);
        }

        @Test
        void findsTheRowOfAnUpperCaseRegistryFileByItsLowerCaseSubject() {
            Cip26Metadata existing = new Cip26Metadata();
            existing.setSubject("abcdef");
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(Cip26Validation.of(List.of()));
            when(cip26MetadataRepository.findById("abcdef")).thenReturn(Optional.of(existing));

            assertThat(service.insertLogo(mapping("ABCDEF"))).isEqualTo(InsertOutcome.INSERTED);
            assertThat(existing.getLogo()).isEqualTo("data:image/png;base64,iVBOR...");
        }

        @Test
        void permanentlySkipsAnOversizedLogoThatLooksLikeAnAttack() {
            when(cip26MetadataValidator.validateLogo(any(), any()))
                    .thenReturn(Cip26Validation.dropped(InvalidTokenReason.LOGO_TOO_LARGE, "the logo has 2000000 characters"));

            assertThat(service.insertLogo(mapping("ABCDEF"))).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
            verify(cip26MetadataRepository, never()).save(any(Cip26Metadata.class));
            // the entry is stored without its logo, and the dropped logo is recorded
            assertThat(reported()).satisfies(report -> {
                assertThat(report.standard()).isEqualTo("CIP-26");
                assertThat(report.reasonCode()).isEqualTo("logo_too_large");
                assertThat(report.subject()).isEqualTo("abcdef");
                assertThat(report.origin()).isEqualTo("ABCDEF.json");
                assertThat(report.reason()).contains("2000000 characters");
            });
        }

        @Test
        void permanentlySkipsWhenNoMetadataRowExists() {
            // Orphan logo — the registry has a logo file but no metadata file. Skip
            // permanently because retrying won't change the registry's state and an
            // orphan row would have nowhere to surface in the API.
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(Cip26Validation.of(List.of()));
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.empty());

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
            verify(cip26MetadataRepository, never()).save(any(Cip26Metadata.class));
        }

        @Test
        void returnsPermanentlySkippedOnNonTransientDataAccessException() {
            Cip26Metadata existing = new Cip26Metadata();
            existing.setSubject("subject1");
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(Cip26Validation.of(List.of()));
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
            when(cip26MetadataValidator.validateLogo(any(), any())).thenReturn(Cip26Validation.of(List.of()));
            when(cip26MetadataRepository.findById("subject1")).thenReturn(Optional.of(existing));
            when(cip26MetadataRepository.save(any(Cip26Metadata.class)))
                    .thenThrow(new CannotAcquireLockException("lock timeout"));

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.TRANSIENTLY_FAILED);
        }
    }

    @Nested
    @DisplayName("deleteMapping")
    class DeleteMapping {

        private static final String SUBJECT =
                "ff7cad970d3a755a1ff0335ccb3f3c1cabf31aacf3f23dd13db61b0630313030";

        @Test
        void returnsDeletedOnHappyPath() {
            DeleteOutcome outcome = service.deleteMapping(SUBJECT);

            assertThat(outcome).isEqualTo(DeleteOutcome.DELETED);
            verify(cip26MetadataRepository).deleteById(SUBJECT);
        }

        @Test
        void deletesAnEntryWhoseFileNameIsInUpperCaseByItsStoredLowerCaseSubject() {
            assertThat(service.deleteMapping(SUBJECT.toUpperCase(Locale.ROOT))).isEqualTo(DeleteOutcome.DELETED);

            verify(cip26MetadataRepository).deleteById(SUBJECT);
        }

        @Test
        void returnsPermanentlyFailedOnNonTransientDataAccessException() {
            doThrow(new DataIntegrityViolationException("fk violation"))
                    .when(cip26MetadataRepository).deleteById(SUBJECT);

            DeleteOutcome outcome = service.deleteMapping(SUBJECT);

            assertThat(outcome).isEqualTo(DeleteOutcome.PERMANENTLY_FAILED);
        }

        @Test
        void returnsTransientlyFailedOnTransientDataAccessException() {
            doThrow(new CannotAcquireLockException("lock timeout"))
                    .when(cip26MetadataRepository).deleteById(SUBJECT);

            DeleteOutcome outcome = service.deleteMapping(SUBJECT);

            assertThat(outcome).isEqualTo(DeleteOutcome.TRANSIENTLY_FAILED);
        }

        @Test
        void returnsTransientlyFailedOnUnclassifiedException() {
            // Unknown exception type — conservative default so a removed token
            // is not silently left behind.
            doThrow(new RuntimeException("DB error"))
                    .when(cip26MetadataRepository).deleteById(SUBJECT);

            DeleteOutcome outcome = service.deleteMapping(SUBJECT);

            assertThat(outcome).isEqualTo(DeleteOutcome.TRANSIENTLY_FAILED);
        }
    }

    @Nested
    @DisplayName("findAllSubjects")
    class FindAllSubjects {

        @Test
        void delegatesToRepository() {
            when(cip26MetadataRepository.findAllSubjects())
                    .thenReturn(java.util.List.of("subject1", "subject2"));

            assertThat(service.findAllSubjects()).containsExactly("subject1", "subject2");
        }
    }
}
