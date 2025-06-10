package com.bloxbean.cardano.yaci.store.plugin.polyglot;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextProvider;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GlobalScriptContextRegistry;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.PolyglotContextPoolFactory;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.js.JsPolyglotPluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.python.PythonPolyglotPluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.graalvm.polyglot.Context;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PolyglotProperties.class)
@ConditionalOnClass(Context.class)
@ConditionalOnProperty(
        prefix = "store.plugins",
        name = "enabled",
        havingValue = "true"
)
@Slf4j
public class PluginExtraAutoConfig {

    @Value("${store.plugins.python.venv:#{null}}")
    private String pythonVenvPath;

    @Bean
    public PluginFactory JsPluginFactory(PluginContextUtil pluginContextUtil,
                                         PluginCacheService pluginCacheService,
                                         VariableProviderFactory variableProviderFactory,
                                         ContextProvider contextProvider,
                                         GlobalScriptContextRegistry globalScriptContextRegistry) {
        return new JsPolyglotPluginFactory(pluginContextUtil, pluginCacheService, variableProviderFactory,
                contextProvider, globalScriptContextRegistry);
    }

    @Bean
    public PluginFactory pythonPluginFactory(PluginContextUtil pluginContextUtil,
                                             PluginCacheService pluginCacheService,
                                             VariableProviderFactory variableProviderFactory,
                                             ContextProvider contextProvider,
                                             GlobalScriptContextRegistry globalScriptContextRegistry) {
        var pythonFactory = new PythonPolyglotPluginFactory(pluginContextUtil, pluginCacheService, variableProviderFactory,
                contextProvider, globalScriptContextRegistry);
        if (pythonVenvPath != null && !pythonVenvPath.isEmpty()) {
            pythonFactory.setVirtualEnvPath(pythonVenvPath);
        }
        return pythonFactory;
    }

    @Bean
    public GlobalScriptContextRegistry globalScriptContextRegistry() {
        return new GlobalScriptContextRegistry();
    }

    @Bean
    @Qualifier("polyglotContextPool")
    public GenericKeyedObjectPool<String, Context> polyglotContextPool(PolyglotProperties polyglotProperties) {
        PolyglotContextPoolFactory factory = new PolyglotContextPoolFactory();

        log.info("PolyglotContextPool configuration : " + polyglotProperties);
        GenericKeyedObjectPoolConfig<Context> config = new GenericKeyedObjectPoolConfig<>();
        config.setMaxTotalPerKey(polyglotProperties.getPoolMaxTotalPerKey());
        config.setMaxIdlePerKey(polyglotProperties.getPoolMaxIdlePerKey());
        config.setMinIdlePerKey(polyglotProperties.getPoolMinIdlePerKey());
        config.setTestOnBorrow(polyglotProperties.isPoolTestOnBorrow());
        config.setTestOnReturn(polyglotProperties.isPoolTestOnReturn());

        return new GenericKeyedObjectPool<>(factory, config);
    }

    @Bean
    public ContextProvider contextProvider(@Qualifier("polyglotContextPool") GenericKeyedObjectPool<String, Context> polyglotContextPool) {
        return new ContextProvider(polyglotContextPool);
    }
}

