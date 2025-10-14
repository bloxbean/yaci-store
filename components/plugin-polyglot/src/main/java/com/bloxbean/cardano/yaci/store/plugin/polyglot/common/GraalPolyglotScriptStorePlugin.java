package com.bloxbean.cardano.yaci.store.plugin.polyglot.common;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptRef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.common.pool.ContextProvider;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.util.ArrayList;
import java.util.Collection;

@Slf4j
public abstract class  GraalPolyglotScriptStorePlugin<T> implements InitPlugin<T>, FilterPlugin<T>, PreActionPlugin<T>, PostActionPlugin<T>, EventHandlerPlugin<T> {
    public static final String INIT_VALUE_STATE_KEY = "__init_plugin__init_value";
    private final String name;
    private final PluginDef pluginDef;
    private final PluginType pluginType;
    private final Engine engine;
    private final PluginStateService stateService;
    private final VariableProviderFactory variableProviderFactory;
    private final GlobalScriptContextRegistry globalScriptContextRegistry;
    private final ContextProvider contextProvider;

    private String functionName;
    protected Source source;

    protected ScriptRef scriptRef;

    public GraalPolyglotScriptStorePlugin(Engine engine,
                                          PluginDef pluginDef,
                                          PluginType pluginType,
                                          PluginStateService pluginStateService,
                                          VariableProviderFactory variableProviderFactory,
                                          GlobalScriptContextRegistry globalScriptContextRegistry,
                                          ContextProvider contextProvider) {
        this.engine = engine;
        this.name = pluginDef.getName();
        this.pluginDef = pluginDef;
        this.pluginType = pluginType;
        this.stateService = pluginStateService;
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
                String wrapped = wrapInFunction(pluginDef.getInlineScript(), getDefaultFunctionName(pluginType));
                this.functionName = getDefaultFunctionName(pluginType);

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
                this.functionName = getDefaultFunctionName(pluginType);
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

            var global = stateService.global();
            if (global != null) {
                global.put(language() + INIT_VALUE_STATE_KEY, initValue);
            }
        }
    }

    @Override
    public Collection<T> filter(Collection<T> items) {
        return invokeWithReturn(items, PluginType.FILTER);
    }

    @Override
    public Collection<T> preAction(Collection<T> items) {
        return invokeWithReturn(items, PluginType.PRE_ACTION);
    }

    private Collection<T> invokeWithReturn(Collection<T> items, PluginType pluginType) {
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
                    Value targetFn = ctx.getBindings(language()).getMember(functionName);
                    if (targetFn == null || !targetFn.canExecute()) {
                        throw new IllegalArgumentException(pluginType.name() + " Function not found: " + functionName);
                    }
                    targetFn.execute(items);
                    Value result = targetFn.execute(items);

                    var resultProxy = result.as(Collection.class);

                    if (resultProxy == null) {
                        throw new IllegalArgumentException(pluginType.name() + " function must return a collection of items");
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
            log.error("Error executing function: {}", functionName, e);
            throw new RuntimeException("Error executing " + pluginType.name() + " function: " + functionName, e);
        } finally {
            if (scriptRef != null && scriptRef.isEnablePool()) {
                contextProvider.getPool().returnObject(scriptRef.getId(), ctx);
            }
        }
    }


    @Override
    public void postAction(Collection<T> items) {
        invoke(items, PluginType.POST_ACTION);
    }

    @Override
    public void handleEvent(Object event) {
        invoke(event, PluginType.EVENT_HANDLER);
    }

    private void invoke(Object arg, PluginType pluginType) {
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
                        throw new IllegalArgumentException(pluginType.name() + " function not found: " + functionName);
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
            log.error("Error executing function: {}", functionName, e);
            throw new RuntimeException("Error executing " + pluginType.name() + " function: " + functionName, e);
        } finally {
            if (scriptRef != null && scriptRef.isEnablePool()) {
                contextProvider.getPool().returnObject(scriptRef.getId(), ctx);
            }
        }
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
                .allowHostClassLookup(className -> true)
                .allowHostClassLoading(true)
                .allowCreateThread(true)
                .allowCreateProcess(true)
                .allowExperimentalOptions(true)
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

                        if (log.isTraceEnabled())
                            log.trace("Setting variable {} = {}", key, value);

                        if (!binding.hasMember(key))
                            binding.putMember(key, value);
                    });
        }

        binding.putMember("state", stateService.forPlugin(name));

        var globalState = stateService.global();
        //TODO -- review if we need this or any perfomance issue
        if (globalState != null) {
            Value __initVal = (Value)globalState.get(language() + INIT_VALUE_STATE_KEY);
            if (__initVal != null) {
                binding.putMember("__init", __initVal);
            }
        }

    }

    private String getDefaultFunctionName(PluginType pluginType) {
        if (pluginType == PluginType.INIT)
            return "__init__";
        else if (pluginType == PluginType.FILTER)
            return "__filter__";
        else if (pluginType == PluginType.PRE_ACTION)
            return "__preAction__";
        else if (pluginType == PluginType.POST_ACTION)
            return "__postAction__";
        else if (pluginType == PluginType.EVENT_HANDLER)
            return "__eventHandler__";
        else
            return "__defaultFunction__";
    }

    public abstract String language();
    protected abstract void preCreateContext(Context.Builder cb);
    protected abstract String wrapInFunction(String script, String fnName);
}
