package com.bloxbean.cardano.yaci.store.plugin.impl.java;

import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.cache.State;
import com.bloxbean.cardano.yaci.store.plugin.file.PluginFileClient;
import com.bloxbean.cardano.yaci.store.plugin.http.PluginHttpClient;
import com.bloxbean.cardano.yaci.store.plugin.util.Locker;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

/**
 * Default {@link PluginContext} implementation. Built per-plugin by {@link JavaStorePluginFactory}
 * so that {@link #state()} resolves to the correct per-plugin state.
 */
public class DefaultPluginContext implements PluginContext {

    private final String pluginName;
    private final PluginStateService stateService;
    private final VariableProviderFactory variableProviderFactory;
    private final PluginContextUtil pluginContextUtil;
    private final ApplicationContext applicationContext;

    public DefaultPluginContext(String pluginName,
                                PluginStateService stateService,
                                VariableProviderFactory variableProviderFactory,
                                PluginContextUtil pluginContextUtil,
                                ApplicationContext applicationContext) {
        this.pluginName = pluginName;
        this.stateService = stateService;
        this.variableProviderFactory = variableProviderFactory;
        this.pluginContextUtil = pluginContextUtil;
        this.applicationContext = applicationContext;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public State<String, Object> state() {
        return stateService.forPlugin(pluginName);
    }

    @Override
    public State<String, Object> globalState() {
        return stateService.global();
    }

    @Override
    public Map<String, Object> variables() {
        return variableProviderFactory != null ? variableProviderFactory.getVariables() : Collections.emptyMap();
    }

    @Override
    public PluginHttpClient http() {
        return pluginContextUtil.getHttp();
    }

    @Override
    public PluginFileClient files() {
        return pluginContextUtil.getFiles();
    }

    @Override
    public Locker locker() {
        return pluginContextUtil.getLocker();
    }

    @Override
    public JdbcTemplate jdbc() {
        return pluginContextUtil.getJdbc();
    }

    @Override
    public NamedParameterJdbcTemplate namedJdbc() {
        return pluginContextUtil.getNamedJdbc();
    }

    @Override
    public RestTemplate restTemplate() {
        return pluginContextUtil.getRest();
    }

    @Override
    public Environment environment() {
        return pluginContextUtil.getEnv();
    }

    @Override
    public <T> T bean(Class<T> type) {
        return applicationContext.getBean(type);
    }

    @Override
    public Object bean(String name) {
        return applicationContext.getBean(name);
    }

    @Override
    public ApplicationContext applicationContext() {
        return applicationContext;
    }
}
