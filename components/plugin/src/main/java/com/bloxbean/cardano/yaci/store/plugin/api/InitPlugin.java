package com.bloxbean.cardano.yaci.store.plugin.api;

/**
 * Interface for initialization plugins that can perform actions during the initialization phase. Only one init plugin can be registered per
 * plugin type.
 *
 * @param <T> The type of the plugin.
 */
public interface InitPlugin<T> extends IPlugin<T>{
    void init();
}
