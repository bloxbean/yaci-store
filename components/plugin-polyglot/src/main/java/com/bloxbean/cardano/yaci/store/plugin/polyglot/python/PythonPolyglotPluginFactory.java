package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.common.plugin.ScriptRef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextProvider;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextSupplier;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.GlobalScriptContextRegistry;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.PolyglotContextPoolFactory;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.util.List;

@Slf4j
public class PythonPolyglotPluginFactory implements PluginFactory {
    private PluginContextUtil pluginContextUtil;
    private PluginCacheService pluginCacheService;
    private VariableProviderFactory variableProviderFactory;
    private ContextProvider contextProvider;
    private GlobalScriptContextRegistry globalScriptContextRegistry;

    private Engine engine;
    private String venvPath;

    public PythonPolyglotPluginFactory(PluginContextUtil pluginContextUtil,
                                       PluginCacheService pluginCacheService,
                                       VariableProviderFactory variableProviderFactory,
                                       ContextProvider contextProvider,
                                       GlobalScriptContextRegistry globalScriptContextRegistry) {
        this.pluginContextUtil = pluginContextUtil;
        this.pluginCacheService = pluginCacheService;
        this.variableProviderFactory = variableProviderFactory;
        this.contextProvider = contextProvider;
        this.globalScriptContextRegistry = globalScriptContextRegistry;

        this.engine = Engine
                .newBuilder(getLang())
                .allowExperimentalOptions(true)
                .build();

        log.info("Python Polyglot Plugin Factory created with GraalVM engine >>");
    }

    @Override
    public String getLang() {
        return "python";
    }

    @Override
    public void initGlobalScripts(List<ScriptRef> scriptRefs) {
        if (scriptRefs == null || scriptRefs.isEmpty())
            return;

        log.info("Initializing global Python scripts... " + scriptRefs);

        for (ScriptRef scriptRef : scriptRefs) {
            if (!getLang().equals(scriptRef.getLang())) {
                throw new IllegalArgumentException("Script type mismatch. Expected: " + getLang() + ", but found: " + scriptRef.getLang());
            }

            if (scriptRef.getId() == null || scriptRef.getId().isEmpty()) {
                throw new IllegalArgumentException("Script ID cannot be null or empty.");
            }

            if (scriptRef.getFile() == null || scriptRef.getFile().isEmpty()) {
                throw new IllegalArgumentException("Script file cannot be null or empty.");
            }

            String code;
            try {
                code = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(scriptRef.getFile())));
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to load script from file: " + scriptRef.getFile(), e);
            }

            Source source = Source.newBuilder(getLang(), code, scriptRef.getId()).buildLiteral();

            log.info("Initializing global Python script: {} with file: {}", scriptRef.getId(), scriptRef.getFile());

            ContextSupplier contextSupplier = () -> {
                log.info("Creating new Python context for script: {}", scriptRef.getId());

                var cb = Context.newBuilder(getLang())
                        .allowAllAccess(true)
                        .allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build())
                        .allowHostAccess(HostAccess.ALL)
                        .allowCreateThread(true)
                        .allowCreateProcess(true)
                        .allowExperimentalOptions(true)
                        .engine(engine);

               //  cb.option("python.IsolateNativeModules", "true");

                if (venvPath != null && !venvPath.isEmpty()) {
                    cb.option("python.Executable", venvPath + "/bin/python")
                            .option("python.ForceImportSite", "true");
                }

                var ctx = cb.build();

                ctx.eval(source);

                ctx.getBindings(getLang()).putMember("util", pluginContextUtil);
                ctx.getBindings(getLang()).putMember("global_cache", pluginCacheService.global());

                return ctx;
            };

            globalScriptContextRegistry.addScriptRef(scriptRef.getId(), scriptRef);
            if (scriptRef.isEnablePool()) {
                log.info("Adding context supplier for script: {} with pooling enabled", scriptRef.getId());
                PolyglotContextPoolFactory.addContextSupplier(scriptRef.getId(), contextSupplier);
            } else {
                log.info("Adding script context for script: {} without pooling", scriptRef.getId());
                globalScriptContextRegistry.addScriptContext(getLang(), scriptRef.getId(), contextSupplier.createContext());
            }
        }

    }

    public void setVirtualEnvPath(String venvPath) {
        this.venvPath = venvPath;
    }

    @Override
    public <T> InitPlugin<T> createInitPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for init plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null) {
            return new PythonScriptStorePlugin<>(engine, def, PluginType.INIT, venvPath, pluginCacheService,
                    variableProviderFactory, contextProvider, globalScriptContextRegistry);
        } else
            throw new IllegalArgumentException("No script or inline-script found in init python plugin: " + def);
    }

    @Override
    public <T> FilterPlugin<T> createFilterPlugin(PluginDef def) {
        if (def.getExpression() != null)
            return new PythonScriptStorePlugin<>(engine, def, PluginType.FILTER, venvPath, pluginCacheService,
                    variableProviderFactory, contextProvider, globalScriptContextRegistry);
        else if (def.getInlineScript() != null || def.getScript() != null) {
            return new PythonScriptStorePlugin<>(engine, def, PluginType.FILTER, venvPath, pluginCacheService,
                    variableProviderFactory, contextProvider, globalScriptContextRegistry);
        } else
            throw new IllegalArgumentException("No expression or script found in filter definition for python plugin: " + def);
    }

    @Override
    public <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for post-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null) {
            return new PythonScriptStorePlugin<>(engine, def, PluginType.POST_ACTION, venvPath, pluginCacheService,
                    variableProviderFactory, contextProvider,  globalScriptContextRegistry);
        } else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for python plugin: " + def);
    }

    @Override
    public <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for pre-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null) {
            return new PythonScriptStorePlugin<>(engine, def, PluginType.PRE_ACTION, venvPath, pluginCacheService,
                    variableProviderFactory, contextProvider, globalScriptContextRegistry);
        } else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for python plugin: " + def);
    }

    @Override
    public <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for event-handler plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null) {
            return new PythonScriptStorePlugin<>(engine, def, PluginType.EVENT_HANDLER, venvPath, pluginCacheService,
                    variableProviderFactory, contextProvider, globalScriptContextRegistry);
        } else
            throw new IllegalArgumentException("No script or inline-script found in event-handler definition for python plugin: " + def);
    }

}
