package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26Metadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.Cip26MetadataRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.util.MappingsUtil;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Cip26StorageReaderImpl implements Cip26StorageReader {

    private final Cip26MetadataRepository cip26MetadataRepository;

    // A subject is stored in lower case, and a client can ask with the subject as the registry file writes it, in upper
    // case for 39 mainnet entries: what is asked for is looked up in lower case, and the maps are keyed by the stored subject.

    @Override
    public Optional<Cip26Metadata> findBySubject(String subject) {
        return cip26MetadataRepository.findById(MappingsUtil.normalizeSubject(subject));
    }

    @Override
    public List<Cip26Metadata> findBySubjects(List<String> subjects) {
        return cip26MetadataRepository.findAllById(storedSubjects(subjects));
    }

    @Override
    public Optional<String> findLogoBySubject(String subject) {
        return cip26MetadataRepository.findById(MappingsUtil.normalizeSubject(subject))
                .map(Cip26Metadata::getLogo);
    }

    @Override
    public Map<String, String> findLogosBySubjects(List<String> subjects) {
        return cip26MetadataRepository.findAllById(storedSubjects(subjects)).stream()
                .filter(row -> row.getLogo() != null)
                .collect(Collectors.toMap(Cip26Metadata::getSubject, Cip26Metadata::getLogo));
    }

    private static List<String> storedSubjects(List<String> subjects) {
        return subjects.stream().map(MappingsUtil::normalizeSubject).distinct().toList();
    }

}
