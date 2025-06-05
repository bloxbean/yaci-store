package com.bloxbean.cardano.yaci.store.plugin.api;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.common.plugin.ScriptRef;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProvider;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PluginRegistry {
    private final StoreProperties storeProperties;
    private final List<PluginFactory> factories;
    private final VariableProviderFactory variableProviderFactory;

    // filterKey -> List of filter instances
    // filterKey is the <store_name>.<target>.<action> (e.g. "utxo.unspent.save" or "utxo.spent.save")
    private final Map<String, InitPlugin> initPlugins = new ConcurrentHashMap<>();
    private final Map<String, List<FilterPlugin<?>>> filters = new ConcurrentHashMap<>();
    private final Map<String, List<PreActionPlugin<?>>> preActions = new ConcurrentHashMap<>();
    private final Map<String, List<PostActionPlugin<?>>> postActions = new ConcurrentHashMap<>();
    private final Map<String, List<EventHandlerPlugin<?>>> eventHandlers = new ConcurrentHashMap<>();

    public PluginRegistry(StoreProperties storeProperties,
                          List<PluginFactory> factories,
                          VariableProviderFactory variableProviderFactory) {
        this.storeProperties = storeProperties;
        this.factories      = factories;
        this.variableProviderFactory = variableProviderFactory;

        log.info("PluginRegistry created with {} factories", factories);
    }

    @PostConstruct
    private void init() {
        if (!storeProperties.isPluginsEnabled()) {
            log.info("Plugin registry is disabled. Skipping initialization.");
            return;
        }
        log.info("Initializing PluginRegistry...");
        initVariableProviders();
        initGlobalScripts();
        initPluginInitializers();
        initFilterplugins();
        initPreActionPlugins();
        initPostActionPlugins();
        initEventHandlersPlugins();
    }

   private void initVariableProviders() {
        List<Class> variableProviderClasses = storeProperties.getPluginVariableProviders();
        if (variableProviderClasses == null || variableProviderClasses.isEmpty()) {
            return;
        }

        log.info("Initializing variable providers: {}", variableProviderClasses);

        List<VariableProvider> pluginVariableProviders = new ArrayList<>();
        for (Class<?> providerClass : variableProviderClasses) {
            try {
                VariableProvider provider = (VariableProvider) providerClass.getDeclaredConstructor().newInstance();
                pluginVariableProviders.add(provider);
            } catch (Exception e) {
                log.error("Failed to initialize variable provider: " + providerClass.getName(), e);
            }
        }

        log.info("Registered variable providers: {}", pluginVariableProviders);
        variableProviderFactory.setConfiguredVariableProviders(pluginVariableProviders);
   }

    private void initGlobalScripts() {
        List<ScriptRef> scriptRefs = storeProperties.getPluginGlobalScripts();
        if (scriptRefs == null || scriptRefs.isEmpty()) {
            log.info("No global Python script references found in configuration.");
            return;
        }

        for (PluginFactory factory : factories) {
            var langScriptRefs = scriptRefs.stream()
                    .filter(scriptRef -> factory.getLang().equals(scriptRef.getLang()))
                    .toList();

            if (!langScriptRefs.isEmpty()) {
                factory.initGlobalScripts(langScriptRefs);
            }
        }

    }

    private void initPluginInitializers() {
        Map<String, PluginDef> initMap  = storeProperties.getPluginInitializers();

        if (initMap == null || initMap.isEmpty()) {
            log.info("No plugin initializers found in configuration.");
            return;
        }

        for (PluginFactory factory : factories) {
            var plugiDef = initMap.get(factory.getLang());
            if (plugiDef == null)
                continue;

            var initPlugin = factory.createInitPlugin(plugiDef);
            initPlugin.initPlugin();

            initPlugins.put(factory.getLang(), initPlugin);
        }
    }

    private void initFilterplugins() {
        Map<String, List<PluginDef>> defsMap = storeProperties.getFilters();
        if (defsMap == null || defsMap.isEmpty()) {
            log.info("No filter definitions found in configuration.");
            return;
        }

        defsMap.forEach((storeKey, defs) -> {
            List<FilterPlugin<?>> instances = defs.stream()
                    .map(def -> {
                        var factory = factories.stream()
                                .filter(f -> f.getLang().equals(def.getLang()))
                                .findFirst()
                                .orElseThrow(() ->
                                        new IllegalArgumentException("Unknown filter type: " + def.getLang() + ", filter: " + def));
                        FilterPlugin<?> filter = factory.createFilterPlugin(def);
                        log.info("Registered filter {} for filter key '{}'", def.getName(), storeKey);
                        return filter;
                    })
                    .collect(Collectors.toList());

            filters.put(storeKey, Collections.unmodifiableList(instances));
        });
    }

    private void initPreActionPlugins() {
        Map<String, List<PluginDef>> defsMap = storeProperties.getPreActions();
        if (defsMap == null || defsMap.isEmpty()) {
            log.info("No pre-action definitions found in configuration.");
            return;
        }

        defsMap.forEach((storeKey, defs) -> {
            List<PreActionPlugin<?>> instances = defs.stream()
                    .map(def -> {
                        var factory = factories.stream()
                                .filter(f -> f.getLang().equals(def.getLang()))
                                .findFirst()
                                .orElseThrow(() ->
                                        new IllegalArgumentException("Unknown pre-action type: " + def.getLang() + ", pre-action: " + def));
                        PreActionPlugin<?> filter = (PreActionPlugin<?>) factory.createPreActionPlugin(def);
                        log.info("Registered pre-action {} for pre-action key '{}'", def.getName(), storeKey);
                        return filter;
                    })
                    .collect(Collectors.toList());

            preActions.put(storeKey, Collections.unmodifiableList(instances));
        });
    }

    private void initPostActionPlugins() {
        Map<String, List<PluginDef>> defsMap = storeProperties.getPostActions();
        if (defsMap == null || defsMap.isEmpty()) {
            log.info("No post-action definitions found in configuration.");
            return;
        }

        defsMap.forEach((storeKey, defs) -> {
            List<PostActionPlugin<?>> instances = defs.stream()
                    .map(def -> {
                        var factory = factories.stream()
                                .filter(f -> f.getLang().equals(def.getLang()))
                                .findFirst()
                                .orElseThrow(() ->
                                        new IllegalArgumentException("Unknown post-action type: " + def.getLang() + ", post-action: " + def));
                        PostActionPlugin<?> filter = (PostActionPlugin<?>) factory.createPostActionPlugin(def);
                        log.info("Registered post-action {} for post-action key '{}'", def.getName(), storeKey);
                        return filter;
                    })
                    .collect(Collectors.toList());

            postActions.put(storeKey, Collections.unmodifiableList(instances));
        });
    }

    private void initEventHandlersPlugins() {
        Map<String, List<PluginDef>> defsMap = storeProperties.getEventHandlers();
        if (defsMap == null || defsMap.isEmpty()) {
            log.info("No event-handler definitions found in configuration.");
            return;
        }

        defsMap.forEach((key, defs) -> {
            List<EventHandlerPlugin<?>> instances = defs.stream()
                    .map(def -> {
                        var factory = factories.stream()
                                .filter(f -> f.getLang().equals(def.getLang()))
                                .findFirst()
                                .orElseThrow(() ->
                                        new IllegalArgumentException("Unknown event-handler type: " + def.getLang() + ", event-handler: " + def));
                        EventHandlerPlugin<?> filter = (EventHandlerPlugin<?>) factory.createEventHandlerPlugin(def);
                        log.info("Registered event-handler {} for key '{}'", def.getName(), key);
                        return filter;
                    })
                    .collect(Collectors.toList());

            eventHandlers.put(key, Collections.unmodifiableList(instances));
        });
    }

    public List<FilterPlugin<?>> getFilterPlugins(String key) {
        return filters.getOrDefault(key, Collections.emptyList());
    }

    public List<PreActionPlugin<?>> getPreActionPlugins(String key) {
        return preActions.getOrDefault(key, Collections.emptyList());
    }

    public List<PostActionPlugin<?>> getPostActionPlugins(String key) {
        return postActions.getOrDefault(key, Collections.emptyList());
    }

    public List<EventHandlerPlugin<?>> getEventHandlerPlugins(String key) {
        return eventHandlers.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public String toString() {
        return filters +", " + preActions + ", " + postActions;
    }
}
