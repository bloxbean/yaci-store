package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only access to the CIP-113 registry: the extension of a token, from the current node of its policy. Empty when
 * CIP-113 is off, so that every token is then served as {@code NATIVE}.
 */
public interface Cip113StorageReader {

    /** The extension of the tokens of a policy, or empty when the policy is not registered. */
    Optional<ProgrammableTokenCip113> findByPolicyId(String policyId);

    /** The extensions of the registered policies among {@code policyIds}, keyed by policy ID in lower case, in one query. */
    Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds);
}
