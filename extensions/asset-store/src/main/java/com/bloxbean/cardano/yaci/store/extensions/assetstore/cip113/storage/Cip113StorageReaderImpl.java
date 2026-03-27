package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.model.ProgrammableTokenCip113;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Cip113StorageReaderImpl implements Cip113StorageReader {

    private final Cip113RegistryService cip113RegistryService;

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
}
