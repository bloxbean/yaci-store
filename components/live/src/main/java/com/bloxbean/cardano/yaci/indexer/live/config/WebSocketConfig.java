package com.bloxbean.cardano.yaci.indexer.live.config;

import com.bloxbean.cardano.yaci.indexer.live.BlocksWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private BlocksWebSocketHandler socketHandler;

    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("Websocket handler registered !!!");
        registry.addHandler(socketHandler, "/ws/liveblocks").setAllowedOrigins("*");
    }

}
