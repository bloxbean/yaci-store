package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GraalPolyglotScriptStorePlugin;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

public class PythonScriptStorePlugin<T> extends GraalPolyglotScriptStorePlugin<T> {
    private final static String PYTHON = "python";

    private String venvPath;

    public PythonScriptStorePlugin(Engine engine, PluginDef pluginDef,
                                   String venvPath,
                                   PluginContextUtil pluginContextUtil,
                                   PluginCacheService pluginCacheService) {
        super(engine, pluginDef, pluginContextUtil, pluginCacheService, false);
        this.venvPath = venvPath;
    }

    public PythonScriptStorePlugin(Engine engine, PluginDef pluginDef,
                                   String venvPath,
                                   PluginContextUtil pluginContextUtil,
                                   PluginCacheService pluginCacheService,
                                   boolean isInitPlugin) {
        super(engine, pluginDef, pluginContextUtil, pluginCacheService, isInitPlugin);
        this.venvPath = venvPath;
    }

    @Override
    public String language() {
        return PYTHON;
    }

    @Override
    protected void preCreateContext(Context.Builder cb) {
        if (venvPath != null && !venvPath.isEmpty()) {
            cb.option("python.Executable", venvPath + "/bin/python")
                    .option("python.ForceImportSite", "true");
        }
    }

    public String wrapInFunction(String script, String fnName) {
        StringBuilder sb = new StringBuilder();
        sb.append("def ").append(fnName).append("(items):\n");
        for (String line : script.split("\\R")) {
            sb.append("    ").append(line).append('\n');
        }
        return sb.toString();
    }
}
