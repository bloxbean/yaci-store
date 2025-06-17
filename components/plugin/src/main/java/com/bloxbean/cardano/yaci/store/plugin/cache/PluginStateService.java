package com.bloxbean.cardano.yaci.store.plugin.cache;

import org.springframework.stereotype.Service;

@Service
public class PluginStateService {
    private final State<String, Object> globalState;
    private final State<String, State<String, Object>> pluginStates;

    public PluginStateService(State<String, Object> globalState,
                              State<String, State<String, Object>> pluginStates
    ) {
        this.globalState = globalState;
        this.pluginStates = pluginStates;
    }

    /**
     * @return the one global state shared by all plugins
     */
    public State<String, Object> global() {
        return globalState;
    }

    /**
     * @param pluginKey a unique identifier for your plugin (e.g. "utxo.save", "metadata.save")
     * @return a dedicated state for that plugin
     */
    public State<String, Object> forPlugin(String pluginKey) {
        return pluginStates.computeIfAbsent(pluginKey, k -> new ConcurrentMapState<>());
    }
}

