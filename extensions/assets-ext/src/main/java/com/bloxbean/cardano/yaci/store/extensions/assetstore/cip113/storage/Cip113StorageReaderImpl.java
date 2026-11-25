package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Looks a policy up by the {@code key} of the registry nodes. Keys are stored as the datum holds them, in lower-case
 * hex, so a policy ID asked in upper case is found too. The origin node (empty key) is never matched: no policy ID is
 * empty.
 */
@RequiredArgsConstructor
public class Cip113StorageReaderImpl implements Cip113StorageReader {

    private final Cip113RegistryNodeRepository registryNodeRepository;
    private final Cip113Configuration cip113Configuration;

    @Override
    public Optional<ProgrammableTokenCip113> findByPolicyId(String policyId) {
        if (!cip113Configuration.isEnabled() || policyId == null || policyId.isEmpty()) {
            return Optional.empty();
        }
        return registryNodeRepository.findFirstByKeyOrderBySlotDescTxIndexDesc(policyId.toLowerCase(Locale.ROOT))
                .map(Cip113StorageReaderImpl::toExtension);
    }

    @Override
    public Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds) {
        if (!cip113Configuration.isEnabled()) {
            return Map.of();
        }
        List<String> keys = policyIds.stream()
                .filter(id -> id != null && !id.isEmpty())
                .map(id -> id.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        if (keys.isEmpty()) {
            return Map.of();
        }
        return registryNodeRepository.findLatestByKeys(keys).stream()
                .collect(Collectors.toMap(Cip113RegistryNode::getKey, Cip113StorageReaderImpl::toExtension));
    }

    static ProgrammableTokenCip113 toExtension(Cip113RegistryNode node) {
        return new ProgrammableTokenCip113(
                node.getMintingLogicScript(),
                node.getTransferLogicScript(),
                node.getThirdPartyTransferLogicScript(),
                node.getUnfrackingLogicScript(),
                node.getGlobalStatePolicyId());
    }
}
