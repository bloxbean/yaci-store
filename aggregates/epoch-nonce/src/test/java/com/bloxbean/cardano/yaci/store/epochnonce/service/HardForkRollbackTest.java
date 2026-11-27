package com.bloxbean.cardano.yaci.store.epochnonce.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.EpochConfig;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.BlockFetchService;
import com.bloxbean.cardano.yaci.store.core.service.EpochChangeEventListener;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.storage.api.CursorStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.EraStorage;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.domain.EpochNonce;
import com.bloxbean.cardano.yaci.store.epochnonce.processor.EpochNonceProcessor;
import com.bloxbean.cardano.yaci.store.epochnonce.storage.EpochNonceStorage;
import com.bloxbean.cardano.yaci.store.epochnonce.util.EpochNonceConfig;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ResourceLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class HardForkRollbackTest {
    @Test
    void rollbackAcrossEraPreservesRetainedNonceAndReplay() {
        var rows = new HashMap<Integer, EpochNonce>();
        var seed = EpochNonce.builder().epoch(50).slot(5000L).block(1L).nonce("11".repeat(32))
                .evolvingNonce("22".repeat(32)).candidateNonce("33".repeat(32))
                .labNonce("44".repeat(32)).lastEpochBlockNonce("55".repeat(32)).build();
        rows.put(50, seed);
        var storage = mock(EpochNonceStorage.class);
        when(storage.findByEpoch(anyInt())).thenAnswer(a -> Optional.ofNullable(rows.get(a.getArgument(0))));
        doAnswer(a -> {
            EpochNonce nonce = a.getArgument(0);
            rows.put(nonce.getEpoch(), nonce);
            return null;
        }).when(storage).save(any());
        when(storage.deleteBySlotGreaterThan(anyLong())).thenAnswer(a -> {
            long slot = a.getArgument(0);
            int before = rows.size();
            rows.values().removeIf(n -> n.getSlot() > slot);
            return before - rows.size();
        });
        var blocks = new ArrayList<Block>();
        blocks.add(storedBlock(5010, "aa"));
        blocks.add(storedBlock(5090, "bb"));
        var blockReader = mock(BlockStorageReader.class);
        when(blockReader.findBlocksByEpoch(50)).thenAnswer(a -> new ArrayList<>(blocks));
        var nonceConfig = mock(EpochNonceConfig.class);
        when(nonceConfig.getEpochLength()).thenReturn(100L);
        when(nonceConfig.getStabilityWindow(any())).thenReturn(20L);
        var properties = mock(StoreProperties.class);
        var nonceService = new EpochNonceService(blockReader, storage, nonceConfig, properties,
                mock(ResourceLoader.class), Optional.empty());
        var processor = new EpochNonceProcessor(nonceService);
        var publisher = mock(ApplicationEventPublisher.class);
        var listener = new EpochChangeEventListener(publisher);
        var transitions = new ArrayList<String>();
        doAnswer(a -> {
            Object e = a.getArgument(0);
            if (e instanceof EpochChangeEvent change) {
                transitions.add(change.getPreviousEpoch() + "->" + change.getEpoch());
                listener.onEpochChange(change);
            } else if (e instanceof EpochTransitionCommitEvent commit) {
                processor.handleEpochTransition(commit);
            } else if (e instanceof RollbackEvent rollback) {
                processor.handleRollback(rollback);
            }
            return null;
        }).when(publisher).publishEvent(any(Object.class));
        var cursor = new AtomicReference<>(cursor(5090, Era.Babbage));
        var cursorService = mock(CursorService.class);
        when(cursorService.getCursor()).thenAnswer(a -> Optional.of(cursor.get()));
        doAnswer(a -> {
            cursor.set(cursor(5090, Era.Babbage));
            return null;
        }).when(cursorService).rollback(5090);
        var cursorStorage = mock(CursorStorage.class);
        when(cursorStorage.findByBlockHash(anyLong(), any())).thenReturn(Optional.of(cursor.get()));
        var era = spy(new EraService(mock(EraStorage.class), cursorStorage, mock(EpochConfig.class),
                mock(GenesisConfig.class), properties));
        doAnswer(a -> (int) ((long) a.getArgument(1) / 100)).when(era).getEpochNo(any(), anyLong());
        doAnswer(a -> (int) ((long) a.getArgument(0) % 100)).when(era).getShelleyEpochSlot(anyLong());
        doReturn(0L).when(era).blockTime(any(), anyLong());
        var fetch = new BlockFetchService(publisher, mock(MetricsService.class), mock(BlockRangeSync.class),
                mock(BlockSync.class), cursorService, era, properties, mock(GenesisConfig.class),
                mock(ShelleyBlockEventPublisher.class), mock(ByronBlockEventPublisher.class), mock(ErrorStorage.class));
        fetch.onBlock(Era.Babbage, incoming(5090, 5089), List.of());
        transitions.clear();
        fetch.onBlock(Era.Conway, incoming(5100, 5090), List.of());
        var expected51 = rows.get(51).getNonce();
        cursor.set(cursor(5100, Era.Conway));
        fetch.onRollback(new Point(5090, "block5090"));
        assertThat(rows).doesNotContainKey(51);
        assertThat(rows.get(50)).isSameAs(seed);
        // This replacement block is past the candidate cutoff at slot 5080.
        fetch.onBlock(Era.Babbage, incoming(5095, 5090), List.of());
        assertThat(transitions).containsExactly("50->51");
        assertThat(rows.get(50)).isSameAs(seed);
        blocks.add(storedBlock(5095, "cc"));
        fetch.onBlock(Era.Conway, incoming(5100, 5095), List.of());
        assertThat(rows.get(51).getNonce()).isEqualTo(expected51);
        assertThat(transitions).containsExactly("50->51", "50->51");
    }

    private Cursor cursor(long slot, Era era) {
        return Cursor.builder().slot(slot).block(slot).blockHash("block" + slot).era(era).build();
    }

    private com.bloxbean.cardano.yaci.core.model.Block incoming(long slot, long previousSlot) {
        var b = mock(com.bloxbean.cardano.yaci.core.model.Block.class, RETURNS_DEEP_STUBS);
        when(b.getHeader().getHeaderBody().getSlot()).thenReturn(slot);
        when(b.getHeader().getHeaderBody().getBlockHash()).thenReturn("block" + slot);
        when(b.getHeader().getHeaderBody().getBlockNumber()).thenReturn(slot);
        when(b.getHeader().getHeaderBody().getPrevHash()).thenReturn("block" + previousSlot);
        when(b.getHeader().getHeaderBody().getIssuerVkey()).thenReturn("aa".repeat(32));
        return b;
    }

    private Block storedBlock(long slot, String hex) {
        return Block.builder().slot(slot).epochSlot((int) (slot % 100)).era(Era.Babbage.getValue())
                .prevHash(hex.repeat(32)).vrfResult(Vrf.builder().output(hex.repeat(64)).build()).build();
    }
}
