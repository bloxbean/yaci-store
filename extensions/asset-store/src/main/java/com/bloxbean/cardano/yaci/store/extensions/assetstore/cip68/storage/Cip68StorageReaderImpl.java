package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.model.MetadataReferenceNft;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.FungibleTokenMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.storage.impl.repository.MetadataReferenceNftRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.service.Cip68TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip68.model.Cip68Constants.LABEL_FT;

/**
 * Fungible token (label 333) implementation of {@link Cip68StorageReader}.
 * All queries are scoped to label 333 to avoid mixing with future NFT data.
 */
@RequiredArgsConstructor
public class Cip68StorageReaderImpl implements Cip68StorageReader {

    private final Cip68TokenService cip68TokenService;
    private final MetadataReferenceNftRepository metadataReferenceNftRepository;

    @Override
    public Optional<FungibleTokenMetadata> findByPolicyIdAndAssetName(String policyId, String assetName) {
        return cip68TokenService.findSubject(policyId, assetName, List.of());
    }

    @Override
    public Optional<FungibleTokenMetadata> findBySubject(String subject) {
        return findBySubject(subject, List.of());
    }

    @Override
    public Optional<FungibleTokenMetadata> findBySubject(String subject, List<String> properties) {
        return cip68TokenService.getReferenceNftSubject(subject)
                .flatMap(assetType -> cip68TokenService.findSubject(
                        assetType.policyId(), assetType.assetName(), properties));
    }

    @Override
    public List<MetadataReferenceNft> findAllByPolicyId(String policyId) {
        return metadataReferenceNftRepository.findByPolicyIdAndLabel(policyId, LABEL_FT);
    }

    @Override
    public List<MetadataReferenceNft> findHistory(String policyId, String assetName, int page, int count) {
        return metadataReferenceNftRepository.findByPolicyIdAndAssetNameAndLabelOrderBySlotDesc(
                policyId, assetName, LABEL_FT,
                PageRequest.of(page, Math.min(count, 100), Sort.by("slot").descending())
        ).getContent();
    }

    @Override
    public List<MetadataReferenceNft> findLatestByPolicyIds(Collection<String> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return List.of();
        }
        return metadataReferenceNftRepository.findLatestByPolicyIdsAndLabel(policyIds, LABEL_FT);
    }

    @Override
    public long count() {
        return metadataReferenceNftRepository.countByLabelAndPolicyIdNotNull(LABEL_FT);
    }
}
