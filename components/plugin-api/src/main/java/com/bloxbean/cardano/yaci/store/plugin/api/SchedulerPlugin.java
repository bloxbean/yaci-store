package com.bloxbean.cardano.yaci.store.plugin.api;

/**
 * Interface for scheduler plugins that can execute scheduled tasks.
 * Variables are directly accessible in the plugin execution context without parameters.
 *
 * @param <T>
 */
public interface SchedulerPlugin<T> extends IPlugin<T> {
    void execute();
}