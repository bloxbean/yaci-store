package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * No-op implementation used when CIP-113 is disabled.
 * All methods return empty results.
 */
public class NoOpCip113StorageReader implements Cip113StorageReader {

    @Override
    public Optional<ProgrammableTokenCip113> findByPolicyId(String policyId) {
        return Optional.empty();
    }

    @Override
    public Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds) {
        return Map.of();
    }

    @Override
    public boolean isProgrammableToken(String policyId) {
        return false;
    }

    @Override
    public Optional<Cip113RegistryNode> findRawByPolicyId(String policyId) {
        return Optional.empty();
    }

    @Override
    public List<String> findAllProgrammableTokenPolicyIds() {
        return List.of();
    }
}
