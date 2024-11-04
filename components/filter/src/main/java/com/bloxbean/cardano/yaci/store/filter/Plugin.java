package com.bloxbean.cardano.yaci.store.filter;

import java.util.concurrent.CompletableFuture;

public interface Plugin {
    /**
     * Returns the unique ID of the plugin.
     *
     * @return plugin ID
     */
    String getPluginId();

    /**
     * Initializes the plugin with required context or configurations.
     */
    void initialize();

    /**
     * Executes the plugin's main function with the provided data.
     *
     * @param data Data to be processed by the plugin.
     * @return Result of the processing, typically as a Future to handle async operations.
     */
    Object execute(Object data);

    /**
     * Shuts down the plugin, performing any necessary cleanup.
     */
    void shutdown();
}

