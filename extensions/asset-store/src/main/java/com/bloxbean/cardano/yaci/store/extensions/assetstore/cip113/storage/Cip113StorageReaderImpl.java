package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.repository.Cip113RegistryNodeRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Cip113StorageReaderImpl implements Cip113StorageReader {

    private final Cip113RegistryService cip113RegistryService;
    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;

    @Override
    public Optional<ProgrammableTokenCip113> findByPolicyId(String policyId) {
        return cip113RegistryService.findByPolicyId(policyId);
    }

    @Override
    public Map<String, ProgrammableTokenCip113> findByPolicyIds(Collection<String> policyIds) {
        return cip113RegistryService.findByPolicyIds(policyIds);
    }

    @Override
    public boolean isProgrammableToken(String policyId) {
        return cip113RegistryService.findByPolicyId(policyId).isPresent();
    }

    @Override
    public Optional<Cip113RegistryNode> findRawByPolicyId(String policyId) {
        return cip113RegistryNodeRepository.findFirstByPolicyIdOrderBySlotDesc(policyId);
    }

    @Override
    public List<String> findAllProgrammableTokenPolicyIds() {
        return cip113RegistryNodeRepository.findAll().stream()
                .map(Cip113RegistryNode::getPolicyId)
                .distinct()
                .toList();
    }
}
