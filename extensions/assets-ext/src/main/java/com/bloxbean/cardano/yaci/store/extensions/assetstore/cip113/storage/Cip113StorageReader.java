package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only access to CIP-113 programmable token registry data.
 * <p>
 * CIP-113 registry nodes are NFTs on-chain that declare transfer validation scripts
 * for token policies. Each entry maps a policy ID to its transfer logic scripts.
 */
public interface Cip113StorageReader {

    /**
     * Get the latest CIP-113 registry state for a policy ID.
     *
     * @param policyId the policy ID to look up
     * @return the programmable token info (transfer scripts) if the policy is registered
     */
    Optional<ProgrammableTokenCip113> findByPolicyId(String policyId);

    /**
     * Batch lookup CIP-113 registry nodes for multiple policy IDs.
     * Returns the latest state per policy, using a single optimized query.
     *
     * @param policyIds the policy IDs to look up
     * @return map of policyId to CIP-113 info for registered policies (unregistered policies are omitted)
     */
    Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds);

    /**
     * Check if a policy ID is registered as a CIP-113 programmable token.
     *
     * @param policyId the policy ID to check
     * @return true if a registry node exists for this policy
     */
    boolean isProgrammableToken(String policyId);

    /**
     * Get the raw registry node entity for a policy ID (includes slot, tx_hash, datum).
     * Useful when you need the full on-chain context, not just the parsed scripts.
     *
     * @param policyId the policy ID
     * @return the latest raw entity if found
     */
    Optional<Cip113RegistryNode> findRawByPolicyId(String policyId);

    /**
     * Get all known programmable token policy IDs.
     *
     * @return list of distinct policy IDs that have registry nodes
     */
    List<String> findAllProgrammableTokenPolicyIds();
}
