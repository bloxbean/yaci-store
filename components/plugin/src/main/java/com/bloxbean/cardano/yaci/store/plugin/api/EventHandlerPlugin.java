package com.bloxbean.cardano.yaci.store.plugin.api;

public interface EventHandlerPlugin<T> extends IPlugin<T> {
    void handleEvent(Object event);
}
