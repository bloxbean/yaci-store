package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        return cip113RegistryNodeRepository.findFirstByPolicyIdOrderBySlotDesc(policyId)
                .map(Cip113StorageReaderImpl::toDto);
    }

    @Override
    public Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds) {
        if (!cip113Configuration.isEnabled() || policyIds.isEmpty()) {
            return Map.of();
        }
        return cip113RegistryNodeRepository.findLatestByPolicyIds(policyIds)
                .stream()
                .collect(Collectors.toMap(
                        Cip113RegistryNode::getPolicyId,
                        Cip113StorageReaderImpl::toDto
                ));
    }

    @Override
    public boolean isProgrammableToken(String policyId) {
        return findByPolicyId(policyId).isPresent();
    }

    @Override
    public Optional<Cip113RegistryNode> findRawByPolicyId(String policyId) {
        return cip113RegistryNodeRepository.findFirstByPolicyIdOrderBySlotDesc(policyId);
    }

    @Override
    public List<String> findAllProgrammableTokenPolicyIds() {
        return cip113RegistryNodeRepository.findDistinctPolicyIds();
    }

    static ProgrammableTokenCip113 toDto(Cip113RegistryNode entity) {
        String transferLogic = entity.getTransferLogicScript();
        String thirdParty = entity.getThirdPartyTransferLogicScript();
        String globalState = entity.getGlobalStatePolicyId();
        return new ProgrammableTokenCip113(
                (transferLogic == null || transferLogic.isEmpty()) ? null : transferLogic,
                (thirdParty == null || thirdParty.isEmpty()) ? null : thirdParty,
                (globalState == null || globalState.isEmpty()) ? null : globalState
        );
    }
}
