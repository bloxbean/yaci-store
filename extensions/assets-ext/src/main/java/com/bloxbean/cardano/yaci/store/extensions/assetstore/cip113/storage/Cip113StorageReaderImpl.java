package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Read-only access to the CIP-113 programmable-token registry.
 *
 * <p><b>Naming note:</b> this reader is deliberately named in policy-ID terms even though the
 * underlying column is {@code key} (see {@link Cip113RegistryNode} — the registry's
 * {@code key} is a policy ID for real rows, a linked-list marker for sentinel rows). The
 * reader contract with API callers is <i>"give me the programmable-token data for this
 * token's policy ID"</i>, so its public methods speak policy IDs and translate to
 * {@code key = ?} lookups internally. Sentinels are never matched because no real token has
 * an empty or 32-byte-of-{@code 0xFF} policy ID.
 */
@Component
@RequiredArgsConstructor
public class Cip113StorageReaderImpl implements Cip113StorageReader {

    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final Cip113Configuration cip113Configuration;

    @Override
    public Optional<ProgrammableTokenCip113> findByPolicyId(String policyId) {
        if (!cip113Configuration.isEnabled()) {
            return Optional.empty();
        }
        return cip113RegistryNodeRepository.findFirstByKeyOrderBySlotDesc(policyId)
                .map(Cip113StorageReaderImpl::toDto);
    }

    @Override
    public Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds) {
        if (!cip113Configuration.isEnabled() || policyIds.isEmpty()) {
            return Map.of();
        }
        return cip113RegistryNodeRepository.findLatestByKeys(policyIds)
                .stream()
                .collect(Collectors.toMap(
                        Cip113RegistryNode::getKey,
                        Cip113StorageReaderImpl::toDto
                ));
    }

    @Override
    public boolean isProgrammableToken(String policyId) {
        return findByPolicyId(policyId).isPresent();
    }

    static ProgrammableTokenCip113 toDto(Cip113RegistryNode entity) {
        String transferLogic = entity.getTransferLogicScript();
        String thirdParty = entity.getThirdPartyTransferLogicScript();
        String globalState = entity.getGlobalStatePolicyId();
        boolean transferLogicAbsent = transferLogic == null || transferLogic.isEmpty();
        boolean thirdPartyAbsent = thirdParty == null || thirdParty.isEmpty();
        return new ProgrammableTokenCip113(
                transferLogicAbsent ? null : transferLogic,
                transferLogicAbsent ? null : entity.getTransferLogicScriptType(),
                thirdPartyAbsent ? null : thirdParty,
                thirdPartyAbsent ? null : entity.getThirdPartyTransferLogicScriptType(),
                (globalState == null || globalState.isEmpty()) ? null : globalState
        );
    }
}
