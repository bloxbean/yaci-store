package com.bloxbean.cardano.yaci.store.script;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.script.storage.*;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.*;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper.JpaScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.JpaDatumRepository;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.JpaScriptRepository;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.JpaTxScriptRepository;
import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(basePackages = "com.bloxbean.cardano.yaci.store.script")
@ComponentScan(basePackages = "com.bloxbean.cardano.yaci.store.script")
@EnableJpaRepositories(basePackages = "com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.*")
@ConditionalOnProperty(prefix = "store.script", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScriptStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TxScriptStorage txScriptStorage(JpaTxScriptRepository jpaTxScriptRepository, JpaScriptMapper jpaScriptMapper) {
        return new JpaTxScriptStorage(jpaTxScriptRepository, jpaScriptMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatumStorage datumStorage(JpaDatumRepository jpaDatumRepository, DSLContext dslContext, ParallelExecutor executorHelper,
                                     StoreProperties storeProperties, PlatformTransactionManager platformTransactionManager) {
        return new JpaDatumStorage(jpaDatumRepository, dslContext, executorHelper, storeProperties, platformTransactionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScriptStorage scriptStorage(JpaScriptRepository jpaScriptRepository, JpaScriptMapper jpaScriptMapper, DSLContext dslContext,
                                       ParallelExecutor executorHelper, StoreProperties storeProperties, PlatformTransactionManager platformTransactionManager) {
        return new JpaScriptStorage(jpaScriptRepository, jpaScriptMapper, dslContext, executorHelper, storeProperties, platformTransactionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TxScriptStorageReader txScriptStorageReader(JpaTxScriptRepository jpaTxScriptRepository, JpaScriptMapper jpaScriptMapper) {
        return new JpaTxScriptStorageReader(jpaTxScriptRepository, jpaScriptMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatumStorageReader datumStorageReader(JpaDatumRepository jpaDatumRepository) {
        return new JpaDatumStorageReader(jpaDatumRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScriptStorageReader scriptStorageReader(JpaScriptRepository jpaScriptRepository, JpaScriptMapper jpaScriptMapper) {
        return new JpaScriptStorageReader(jpaScriptRepository, jpaScriptMapper);
    }
}
