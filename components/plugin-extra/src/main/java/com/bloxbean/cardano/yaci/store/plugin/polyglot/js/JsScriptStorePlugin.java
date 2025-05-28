package com.bloxbean.cardano.yaci.store.plugin.polyglot.js;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GraalPolyglotScriptStorePlugin;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

public class JsScriptStorePlugin<T> extends GraalPolyglotScriptStorePlugin<T> {
    private final static String LANGUAGE = "js";

    public JsScriptStorePlugin(Engine engine, PluginDef pluginDef, PluginContextUtil pluginContextUtil, PluginCacheService pluginCacheService) {
        super(engine, pluginDef, pluginContextUtil, pluginCacheService, false);
    }

    public JsScriptStorePlugin(Engine engine, PluginDef pluginDef, PluginContextUtil pluginContextUtil, PluginCacheService pluginCacheService, boolean isInitPlugin) {
        super(engine, pluginDef, pluginContextUtil, pluginCacheService, isInitPlugin);
    }

    @Override
    public String language() {
        return LANGUAGE;
    }

    @Override
    protected void preCreateContext(Context.Builder cb) {

    }

    public String wrapInFunction(String script, String fnName) {
        StringBuilder sb = new StringBuilder();
        sb.append("function ").append(fnName).append("(items) {\n");
        for (String line : script.split("\\R")) {
            sb.append("    ").append(line).append('\n');
        }
        sb.append("}\n");
        System.out.println(sb.toString());
        return sb.toString();
    }
}
