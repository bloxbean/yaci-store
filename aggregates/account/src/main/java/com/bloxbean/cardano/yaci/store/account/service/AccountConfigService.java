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
    public void upateConfig(String configId, ConfigStatus status, Long block) {
        AccountConfigEntity accountConfigEntity = accountConfigRepository.findById(configId).orElse(null);
        if (accountConfigEntity == null) {
            accountConfigEntity = AccountConfigEntity.builder()
                    .configId(configId)
                    .status(status)
                    .block(block)
                    .build();
        } else {
            accountConfigEntity.setStatus(status);
            accountConfigEntity.setBlock(block);
        }

        accountConfigRepository.save(accountConfigEntity);
    }

    @Transactional
    public Optional<AccountConfigEntity> getConfig(String configId) {
        return accountConfigRepository.findById(configId);
    }
}
