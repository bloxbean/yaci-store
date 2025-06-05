package com.bloxbean.cardano.yaci.store.plugin.variables;

import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DefaultVariableProvider implements VariableProvider {
    private final PluginContextUtil pluginContextUtil;
    private final PluginCacheService cacheService;

    @Override
    public Map<String, Object> getVariables() {
        return Map.of("util", pluginContextUtil,
                "global_cache", cacheService);
    }
}
