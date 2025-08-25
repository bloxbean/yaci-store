package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

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
public class PythonScriptStorePlugin<T> extends GraalPolyglotScriptStorePlugin<T> {
    private final static String PYTHON = "python";

    private String venvPath;

    public PythonScriptStorePlugin(Engine engine,
                                   PluginDef pluginDef,
                                   PluginType pluginType,
                                   String venvPath,
                                   PluginStateService pluginStateService,
                                   VariableProviderFactory variableProviderFactory,
                                   ContextProvider contextProvider,
                                   GlobalScriptContextRegistry globalScriptContextRegistry) {
        super(engine, pluginDef, pluginType, pluginStateService, variableProviderFactory, globalScriptContextRegistry, contextProvider);
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
            if (log.isTraceEnabled())
                log.trace("Using Python virtual environment at: {}", venvPath);
        } else {
            if (log.isTraceEnabled())
                log.trace("No Python virtual environment specified >>>>>>>>>>>>");
        }

//        cb.option("python.IsolateNativeModules", "true");

        cb.allowExperimentalOptions(true); //TODO
    }

    public String wrapInFunction(String script, String fnName) {
        if (getPluginType() == PluginType.SCHEDULER) {
            StringBuilder sb = new StringBuilder();
            sb.append("def ").append(fnName).append("():\n");
            for (String line : script.split("\\R")) {
                sb.append("    ").append(line).append('\n');
            }
            return sb.toString();
        } else {
            var argName = "items";
            if (getPluginType() == PluginType.EVENT_HANDLER)
                argName = "event";

            StringBuilder sb = new StringBuilder();
            sb.append("def ").append(fnName).append("(" + argName + "):\n");
            for (String line : script.split("\\R")) {
                sb.append("    ").append(line).append('\n');
            }
            return sb.toString();
        }
    }
}
