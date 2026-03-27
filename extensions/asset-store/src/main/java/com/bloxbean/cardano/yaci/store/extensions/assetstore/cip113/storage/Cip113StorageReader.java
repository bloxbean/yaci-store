package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only access to CIP-113 programmable token registry data.
 */
public interface Cip113StorageReader {

    /**
     * Get the latest CIP-113 registry state for a policy ID.
     *
     * @param policyId the policy ID to look up
     * @return the programmable token info if the policy is registered
     */
    Optional<ProgrammableTokenCip113> findByPolicyId(String policyId);

    /**
     * Batch lookup CIP-113 registry nodes for multiple policy IDs.
     *
     * @param policyIds the policy IDs to look up
     * @return map of policyId → CIP-113 info for registered policies
     */
    Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds);

    /**
     * Check if a policy ID is registered as a CIP-113 programmable token.
     *
     * @param policyId the policy ID to check
     * @return true if registered
     */
    boolean isProgrammableToken(String policyId);
}
