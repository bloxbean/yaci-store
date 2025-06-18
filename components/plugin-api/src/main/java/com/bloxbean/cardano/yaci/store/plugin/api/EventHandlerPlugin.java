package com.bloxbean.cardano.yaci.store.plugin.api;

/**
 * Interface for event handler plugins that can handle published events.
 *
 * @param <T>
 */
public interface EventHandlerPlugin<T> extends IPlugin<T> {
    void handleEvent(Object event);
}
