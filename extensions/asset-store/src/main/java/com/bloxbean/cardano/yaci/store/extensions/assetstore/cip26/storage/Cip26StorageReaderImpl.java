package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository.TokenLogoRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository.TokenMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
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
                .map(tokenLogo -> tokenLogo.getLogo());
    }
}
