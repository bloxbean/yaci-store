package com.bloxbean.cardano.yaci.store.snapshot.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Resolves {@code import.handler} names to implementations.
 *
 * <p>Built-in handlers are registered directly. Plugins may contribute more through the standard
 * {@link ServiceLoader} mechanism, but may never replace a built-in name: control-state
 * reconstruction is exactly where a silent override would be most damaging.
 */
public class HandlerRegistry {

    private final Map<String, SnapshotHandler> handlers = new LinkedHashMap<>();

    public HandlerRegistry() {
        register(new CursorTailHandler(), true);
        register(new EraTransitionHandler(), true);
        register(new AdaPotJobsHandler(), true);
        register(new AccountConfigHandler(), true);
        for (SnapshotHandler h : ServiceLoader.load(SnapshotHandler.class)) {
            register(h, false);
        }
    }

    private void register(SnapshotHandler handler, boolean builtIn) {
        SnapshotHandler existing = handlers.get(handler.name());
        if (existing != null) {
            if (!builtIn) {
                throw new IllegalStateException("Handler '" + handler.name() + "' from "
                        + handler.getClass().getName() + " cannot override the built-in "
                        + existing.getClass().getName());
            }
            throw new IllegalStateException("Duplicate built-in handler: " + handler.name());
        }
        handlers.put(handler.name(), handler);
    }

    public SnapshotHandler get(String name) {
        SnapshotHandler h = handlers.get(name);
        if (h == null) {
            throw new IllegalArgumentException("Unknown snapshot handler '" + name + "'. Known: "
                    + handlers.keySet());
        }
        return h;
    }

    public Set<String> names() {
        return handlers.keySet();
    }
}
