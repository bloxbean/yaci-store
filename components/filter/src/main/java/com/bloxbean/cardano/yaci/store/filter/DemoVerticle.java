package com.bloxbean.cardano.yaci.store.filter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class DemoVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        System.out.println("DemoVerticle started");
        vertx.eventBus().consumer("plugin.filterPlugin", message -> {
            System.out.println("Received message: " + message.body());
            message.reply("pong");
        });
    }

    @Override
    public void stop() {
        System.out.println("DemoVerticle stopped");
    }
}
