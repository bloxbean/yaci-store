package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.entity.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.repository.MetadataReferenceNftRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68FungibleTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Cip68StorageReaderImpl implements Cip68StorageReader {

    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @Override
    public Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName) {
        return cip68FungibleTokenService.findSubject(policyId, assetName, List.of());
    }

    @Override
    public Optional<FungibleTokenMetadata> findBySubject(String subject) {
        return cip68FungibleTokenService.getReferenceNftSubject(subject)
                .flatMap(assetType -> cip68FungibleTokenService.findSubject(
                        assetType.policyId(), assetType.assetName(), List.of()));
    }

    @Override
    public List<MetadataReferenceNft> findAllByPolicyId(String policyId) {
        return metadataReferenceNftRepository.findByPolicyId(policyId);
    }

    @Override
    public List<MetadataReferenceNft> findHistory(String policyId, String assetName, int page, int count) {
        return metadataReferenceNftRepository.findByPolicyIdAndAssetNameOrderBySlotDesc(
                policyId, assetName,
                PageRequest.of(page, Math.min(count, 100), Sort.by("slot").descending())
        ).getContent();
    }

    @Override
    public long count() {
        return metadataReferenceNftRepository.countByPolicyIdNotNull();
    }
}
