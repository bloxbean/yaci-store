package com.bloxbean.cardano.yaci.store.live.processor;

import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.events.BlockHeaderEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.live.BlocksWebSocketHandler;
import com.bloxbean.cardano.yaci.store.live.cache.BlockCache;
import com.bloxbean.cardano.yaci.store.live.cache.RecentTxCache;
import com.bloxbean.cardano.yaci.store.live.dto.BlockData;
import com.bloxbean.cardano.yaci.store.live.dto.RecentTxs;
import com.bloxbean.cardano.yaci.store.live.mapper.BlockDataMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LiveEventProcessorTest {
    @Mock
    private BlocksWebSocketHandler socketHandler;
    @Mock
    private BlockCache blockCache;
    @Mock
    private RecentTxCache recentTxCache;
    @Mock
    private EraService eraService;
    @Mock
    private BlockDataMapper blockDataMapper;
    @InjectMocks
    private LiveEventProcessor liveEventProcessor;
    @Captor
    private ArgumentCaptor<BlockData> argBlockDataCaptor;
    @Captor
    private ArgumentCaptor<RecentTxs> argTransactionCaptor;

    @Test
    void givenBlockHeaderEvent_whenIsSyncModeIsFalse_shouldReturn() throws IOException {
        BlockHeaderEvent blockHeaderEvent = BlockHeaderEvent.builder()
                .metadata(EventMetadata.builder()
                        .syncMode(false)
                        .build())
                .build();

        liveEventProcessor.handleBlockHeaderEvent(blockHeaderEvent);
        Mockito.verify(blockCache, Mockito.never()).addBlock(Mockito.any());
        Mockito.verify(socketHandler, Mockito.never()).broadcastBlockData(Mockito.any());
    }

    @Test
    void givenBlockHeaderEvent_shouldBroadcastBlockData() throws IOException {
        BlockHeaderEvent blockHeaderEvent = BlockHeaderEvent.builder()
                .metadata(EventMetadata.builder()
                        .syncMode(true)
                        .era(Era.Shelley)
                        .noOfTxs(1)
                        .blockTime(1655770380)
                        .slotLeader("de7ca985023cf892f4de7f5f1d0a7181668884752d9ebb9e96c95059")
                        .epochNumber(106)
                        .epochSlot(205406)
                        .slot(87140)
                        .build())
                .blockHeader(BlockHeader.builder()
                        .headerBody(HeaderBody.builder()
                                .blockNumber(177073)
                                .slot(87140)
                                .vrfVkey("ea49e4652c460b9ee6daafefc999ca667fbe5eb5d7a7aeabbdff6fe19c1a3c9f")
                                .blockHash("4a620c90e7dd4f68b7a4be2dc5736b2c7f3f6d02bdff8f2721c79f233c6857e4")
                                .blockBodySize(227)
                                .build())
                        .bodySignature("b2d0b7d7633cca1adf49694225399ec86e0b79739b1b2ed94c7aed8502214767")
                        .build())
                .build();

        Mockito.when(blockDataMapper.blockHeaderToBlockData(Mockito.any())).thenReturn(BlockData.builder().build());

        liveEventProcessor.handleBlockHeaderEvent(blockHeaderEvent);

        Mockito.verify(eraService).slotsPerEpoch(Era.Shelley);
        Mockito.verify(blockCache).addBlock(Mockito.any());
        Mockito.verify(socketHandler, Mockito.times(1)).broadcastBlockData(argBlockDataCaptor.capture());

        BlockData blockData = argBlockDataCaptor.getValue();

        assertThat(blockData.getEra()).isEqualTo(2);
        assertThat(blockData.getNTx()).isEqualTo(1);
        assertThat(blockData.getBlockTime()).isEqualTo(1655770380);
        assertThat(blockData.getSlotLeader()).isEqualTo("de7ca985023cf892f4de7f5f1d0a7181668884752d9ebb9e96c95059");
        assertThat(blockData.getEpoch()).isEqualTo(106);
        assertThat(blockData.getSlot()).isEqualTo(87140);
        assertThat(blockData.getEpochSlot()).isEqualTo(205406);
    }

    @Test
    void givenTransactionEvent_SyncModeIsFalse_shouldReturn() throws IOException {
        TransactionEvent event = TransactionEvent.builder()
                .metadata(EventMetadata.builder()
                        .syncMode(false)
                        .build())
                .build();

        liveEventProcessor.handleTransactionEvent(event);
        Mockito.verify(socketHandler, Mockito.never()).broadcastRecentTxs(Mockito.any());
    }

    @Test
    void givenTransactionEvent_TransactionIsZeroSize_shouldReturn() throws IOException {
        List<Transaction> transactions = new ArrayList<>();

        TransactionEvent event = TransactionEvent.builder()
                .metadata(EventMetadata.builder()
                        .syncMode(true)
                        .build())
                .transactions(transactions)
                .build();

        liveEventProcessor.handleTransactionEvent(event);
        Mockito.verify(socketHandler, Mockito.never()).broadcastRecentTxs(Mockito.any());
    }

    @Test
    void givenTransactionEvent_shouldBroadcastTxs() throws IOException {
        TransactionEvent event = TransactionEvent.builder()
                .metadata(EventMetadata.builder()
                        .syncMode(true)
                        .block(177069)
                        .slot(86600)
                        .build())
                .transactions(transactions())
                .build();

        liveEventProcessor.handleTransactionEvent(event);

        Mockito.verify(recentTxCache, Mockito.times(1)).addTx(Mockito.any());
        Mockito.verify(socketHandler).broadcastRecentTxs(argTransactionCaptor.capture());

        RecentTxs recentTxs = argTransactionCaptor.getValue();

        assertThat(recentTxs.getRecentTxs().getFirst().getHash()).isEqualTo("b731574b44de062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45");
        assertThat(recentTxs.getRecentTxs().getFirst().getSlot()).isEqualTo(86600);
        assertThat(recentTxs.getRecentTxs().getFirst().getBlock()).isEqualTo(177069);
        assertThat(recentTxs.getRecentTxs().getFirst().getOutput()).isEqualTo(96000000);
        assertThat(recentTxs.getRecentTxs().getFirst().getOutputAddresses()).hasSize(1);
        assertThat(recentTxs.getRecentTxs().getFirst().getOutputAddresses()).isEqualTo(Set.of("addr_test1qpe6s9amgfwtu9u6lqj998vke6uncswr4dg88qqft5d7f67kfjf77qy57hqhnefcqyy7hmhsygj9j38rj984hn9r57fswc4wg0"));
    }

    private List<Transaction> transactions() {
        List<Transaction> transactions = new ArrayList<>();

        transactions.add(Transaction.builder()
                        .txHash("b731574b44de062ade1e70d0040abde47a6626c7d8e98816a9d87e6bd6228b45")
                        .body(TransactionBody.builder()
                                .outputs(outputsTransactionBody())
                                .build())
                .build());

        return transactions;
    }

    private List<TransactionOutput> outputsTransactionBody() {
        List<TransactionOutput> transactionOutputs = new ArrayList<>();
        List<Amount> amounts = new ArrayList<>();

        amounts.add(Amount.builder()
                .quantity(BigInteger.valueOf(48000000))
                .assetName("lovelace")
                .policyId(null)
                .build());

        amounts.add(Amount.builder()
                .quantity(BigInteger.valueOf(48000000))
                .assetName("lovelace")
                .policyId(null)
                .build());

        transactionOutputs.add(TransactionOutput.builder()
                        .address("addr_test1qpe6s9amgfwtu9u6lqj998vke6uncswr4dg88qqft5d7f67kfjf77qy57hqhnefcqyy7hmhsygj9j38rj984hn9r57fswc4wg0")
                        .amounts(amounts)
                .build());
        return transactionOutputs;
    }
}
