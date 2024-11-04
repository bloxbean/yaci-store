package com.bloxbean.cardano.yaci.store.filter;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class VertxJsPlugin implements Plugin {

    private final String pluginId;
    private final Vertx vertx;
    private final String verticlePath;
    private MessageConsumer<Object> consumer;

    public VertxJsPlugin(Vertx vertx, String pluginId, String verticlePath) {
        this.vertx = vertx;
        this.pluginId = pluginId;
        this.verticlePath = verticlePath;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public void initialize() {
        Path path = Paths.get(verticlePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("Verticle file not found at " + verticlePath);
        }

//        vertx.deployVerticle(new DemoVerticle());

        vertx.deployVerticle(verticlePath, result -> {
            if (result.succeeded()) {
                System.out.println("JavaScript plugin " + pluginId + " initialized from path: " + verticlePath);
            } else {
                System.err.println("Failed to initialize plugin " + pluginId + ": " + result.cause());
            }
        });
    }


    @Override
    public CompletableFuture<Object> execute(Object data) {
        CompletableFuture<Object> future = new CompletableFuture<>();

        // Send data to the JavaScript plugin via Event Bus
        vertx.eventBus().<Object>request("plugin." + pluginId, data, reply -> {
            if (reply.succeeded()) {
                future.complete(reply.result().body());
            } else {
                future.completeExceptionally(reply.cause());
            }
        });

        return future;
    }

    @Override
    public void shutdown() {
        if (consumer != null) {
            consumer.unregister();
        }
        System.out.println("JavaScript plugin " + pluginId + " shut down.");
    }
}
