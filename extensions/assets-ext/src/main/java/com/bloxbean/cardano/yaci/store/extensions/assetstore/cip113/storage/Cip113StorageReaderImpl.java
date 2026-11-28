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
 * <p>
 * Only the nodes of the configured registries count. A policy registered in more than one of them is served from its
 * most recent node: the latest by slot, then transaction index, then output index.
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
        return registryNodeRepository.findFirstByKeyAndRegistryPolicyIdInOrderBySlotDescTxIndexDescOutputIndexDesc(
                        policyId.toLowerCase(Locale.ROOT), cip113Configuration.getRegistryNftPolicyIds())
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
        return registryNodeRepository.findLatestByKeys(keys, cip113Configuration.getRegistryNftPolicyIds()).stream()
                .collect(Collectors.toMap(Cip113RegistryNode::getKey, node -> node, Cip113StorageReaderImpl::later))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> toExtension(entry.getValue())));
    }

    /** Of two nodes of a key in the same transaction, in two registries: the later output, as the single lookup takes. */
    private static Cip113RegistryNode later(Cip113RegistryNode a, Cip113RegistryNode b) {
        return b.getOutputIndex() > a.getOutputIndex() ? b : a;
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
