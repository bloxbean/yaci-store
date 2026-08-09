package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenMappingService {

    private final ObjectMapper objectMapper;

    /**
     * Parses a single registry mapping file. Returns {@link Optional#empty()} on any failure
     * so the sync loop can keep going.
     *
     * <p>Exceptions are split by category so operators can distinguish:
     * <ul>
     *   <li>{@link JsonProcessingException} — malformed minter data; expected occurrence on
     *       public registries, logged at WARN with filename only (no stack trace);</li>
     *   <li>{@link IOException} — file/disk read failure; infrastructure-shaped, logged at
     *       ERROR with full stack trace so it surfaces in alerting.</li>
     * </ul>
     * Both buckets currently translate to the same downstream outcome
     * ({@code Cip26MetadataSyncService.FileOutcome.SKIPPED_NO_MAPPING}), so the cursor
     * behaviour is unchanged — this is a logging-hygiene fix, not a control-flow change.
     */
    public Optional<Mapping> parseMappings(File mappingFile) {
        try {
            return Optional.of(objectMapper.readValue(mappingFile, Mapping.class));
        } catch (JsonProcessingException e) {
            log.warn("Malformed JSON in registry file '{}': {}", mappingFile.getName(), e.getOriginalMessage());
            return Optional.empty();
        } catch (IOException e) {
            log.error("I/O failure reading registry file '{}'", mappingFile.getName(), e);
            return Optional.empty();
        }
    }

}
