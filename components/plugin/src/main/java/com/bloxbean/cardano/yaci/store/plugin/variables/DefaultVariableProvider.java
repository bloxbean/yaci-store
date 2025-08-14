package com.bloxbean.cardano.yaci.store.plugin.variables;

import com.bloxbean.cardano.yaci.store.plugin.api.VariableProvider;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DefaultVariableProvider implements VariableProvider {

    private final PluginContextUtil pluginContextUtil;
    private final PluginStateService stateService;

    private final Map<String, Object> cachedVariables;

    public DefaultVariableProvider(PluginContextUtil pluginContextUtil, PluginStateService stateService) {
        this.pluginContextUtil = pluginContextUtil;
        this.stateService = stateService;

        this.cachedVariables = Map.of(
                "jdbc", pluginContextUtil.getJdbc(),
                "named_jdbc", pluginContextUtil.getNamedJdbc(),
                "rest", pluginContextUtil.getRest(),
                "env", pluginContextUtil.getEnv(),
                "http", pluginContextUtil.getHttp(),
                "locker", pluginContextUtil.getLocker(),
                "global_state", stateService.global()
        );
    }

    @Override
    public Map<String, Object> getVariables() {
        return cachedVariables;
    }
}

