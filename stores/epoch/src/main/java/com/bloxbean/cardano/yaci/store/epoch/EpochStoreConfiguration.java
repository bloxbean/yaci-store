package com.bloxbean.cardano.yaci.store.epoch;

import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.local.LocalClientProviderManager;
import com.bloxbean.cardano.yaci.store.epoch.processor.LocalEpochParamsScheduler;
import com.bloxbean.cardano.yaci.store.epoch.service.LocalEpochParamService;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.LocalEpochParamsStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorageReader;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.EpochParamStorageImpl;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.LocalEpochParamsStorageImpl;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.ProtocolParamsProposalStorageImpl;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.ProtocolParamsProposalStorageReaderImpl;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.mapper.ProtocolParamsMapper;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.CostModelRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.EpochParamRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.LocalEpochParamsRepository;
import com.bloxbean.cardano.yaci.store.epoch.storage.impl.repository.ProtocolParamsProposalRepository;
import jakarta.annotation.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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

    @Bean
    @ConditionalOnMissingBean
    public ProtocolParamsProposalStorageReader protocolParamsProposalStorageReader(ProtocolParamsProposalRepository protocolParamsProposalReadRepository,
                                                                            ProtocolParamsMapper protocolParamsMapper) {
        return new ProtocolParamsProposalStorageReaderImpl(protocolParamsProposalReadRepository, protocolParamsMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalEpochParamsStorage localEpochParamsStorage(LocalEpochParamsRepository localProtocolParamsRepository) {
        return new LocalEpochParamsStorageImpl(localProtocolParamsRepository);
    }

    @Bean
    public LocalEpochParamService localEpochParamService(@Nullable LocalClientProviderManager localClientProviderManager,
                                                         @Nullable LocalEpochParamsStorage localProtocolParamsStorage,
                                                         EraService eraService) {
        if (localClientProviderManager != null) {
            return new LocalEpochParamService(localClientProviderManager, localProtocolParamsStorage, eraService);
        } else
            return null;
    }

    @Bean
    @ConditionalOnExpression("'${store.cardano.n2c-node-socket-path:}' != '' || '${store.cardano.n2c-host:}' != ''")
    public LocalEpochParamsScheduler localEpochParamsScheduler(LocalEpochParamService localEpochParamService) {
        return new LocalEpochParamsScheduler(localEpochParamService);
    }
}
