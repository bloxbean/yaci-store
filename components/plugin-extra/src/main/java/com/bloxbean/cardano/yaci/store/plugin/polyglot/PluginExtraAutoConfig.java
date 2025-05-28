package com.bloxbean.cardano.yaci.store.plugin.polyglot;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.js.JsPolyglotPluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.python.PythonPolyglotPluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import org.graalvm.polyglot.Context;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(Context.class)
@ConditionalOnProperty(
        prefix = "store.plugins",
        name = "enabled",
        havingValue = "true"
)
public class PluginExtraAutoConfig {
    @Bean
    public PluginFactory JsPluginFactory(PluginContextUtil pluginContextUtil, PluginCacheService pluginCacheService) {
        return new JsPolyglotPluginFactory(pluginContextUtil, pluginCacheService);
    }

    @Bean PluginFactory pythonPluginFactory(PluginContextUtil pluginContextUtil, PluginCacheService pluginCacheService) {
        return new PythonPolyglotPluginFactory(pluginContextUtil, pluginCacheService);
    }
}

