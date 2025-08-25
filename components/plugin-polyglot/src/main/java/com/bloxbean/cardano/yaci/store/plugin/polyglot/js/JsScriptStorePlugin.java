package com.bloxbean.cardano.yaci.store.plugin.polyglot.js;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextProvider;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GlobalScriptContextRegistry;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GraalPolyglotScriptStorePlugin;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

@Slf4j
public class JsScriptStorePlugin<T> extends GraalPolyglotScriptStorePlugin<T> {
    private final static String LANGUAGE = "js";

    public JsScriptStorePlugin(Engine engine,
                               PluginDef pluginDef,
                               PluginType pluginType,
                               PluginStateService pluginCacheService,
                               VariableProviderFactory variableProviderFactory,
                               ContextProvider contextProvider,
                               GlobalScriptContextRegistry globalScriptContextRegistry) {
        super(engine, pluginDef, pluginType, pluginCacheService, variableProviderFactory, globalScriptContextRegistry, contextProvider);
    }

    @Override
    public String language() {
        return LANGUAGE;
    }

    @Override
    protected void preCreateContext(Context.Builder cb) {

    }

    public String wrapInFunction(String script, String fnName) {
        if (getPluginType() == PluginType.SCHEDULER) {
            StringBuilder sb = new StringBuilder();
            sb.append("function ").append(fnName).append("() {\n");
            for (String line : script.split("\\R")) {
                sb.append("    ").append(line).append('\n');
            }
            sb.append("}\n");
            log.debug(sb.toString());
            return sb.toString();
        } else {
            var argName = "items";
            if (getPluginType() == PluginType.EVENT_HANDLER)
                argName = "event";

            StringBuilder sb = new StringBuilder();
            sb.append("function ").append(fnName).append("(" + argName + ") {\n");
            for (String line : script.split("\\R")) {
                sb.append("    ").append(line).append('\n');
            }
            sb.append("}\n");
            log.debug(sb.toString());
            return sb.toString();
        }
    }

}
