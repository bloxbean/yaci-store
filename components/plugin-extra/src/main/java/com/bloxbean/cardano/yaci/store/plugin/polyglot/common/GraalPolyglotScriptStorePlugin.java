package com.bloxbean.cardano.yaci.store.plugin.polyglot.common;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.common.plugin.ScriptRef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextProvider;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public abstract class  GraalPolyglotScriptStorePlugin<T> implements InitPlugin<T>, FilterPlugin<T>, PreActionPlugin<T>, PostActionPlugin<T>, EventHandlerPlugin<T> {
    public static final String INIT_VALUE_CACHE_KEY = "__init_plugin__init_value";
    private final String name;
    private final PluginDef pluginDef;
    private final PluginType pluginType;
    private final Engine engine;
    private final PluginCacheService cacheService;
    private final VariableProviderFactory variableProviderFactory;
    private final GlobalScriptContextRegistry globalScriptContextRegistry;
    private final ContextProvider contextProvider;

    private String functionName;
    protected Source source;

    protected ScriptRef scriptRef;

    public GraalPolyglotScriptStorePlugin(Engine engine,
                                          PluginDef pluginDef,
                                          PluginType pluginType,
                                          PluginCacheService pluginCacheService,
                                          VariableProviderFactory variableProviderFactory,
                                          GlobalScriptContextRegistry globalScriptContextRegistry,
                                          ContextProvider contextProvider) {
        this.engine = engine;
        this.name = pluginDef.getName();
        this.pluginDef = pluginDef;
        this.pluginType = pluginType;
        this.cacheService = pluginCacheService;
        this.variableProviderFactory = variableProviderFactory;
        this.contextProvider = contextProvider;
        this.globalScriptContextRegistry = globalScriptContextRegistry;

        if (pluginDef.getExpression() != null) {
            throw new IllegalArgumentException(String.format("Expression is not supported in %s plugin. Use script or inline-script", language()));
        }

        if (pluginDef.getInlineScript() == null
                && (pluginDef.getScript() == null || (pluginDef.getScript().getFile() == null && pluginDef.getScript().getId() == null))) {
            throw new IllegalArgumentException("Inline script or script file cannot be null or empty " + pluginDef);
        }

        if (pluginDef.getInlineScript() != null) {
            if (pluginType != PluginType.INIT) { //wrap in a function only if it's not an init plugin
                String wrapped = wrapInFunction(pluginDef.getInlineScript(), "__filter");
                this.functionName = "__filter";

                source = Source.newBuilder(language(), wrapped, pluginDef.getName()).buildLiteral();
            } else {
                source = Source.newBuilder(language(), pluginDef.getInlineScript(), pluginDef.getName() + "_init").buildLiteral();
            }

        } else if (pluginDef.getScript() != null && pluginDef.getScript().getFile() != null) {
            this.functionName = pluginDef.getScript().getFunction();

            String file = pluginDef.getScript().getFile();
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Script file cannot be null or empty " + pluginDef);
            }

            String code;
            try {
                code = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(file)));
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to load script from file: " + file, e);
            }

            if (this.functionName == null && pluginType != PluginType.INIT) {
                this.functionName = "__filter";
                code = wrapInFunction(code, this.functionName);
            }

            source = Source.newBuilder(language(), code, file).buildLiteral();
        } else if (pluginDef.getScript() != null && pluginDef.getScript().getId() != null) { //Pointing to global ref script
            this.functionName = pluginDef.getScript().getFunction();
            scriptRef = this.globalScriptContextRegistry.getScriptRef(pluginDef.getScript().getId());
            if (scriptRef == null) {
                throw new IllegalArgumentException("No script found with id: " + pluginDef.getScript().getId());
            }

            log.info("Setting global script context for plugin: {} with id: {}", name, pluginDef.getScript().getId());
        } else {
            throw new IllegalArgumentException("No inline-script or script file/id found in plugin definition: " + pluginDef);
        }
    }

    @Override
    public void initPlugin() {
        var ctx = createContext();
        setCommonVariables(ctx);

        ctx.eval(source);

        Value __init = ctx.getBindings(language()).getMember("__init");

        Value initValue = null;
        if (__init != null) {
            initValue = __init.execute();
        }

        //TODO check if we need to close the context
        if (initValue != null) {

            var global = cacheService.global();
            if (global != null) {
                global.put(language() + INIT_VALUE_CACHE_KEY, initValue);
            }
        }
    }

    @Override
    public Collection<T> filter(Collection<T> items) {
        Context ctx = null;
        try {
            if (scriptRef == null) {
                ctx = createContext();
                ctx.eval(source);
                setCommonVariables(ctx);
            } else {
                if (scriptRef.isEnablePool()) {
                    ctx = contextProvider.getPool().borrowObject(scriptRef.getId());
                } else {
                    ctx = globalScriptContextRegistry.getScriptContext(language(), scriptRef.getId());
                }
            }

            synchronized (ctx) {
                ctx.enter();
                try {
                    Value filterFn = ctx.getBindings(language()).getMember(functionName);
                    if (filterFn == null || !filterFn.canExecute()) {
                        throw new IllegalArgumentException("Function not found: " + functionName);
                    }
                    filterFn.execute(items);
                    Value result = filterFn.execute(items);

                    var resultProxy = result.as(Collection.class);

                    if (resultProxy == null) {
                        throw new IllegalArgumentException("Filter function must return a collection of items");
                    }

                    Collection<T> resultList = new ArrayList<>(resultProxy);
                    return resultList;
                } finally {
                    ctx.leave();
                    if (scriptRef == null && ctx != null) {
                        ctx.close(); //close only if we created a new context
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error executing filter function: {}", functionName, e);
            throw new RuntimeException("Error executing filter function: " + functionName, e);
        } finally {
            if (scriptRef != null && scriptRef.isEnablePool()) {
                contextProvider.getPool().returnObject(scriptRef.getId(), ctx);
            }
        }
    }


    @Override
    public void postAction(Collection<T> items) {
        invoke(items);
    }

    private void invoke(Object arg) {
        Context ctx = null;
        try {
            if (scriptRef == null) {
                ctx = createContext();
                ctx.eval(source);
                setCommonVariables(ctx);
            } else {
                if (scriptRef.isEnablePool()) {
                    ctx = contextProvider.getPool().borrowObject(scriptRef.getId());
                } else {
                    ctx = globalScriptContextRegistry.getScriptContext(language(), scriptRef.getId());
                }
            }

            synchronized (ctx) {
                ctx.enter();
                try {
                    Value fn = ctx.getBindings(language()).getMember(functionName);
                    if (fn == null || !fn.canExecute()) {
                        throw new IllegalArgumentException("Function not found: " + functionName);
                    }
                    fn.execute(arg);
                } finally {
                    ctx.leave();
                    if (scriptRef == null && ctx != null) {
                        ctx.close(); //close only if we created a new context
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error executing action function: {}", functionName, e);
            throw new RuntimeException("Error executing action function: " + functionName, e);
        } finally {
            if (scriptRef != null && scriptRef.isEnablePool()) {
                contextProvider.getPool().returnObject(scriptRef.getId(), ctx);
            }
        }
    }

    @Override
    public void preAction(Collection<T> items) {
       invoke(items);
    }

    @Override
    public void handleEvent(Object event) {
       invoke(event);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PluginDef getPluginDef() {
        return pluginDef;
    }

    @Override
    public PluginType getPluginType() {
        return pluginType;
    }

    protected Context createContext() {
        var cb = Context.newBuilder(language())
                .allowAllAccess(true)
                .allowIO(IOAccess.newBuilder().allowHostFileAccess(true).build())
                .allowHostAccess(HostAccess.ALL)
                .engine(engine);

        preCreateContext(cb);

        return cb.build();
    }

    private void setCommonVariables(Context ctx) {
        var binding = ctx.getBindings(language());

        var variables = variableProviderFactory != null? variableProviderFactory.getVariables(): null;
        if (variables != null) {
            variables.entrySet()
                    .stream()
                    .forEach(entry -> {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        log.info("Setting variable {} = {}", key, value);

                        if (!binding.hasMember(key))
                            binding.putMember(key, value);
                    });
        }

        binding.putMember("cache", cacheService.forPlugin(name));

        var globalCache = cacheService.global();
        //TODO -- review if we need this or any perfomance issue
        if (globalCache != null) {
            Value __initVal = (Value)globalCache.get(language() + INIT_VALUE_CACHE_KEY);
            if (__initVal != null) {
                binding.putMember("__init", __initVal);
            }
        }

    }

    public abstract String language();
    protected abstract void preCreateContext(Context.Builder cb);
    protected abstract String wrapInFunction(String script, String fnName);
}
