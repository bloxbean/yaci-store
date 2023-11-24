package com.bloxbean.cardano.yaci.store.account.service;

import com.bloxbean.cardano.yaci.store.account.storage.impl.model.AccountConfigEntity;
import com.bloxbean.cardano.yaci.store.account.storage.impl.repository.AccountConfigRepository;
import com.bloxbean.cardano.yaci.store.account.util.ConfigStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountConfigService {
    private final AccountConfigRepository accountConfigRepository;

    @Transactional
    public void upateConfig(String configId, ConfigStatus status, Long block, String blockHash, Long slot) {
        AccountConfigEntity accountConfigEntity = accountConfigRepository.findById(configId).orElse(null);
        if (accountConfigEntity == null) {
            accountConfigEntity = AccountConfigEntity.builder()
                    .configId(configId)
                    .status(status)
                    .block(block)
                    .blockHash(blockHash)
                    .slot(slot)
                    .build();
        } else {
            accountConfigEntity.setStatus(status);
            accountConfigEntity.setBlock(block);
            accountConfigEntity.setBlockHash(blockHash);
            accountConfigEntity.setSlot(slot);
        }

        accountConfigRepository.save(accountConfigEntity);
    }

    @Transactional
    public Optional<AccountConfigEntity> getConfig(String configId) {
        return accountConfigRepository.findById(configId);
    }
}
