package com.bloxbean.cardano.yaci.store.filter;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class VertxPlugin implements Plugin {
    private final String pluginId;
    protected final Vertx vertx;
    private MessageConsumer<Object> consumer;

    public VertxPlugin(Vertx vertx, String pluginId) {
        this.vertx = vertx;
        this.pluginId = pluginId;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public void initialize() {
        // Register the plugin to a unique Event Bus address
        consumer = vertx.eventBus().consumer("plugin." + pluginId, message -> {
            Object data = message.body();
            CompletableFuture<Object> result = execute(data);
            result.thenAccept(message::reply);
        });
        log.info("Plugin " + pluginId + " initialized and listening on Event Bus address: plugin." + pluginId);
    }

    @Override
    public abstract CompletableFuture<Object> execute(Object data);

    @Override
    public void shutdown() {
        // Unregister the consumer from the Event Bus
        if (consumer != null) {
            consumer.unregister();
        }
       log.info("Plugin " + pluginId + " shut down.");
    }
}
