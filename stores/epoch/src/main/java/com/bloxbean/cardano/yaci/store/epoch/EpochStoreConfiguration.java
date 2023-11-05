package com.bloxbean.cardano.yaci.store.epoch;

import com.bloxbean.cardano.yaci.store.epoch.storage.api.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.api.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.EpochParamStorageImpl;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.ProtocolParamsProposalStorageImpl;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.repository.CostModelRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.repository.EpochParamRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.repository.ProtocolParamsProposalRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ConditionalOnProperty(
        prefix = "store.epoch",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.epoch"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.epoch"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.epoch"})
@EnableTransactionManagement
public class EpochStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProtocolParamsProposalStorage protocolParamsProposalStorage(ProtocolParamsProposalRepository protocolParamsProposalRepository,
                                                                       ProtocolParamsMapper protocolParamsMapper) {
        return new ProtocolParamsProposalStorageImpl(protocolParamsProposalRepository, protocolParamsMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochParamStorage epochParamStorage(EpochParamRepository epochParamRepository, CostModelRepository costModelRepository,
                                               ProtocolParamsMapper protocolParamsMapper) {
        return new EpochParamStorageImpl(epochParamRepository, costModelRepository, protocolParamsMapper);
    }
}
