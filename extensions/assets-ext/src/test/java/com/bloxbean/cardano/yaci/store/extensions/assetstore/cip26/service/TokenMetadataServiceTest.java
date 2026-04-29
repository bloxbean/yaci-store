package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Item;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenLogo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenLogoRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenMetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.TokenMetadataValidator;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenMetadataService")
class TokenMetadataServiceTest {

    @Mock private TokenMetadataRepository tokenMetadataRepository;
    @Mock private TokenLogoRepository tokenLogoRepository;
    @Mock private TokenMetadataValidator tokenMetadataValidator;

    private TokenMetadataService service;

    @BeforeEach
    void setUp() {
        service = new TokenMetadataService(
                tokenMetadataRepository, tokenLogoRepository, tokenMetadataValidator);
    }

    private static Mapping mapping(String subject) {
        // Item record fields: (sequenceNumber, value, signatures)
        return new Mapping(subject, null,
                new Item(1, "Name", null), null, null, null, "policy",
                new Item(1, "Description", null));
    }

    @Nested
    @DisplayName("insertMapping")
    class InsertMapping {

        @Test
        void returnsInsertedOnHappyPath() {
            when(tokenMetadataValidator.validate(any())).thenReturn(true);

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.INSERTED);
            verify(tokenMetadataRepository).save(any(TokenMetadata.class));
        }

        @Test
        void returnsPermanentlySkippedWhenValidationFails() {
            // The validator (yaci wrapper around cf-tokens-cip26) rejects the row.
            // Retrying the same row will reject again — must not block the cursor.
            when(tokenMetadataValidator.validate(any())).thenReturn(false);

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
            verify(tokenMetadataRepository, never()).save(any(TokenMetadata.class));
        }

        @Test
        void returnsPermanentlySkippedOnNonTransientDataAccessException() {
            // Constraint violation, value too long, encoding error. The same insert
            // would fail again — advance past it instead of looping forever.
            when(tokenMetadataValidator.validate(any())).thenReturn(true);
            when(tokenMetadataRepository.save(any(TokenMetadata.class)))
                    .thenThrow(new DataIntegrityViolationException("value too long for column"));

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
        }

        @Test
        void returnsTransientlyFailedOnTransientDataAccessException() {
            // Lock timeout, lost connection — likely to succeed next time. Block
            // the cursor advance so the next sync retries this entry.
            when(tokenMetadataValidator.validate(any())).thenReturn(true);
            when(tokenMetadataRepository.save(any(TokenMetadata.class)))
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
            when(tokenMetadataValidator.validate(any())).thenReturn(true);
            when(tokenMetadataRepository.save(any(TokenMetadata.class)))
                    .thenThrow(new RuntimeException("something unexpected"));

            InsertOutcome outcome = service.insertMapping(
                    mapping("subject1"), LocalDateTime.now(), "author@test.com");

            assertThat(outcome).isEqualTo(InsertOutcome.TRANSIENTLY_FAILED);
        }
    }

    @Nested
    @DisplayName("insertLogo")
    class InsertLogo {

        @Test
        void returnsInsertedOnHappyPath() {
            when(tokenMetadataValidator.validateLogo(any(), any())).thenReturn(true);

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.INSERTED);
            verify(tokenLogoRepository).save(any(TokenLogo.class));
        }

        @Test
        void returnsPermanentlySkippedOnNonTransientDataAccessException() {
            when(tokenMetadataValidator.validateLogo(any(), any())).thenReturn(true);
            when(tokenLogoRepository.save(any(TokenLogo.class)))
                    .thenThrow(new DataIntegrityViolationException("logo too large"));

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.PERMANENTLY_SKIPPED);
        }

        @Test
        void returnsTransientlyFailedOnTransientDataAccessException() {
            when(tokenMetadataValidator.validateLogo(any(), any())).thenReturn(true);
            when(tokenLogoRepository.save(any(TokenLogo.class)))
                    .thenThrow(new CannotAcquireLockException("lock timeout"));

            InsertOutcome outcome = service.insertLogo(mapping("subject1"));

            assertThat(outcome).isEqualTo(InsertOutcome.TRANSIENTLY_FAILED);
        }
    }
}
