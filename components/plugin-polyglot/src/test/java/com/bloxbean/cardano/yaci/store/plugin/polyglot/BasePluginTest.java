package com.bloxbean.cardano.yaci.store.plugin.polyglot;

import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GlobalScriptContextRegistry;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextProvider;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.PolyglotContextPoolFactory;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import com.bloxbean.cardano.yaci.store.plugin.variables.DefaultVariableProvider;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

public class BasePluginTest {

    protected PluginCacheConfig pluginCacheConfig;
    protected PluginContextUtil pluginContextUtil;
    protected PluginCacheService pluginCacheService;
    protected VariableProviderFactory variableProviderFactory;
    protected ContextProvider contextProvider;
    protected GlobalScriptContextRegistry globalScriptContextRegistry = new GlobalScriptContextRegistry();

    @BeforeEach
    public void setup() {
        pluginCacheConfig = new PluginCacheConfig();
        pluginCacheService = new PluginCacheService(pluginCacheConfig.globalCache(),
                pluginCacheConfig.pluginCaches());

        pluginContextUtil = new PluginContextUtil(null, null, null);

        variableProviderFactory = new VariableProviderFactory(List.of(new DefaultVariableProvider(pluginContextUtil, pluginCacheService)));

        PolyglotContextPoolFactory factory = new PolyglotContextPoolFactory();

        GenericKeyedObjectPoolConfig<Context> config = new GenericKeyedObjectPoolConfig<>();
        config.setMaxTotalPerKey(30);
        config.setMaxIdlePerKey(20);
        config.setMinIdlePerKey(20);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);

        var polyglotContextPool = new GenericKeyedObjectPool<>(factory, config);
        contextProvider = new ContextProvider(polyglotContextPool);
    }
}
