package com.bloxbean.cardano.yaci.store.protocolparams;

import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.api.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.EpochParamStorageImpl;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.ProtocolParamsProposalStorageImpl;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository.EpochParamRepository;
import com.bloxbean.cardano.yaci.store.protocolparams.storage.impl.jpa.repository.ProtocolParamsProposalRepository;
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
        prefix = "store.protocolparams",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.protocolparams"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.protocolparams"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.protocolparams"})
@EnableTransactionManagement
public class ProtocolParamsStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProtocolParamsProposalStorage protocolParamsProposalStorage(ProtocolParamsProposalRepository protocolParamsProposalRepository,
                                                                       ProtocolParamsMapper protocolParamsMapper) {
        return new ProtocolParamsProposalStorageImpl(protocolParamsProposalRepository, protocolParamsMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public EpochParamStorage epochParamStorage(EpochParamRepository epochParamRepository, ProtocolParamsMapper protocolParamsMapper) {
        return new EpochParamStorageImpl(epochParamRepository, protocolParamsMapper);
    }
}
