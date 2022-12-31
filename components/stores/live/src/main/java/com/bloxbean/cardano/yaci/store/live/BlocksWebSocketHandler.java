package com.bloxbean.cardano.yaci.store.live;

import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.live.cache.BlockCache;
import com.bloxbean.cardano.yaci.store.live.dto.BlockData;
import com.bloxbean.cardano.yaci.store.live.dto.MempoolTxs;
import com.bloxbean.cardano.yaci.store.live.dto.OnJoinData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class BlocksWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private BlockCache blockCache;

    private List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) {
        log.error("error occured at sender " + session, throwable);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (log.isDebugEnabled())
            log.info("Connection Established --- " + session);
        OnJoinData onJoinData = OnJoinData.builder()
                .blocks(blockCache.getBlocks())
                .build();

        TextMessage content = new TextMessage(JsonUtil.getPrettyJson(onJoinData));
        session.sendMessage(content);
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    public void broadcastBlockData(BlockData blockData) throws IOException {
        TextMessage content = new TextMessage(JsonUtil.getPrettyJson(blockData));
        for (WebSocketSession session: sessions) {
            session.sendMessage(content);
        }
    }

    public void broadcastMempoolTxs(MempoolTxs mempoolTxs) throws IOException {
        TextMessage content = new TextMessage(JsonUtil.getPrettyJson(mempoolTxs));
        for (WebSocketSession session: sessions) {
            session.sendMessage(content);
        }
    }
}

