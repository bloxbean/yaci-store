package com.bloxbean.cardano.yaci.store.live;

import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.live.cache.BlockCache;
import com.bloxbean.cardano.yaci.store.live.cache.RecentTxCache;
import com.bloxbean.cardano.yaci.store.live.dto.*;
import com.bloxbean.cardano.yaci.store.live.service.InitialDataProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlocksWebSocketHandler extends TextWebSocketHandler {
    private final BlockCache blockCache;
    private final RecentTxCache recentTxCache;
    private final InitialDataProviderService initialDataProvider;

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

        List<RecentTx> recentTxList;
        if (recentTxCache.getRecentTxs().size() == 0) {
            recentTxList = initialDataProvider.getRecentTransactions();
        } else {
            recentTxList = recentTxCache.getRecentTxs();
        }

        List<BlockData> blocks;
        if (blockCache.getBlocks().size() == 0) {
            blocks = initialDataProvider.getRecentBlocks();
        } else {
            blocks = blockCache.getBlocks();
        }

        OnJoinData onJoinData = OnJoinData.builder()
                .blocks(blocks)
                .recentTxs(recentTxList)
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

    public void broadcastRecentTxs(RecentTxs recentTxs) throws IOException{
        TextMessage content = new TextMessage(JsonUtil.getPrettyJson(recentTxs));
        for (WebSocketSession session: sessions) {
            session.sendMessage(content);
        }
    }
}

