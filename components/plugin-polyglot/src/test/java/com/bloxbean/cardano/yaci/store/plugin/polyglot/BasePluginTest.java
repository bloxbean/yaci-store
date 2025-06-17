package com.bloxbean.cardano.yaci.store.plugin.polyglot;

import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.http.PluginHttpClient;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GlobalScriptContextRegistry;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextProvider;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.PolyglotContextPoolFactory;
import com.bloxbean.cardano.yaci.store.plugin.util.Locker;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import com.bloxbean.cardano.yaci.store.plugin.variables.DefaultVariableProvider;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class BasePluginTest {

    @Mock
    private Environment env;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private PluginHttpClient pluginHttpClient;
    @Mock
    private Locker locker;

    @InjectMocks
    protected PluginContextUtil pluginContextUtil;

    protected PluginStateConfig pluginCacheConfig;
    protected PluginStateService pluginCacheService;
    protected VariableProviderFactory variableProviderFactory;
    protected ContextProvider contextProvider;
    protected GlobalScriptContextRegistry globalScriptContextRegistry = new GlobalScriptContextRegistry();

    @BeforeEach
    public void setup() {
        pluginCacheConfig = new PluginStateConfig();
        pluginCacheService = new PluginStateService(pluginCacheConfig.globalState(),
                pluginCacheConfig.pluginStates());

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
