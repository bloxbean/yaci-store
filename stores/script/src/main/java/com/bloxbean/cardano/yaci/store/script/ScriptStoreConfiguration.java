package com.bloxbean.cardano.yaci.store.script;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.executor.ParallelExecutor;
import com.bloxbean.cardano.yaci.store.script.storage.*;
import com.bloxbean.cardano.yaci.store.script.storage.impl.*;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.DatumRepository;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.ScriptRepository;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.TxScriptRepository;
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

@ConditionalOnProperty(
        prefix = "store.script",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ComponentScan(basePackages = {"com.bloxbean.cardano.yaci.store.script"})
@EnableJpaRepositories( basePackages = {"com.bloxbean.cardano.yaci.store.script"})
@EntityScan(basePackages = {"com.bloxbean.cardano.yaci.store.script"})
@EnableTransactionManagement
public class ScriptStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TxScriptStorage txScriptStorage(TxScriptRepository txScriptRepository, ScriptMapper scriptMapper) {
        return new TxScriptStorageImpl(txScriptRepository, scriptMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatumStorage datumStorage(DatumRepository datumRepository, DSLContext dslContext, ParallelExecutor executorHelper,
                                     StoreProperties storeProperties, PlatformTransactionManager platformTransactionManager) {
        return new DatumStorageImpl(datumRepository, dslContext, executorHelper, storeProperties, platformTransactionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScriptStorage scriptStorage(ScriptRepository scriptRepository, ScriptMapper scriptMapper, DSLContext dslContext,
                                       ParallelExecutor executorHelper, StoreProperties storeProperties, PlatformTransactionManager platformTransactionManager) {
        return new ScriptStorageImpl(scriptRepository, scriptMapper, dslContext, executorHelper, storeProperties, platformTransactionManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public TxScriptStorageReader txScriptStorageReader(TxScriptRepository txScriptRepository, ScriptMapper scriptMapper) {
        return new TxScriptStorageReaderImpl(txScriptRepository, scriptMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public DatumStorageReader datumStorageReader(DatumRepository datumRepository) {
        return new DatumStorageReaderImpl(datumRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public ScriptStorageReader scriptStorageReader(ScriptRepository scriptRepository, ScriptMapper scriptMapper) {
        return new ScriptStorageReaderImpl(scriptRepository, scriptMapper);
    }
}
