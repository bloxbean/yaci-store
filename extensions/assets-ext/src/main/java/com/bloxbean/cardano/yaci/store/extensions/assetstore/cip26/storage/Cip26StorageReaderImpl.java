package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenLogo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenLogoRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.TokenMetadataRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Cip26StorageReaderImpl implements Cip26StorageReader {

    private final TokenMetadataRepository tokenMetadataRepository;
    private final TokenLogoRepository tokenLogoRepository;

    @Override
    public Optional<TokenMetadata> findBySubject(String subject) {
        return tokenMetadataRepository.findById(subject);
    }

    @Override
    public List<TokenMetadata> findBySubjects(List<String> subjects) {
        return tokenMetadataRepository.findAllById(subjects);
    }

    @Override
    public Optional<String> findLogoBySubject(String subject) {
        return tokenLogoRepository.findById(subject)
                .map(TokenLogo::getLogo);
    }

    @Override
    public Map<String, String> findLogosBySubjects(List<String> subjects) {
        return tokenLogoRepository.findAllById(subjects).stream()
                .filter(logo -> logo.getLogo() != null)
                .collect(Collectors.toMap(TokenLogo::getSubject, TokenLogo::getLogo));
    }
}
