package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.byron.*;
import com.bloxbean.cardano.yaci.core.model.byron.payload.ByronTxPayload;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.events.ByronEbBlockEvent;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ByronBlockProcessorTest {
    @InjectMocks
    private ByronBlockProcessor byronBlockProcessor;

    @Mock
    private BlockStorage blockStorage;

    @Captor
    ArgumentCaptor<Block> argGenesisCaptor;

    @Captor
    ArgumentCaptor<Block> argByronCaptor;

    @Captor
    ArgumentCaptor<Block> byronEbArgCaptor;

    @Test
    void givenGenesisBlockEvent_shouldSaveBlock() throws Exception {
        GenesisBlockEvent genesisBlockEvent = GenesisBlockEvent.builder()
                .era(Era.Byron)
                .blockHash("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937")
                .slot(-1)
                .block(-1)
                .blockTime(1654041600)
                .build();

        byronBlockProcessor.handleGenesisBlockEvent(genesisBlockEvent);
        Mockito.verify(blockStorage, Mockito.times(1)).save(argGenesisCaptor.capture());

        Block block = argGenesisCaptor.getValue();

        assertThat(block.getHash()).isEqualTo("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937");
        assertThat(block.getBlockTime()).isEqualTo(1654041600);
        assertThat(block.getSlot()).isEqualTo(-1);
        assertThat(block.getEra()).isEqualTo(Era.Byron.getValue());
        assertThat(block.getNumber()).isEqualTo(-1);
    }

    @Test
    void givenByronMainBlockEvent_shouldSaveBlock() throws Exception {
        ByronMainBlockEvent byronMainBlockEvent = ByronMainBlockEvent.builder()
                .metadata(EventMetadata.builder()
                        .slot(95520)
                        .block(1)
                        .epochNumber(0)
                        .epochSlot(2)
                        .blockTime(1655778720)
                        .slotLeader("d15422b2e8b60e500a82a8f4ceaa98b04e55a0171d1125f6c58f8758")
                        .build())
                .byronMainBlock(ByronMainBlock.builder()
                        .header(ByronBlockHead.builder()
                                .blockHash("4bbe984de25c79052af653c2424122a7b324b27143886849028789a597ce4ae6")
                                .prevBlock("90a24bc18a5b0d5be239379248b0f5fe6e3f830c69604c8dff870d922d948bb1")
                                .build())
                        .body(ByronBlockBody.builder()
                                .txPayload(txPayload())
                                .build())
                        .build())
                .build();

        byronBlockProcessor.handleByronMainBlockEvent(byronMainBlockEvent);

        Mockito.verify(blockStorage, Mockito.times(1)).save(argByronCaptor.capture());

        Block block = argByronCaptor.getValue();

        assertThat(block.getHash()).isEqualTo("4bbe984de25c79052af653c2424122a7b324b27143886849028789a597ce4ae6");
        assertThat(block.getBlockTime()).isEqualTo(1655778720);
        assertThat(block.getSlot()).isEqualTo(95520);
        assertThat(block.getNumber()).isEqualTo(1);
        assertThat(block.getPrevHash()).isEqualTo("90a24bc18a5b0d5be239379248b0f5fe6e3f830c69604c8dff870d922d948bb1");
        assertThat(block.getSlotLeader()).isEqualTo("d15422b2e8b60e500a82a8f4ceaa98b04e55a0171d1125f6c58f8758");
        assertThat(block.getTotalOutput()).isEqualTo(48000000);
        assertThat(block.getEpochSlot()).isEqualTo(2);
        assertThat(block.getEpochNumber()).isEqualTo(0);
    }

    private List<ByronTxPayload> txPayload() {
        List<ByronTxPayload> byronTxPayloads = new ArrayList<>();

        List<ByronTxIn> byronTxIns = new ArrayList<>();
        byronTxIns.add(ByronTxIn.builder()
                .index(0)
                .txId("6d2174d3956d8eb2b3e1e198e817ccf1332a599d5d7320400bfd820490d706be")
                .build());

        List<ByronTxOut> byronTxOuts = new ArrayList<>();
        byronTxOuts.add(ByronTxOut.builder()
                .amount(BigInteger.valueOf(48000000))
                .build());

        byronTxPayloads.add(ByronTxPayload.builder()
                .transaction(ByronTx.builder()
                        .txHash("b9ebe459c3ba8e890f951dacb50cba6fa02cf099c6308c7abd26cf616bf26ca5")
                        .inputs(byronTxIns)
                        .outputs(byronTxOuts)
                        .build())
                .build());

        return byronTxPayloads;
    }

    @Test
    void givenByronEbBlockEvent_shouldSaveBlock() throws Exception {
        ByronEbBlockEvent byronEbBlockEvent = ByronEbBlockEvent.builder()
                .byronEbBlock(ByronEbBlock.builder()
                        .header(ByronEbHead.builder()
                                .prevBlock("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937")
                                .blockHash("9ad7ff320c9cf74e0f5ee78d22a85ce42bb0a487d0506bf60cfb5a91ea4497d2")
                                .build())
                        .build())
                .metadata(EventMetadata.builder()
                        .block(0)
                        .slotLeader(null)
                        .epochSlot(0)
                        .epochNumber(0)
                        .blockTime(1654041600)
                        .slot(0)
                        .build())
                .build();

        byronBlockProcessor.handleByronEbBlockEvent(byronEbBlockEvent);
        Mockito.verify(blockStorage, Mockito.times(1)).save(byronEbArgCaptor.capture());

        Block block = byronEbArgCaptor.getValue();

        assertThat(block.getHash()).isEqualTo("9ad7ff320c9cf74e0f5ee78d22a85ce42bb0a487d0506bf60cfb5a91ea4497d2");
        assertThat(block.getBlockTime()).isEqualTo(1654041600);
        assertThat(block.getSlot()).isEqualTo(0);
        assertThat(block.getNumber()).isEqualTo(0);
        assertThat(block.getPrevHash()).isEqualTo("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937");
        assertThat(block.getSlotLeader()).isNull();
        assertThat(block.getEpochSlot()).isEqualTo(0);
        assertThat(block.getEpochNumber()).isEqualTo(0);
    }
}
