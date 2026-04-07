package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenMappingService {

    private final ObjectMapper objectMapper;

    public Optional<Mapping> parseMappings(File mappingFile) {
        try {
            return Optional.of(objectMapper.readValue(mappingFile, Mapping.class));
        } catch (Exception e) {
            log.warn("could not process file {}", mappingFile.getName(), e);
            return Optional.empty();
        }
    }

}
