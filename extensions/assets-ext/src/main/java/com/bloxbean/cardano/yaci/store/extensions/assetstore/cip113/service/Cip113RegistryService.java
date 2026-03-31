package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.repository.Cip113RegistryNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113RegistryService {

    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final Cip113Configuration cip113Configuration;

    /**
     * Look up the latest CIP-113 registry node for a given policy ID.
     */
    public Optional<ProgrammableTokenCip113> findByPolicyId(String policyId) {
        if (!cip113Configuration.isEnabled()) {
            return Optional.empty();
        }

        return cip113RegistryNodeRepository.findFirstByPolicyIdOrderBySlotDesc(policyId)
                .map(Cip113RegistryService::toDto);
    }

    /**
     * Batch lookup of CIP-113 registry nodes for multiple policy IDs.
     * Returns a map of policyId to ProgrammableTokenCip113 for policies that are registered.
     */
    public Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds) {
        if (!cip113Configuration.isEnabled() || policyIds.isEmpty()) {
            return Map.of();
        }

        return cip113RegistryNodeRepository.findLatestByPolicyIds(policyIds)
                .stream()
                .collect(Collectors.toMap(
                        Cip113RegistryNode::getPolicyId,
                        Cip113RegistryService::toDto
                ));
    }

    /**
     * Check if a UTxO contains a CIP-113 registry node NFT.
     */
    public boolean containsRegistryNode(AddressUtxo utxo) {
        if (!cip113Configuration.isEnabled()) {
            return false;
        }

        return utxo.getAmounts().stream()
                .anyMatch(amt -> amt.getQuantity().equals(BigInteger.ONE)
                        && cip113Configuration.isMonitoredPolicyId(amt.getPolicyId()));
    }

    private static ProgrammableTokenCip113 toDto(Cip113RegistryNode entity) {
        String globalState = entity.getGlobalStatePolicyId();
        return new ProgrammableTokenCip113(
                entity.getTransferLogicScript(),
                entity.getThirdPartyTransferLogicScript(),
                (globalState == null || globalState.isEmpty()) ? null : globalState
        );
    }

}
