package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.Cip26MetadataRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Cip26StorageReaderImpl implements Cip26StorageReader {

    private final Cip26MetadataRepository cip26MetadataRepository;

    @Override
    public Optional<Cip26Metadata> findBySubject(String subject) {
        return cip26MetadataRepository.findById(subject);
    }

    @Override
    public List<Cip26Metadata> findBySubjects(List<String> subjects) {
        return cip26MetadataRepository.findAllById(subjects);
    }

    @Override
    public Optional<String> findLogoBySubject(String subject) {
        return cip26MetadataRepository.findById(subject)
                .map(Cip26Metadata::getLogo);
    }

    @Override
    public Map<String, String> findLogosBySubjects(List<String> subjects) {
        return cip26MetadataRepository.findAllById(subjects).stream()
                .filter(row -> row.getLogo() != null)
                .collect(Collectors.toMap(Cip26Metadata::getSubject, Cip26Metadata::getLogo));
    }

}
