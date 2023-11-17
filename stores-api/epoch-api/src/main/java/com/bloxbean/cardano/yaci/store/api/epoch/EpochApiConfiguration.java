package com.bloxbean.cardano.yaci.store.api.epoch;

import com.bloxbean.cardano.yaci.store.api.epoch.storage.ProtocolParamsProposalReader;
import com.bloxbean.cardano.yaci.store.api.epoch.storage.impl.ProtocolParamsProposalReaderImpl;
import com.bloxbean.cardano.yaci.store.api.epoch.storage.impl.repository.ProtocolParamsProposalReadRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.mapper.ProtocolParamsMapper;
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
        name = "api-enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.epoch"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.api.epoch.storage"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.api.epoch.storage"})
@EnableTransactionManagement
public class EpochApiConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProtocolParamsProposalReader protocolParamsProposalReader(ProtocolParamsProposalReadRepository protocolParamsProposalReadRepository,
                                                                     ProtocolParamsMapper protocolParamsMapper) {
        return new ProtocolParamsProposalReaderImpl(protocolParamsProposalReadRepository, protocolParamsMapper);
    }

}
