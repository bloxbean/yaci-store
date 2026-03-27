package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.TokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository.TokenLogoRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository.TokenMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    @Override
    public List<TokenMetadata> findByPolicy(String policyId) {
        return tokenMetadataRepository.findByPolicy(policyId);
    }

    @Override
    public List<TokenMetadata> searchByName(String name, int page, int count) {
        Pageable pageable = PageRequest.of(page, Math.min(count, 100));
        return tokenMetadataRepository.findByNameContainingIgnoreCase(name, pageable).getContent();
    }

    @Override
    public List<TokenMetadata> findByTicker(String ticker, int page, int count) {
        Pageable pageable = PageRequest.of(page, Math.min(count, 100));
        return tokenMetadataRepository.findByTickerIgnoreCase(ticker, pageable).getContent();
    }

    @Override
    public long count() {
        return tokenMetadataRepository.countByPolicyNotNull();
    }
}
