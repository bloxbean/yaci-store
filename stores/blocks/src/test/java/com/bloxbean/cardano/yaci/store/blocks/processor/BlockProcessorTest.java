package com.bloxbean.cardano.yaci.store.blocks.processor;

import com.bloxbean.cardano.yaci.core.model.*;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.bloxbean.cardano.yaci.store.blocks.storage.api.BlockStorage;
import com.bloxbean.cardano.yaci.store.events.BlockEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BlockProcessorTest {
    @Mock
    private BlockStorage blockStorage;
    @InjectMocks
    private BlockProcessor blockProcessor;
    @Captor
    ArgumentCaptor<Block> blockArgCaptor;

    @Test
    void givenBlockEvent_shouldHandleBlockEventAndSaveBlock() {

        blockProcessor.handleBlockHeaderEvent(blockEvent());

        Mockito.verify(blockStorage, Mockito.times(1)).save(blockArgCaptor.capture());

        Block block = blockArgCaptor.getValue();

        assertThat(block.getHash()).isEqualTo("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937");
        assertThat(block.getNumber()).isEqualTo(200);
        assertThat(block.getSlot()).isEqualTo(86880);
        assertThat(block.getEpochNumber()).isEqualTo(200);
        assertThat(block.getEpochSlot()).isEqualTo(205347);
        assertThat(block.getBlockTime()).isEqualTo(1654041600);
        assertThat(block.getEra()).isEqualTo(7);
        assertThat(block.getPrevHash()).isEqualTo("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937");
        assertThat(block.getIssuerVkey()).isEqualTo("618b625df30de53895ff29e7a3770dca56c2ff066d4aa05a6971905deecef6db");
        assertThat(block.getVrfVkey()).isEqualTo("ea49e4652c460b9ee6daafefc999ca667fbe5eb5d7a7aeabbdff6fe19c1a3c9f");
        assertThat(block.getNonceVrf().getOutput()).isEqualTo("c5fd63bee2f2ca0168f970c486eb7bfcf7cc30b1087e64f1c4b9fae591e35c431aff49fe3f4226a77595d2bb71a322d242d4feadca148a7cbc4406f7fbccd00d67470fb740b982f1f045249f91ef1605");
        assertThat(block.getNonceVrf().getProof()).isEqualTo("e8690cbbd225df7e2f3815d7c65f47275b362d6f5e696ad5b4c51646c43f5ac4a7182315c1cc5837ed32e97bf90c8dcf0a40540ebf0862ec7a01d84560964a9f");
        assertThat(block.getLeaderVrf().getOutput()).isEqualTo("71dac7e1b3cdf1398fe1ee81defa9b3a21691f79e14f1f6383eccba91122c85d7b175bfe6e3501d1ca41b11984d1cc6964d7b13b13c73357940dbe3768b4cf3efa44cfd882316492ca048d70424b6405");
        assertThat(block.getLeaderVrf().getProof()).isEqualTo("dca201faa1ea8db34b1a24debaa320e10c739e51ba00a7e60b3fd625d893045402ae6b0134277535e1a34f08f8198d9d7d38076836e810b3f7abda3b5ecd1246");
        assertThat(block.getVrfResult().getOutput()).isEqualTo("224e651efc0c8a22828d60517c16e4e2762588503d8ad6da7746ac8fd9f81342913bca6cd5bb9a76311ecafe71105ff2a621c693424accae9831f46917cd524a6fcebf70383a51d6f45fb557222bf307");
        assertThat(block.getVrfResult().getProof()).isEqualTo("a51905dc0a01d438ee2c29ddeb09684dd31e5af09135fa9530e4dca7073b8e82a927a427976ecd9851422db8a8d749288d6fc4cc0c5696dc8428874c4f00a84c");
        assertThat(block.getBlockBodySize()).isZero();
        assertThat(block.getBlockBodyHash()).isEqualTo("1033376be025cb705fd8dd02eda11cc73975a062b5d14ffd74d6ff69e69a2ff7");
        assertThat(block.getProtocolVersion()).isEqualTo("3.0");
        assertThat(block.getNoOfTxs()).isEqualTo(3);
        assertThat(block.getSlotLeader()).isEqualTo("12946a3fe080dd99af599bfff10a05cd3de19bd38ed85b25dee35dd5");
        assertThat(block.getOpCertHotVKey()).isEqualTo("54914d6210ca26a038509e3f5b01eeec60719766ec2f839e2c8b2edcb39e4847");
        assertThat(block.getOpcertKesPeriod()).isZero();
        assertThat(block.getOpCertSigma()).isEqualTo("2afe39cf023b5f25961b2e4b9148d533c6677c9c48b81e5d5a2cf52e32040f77683e81f0722baa5dea2ba0a425c5b43e4bb912c34a2f826ef4cc639cb3778a0f");
        assertThat(block.getOpCertSeqNumber()).isZero();
    }

    private BlockEvent blockEvent() {
        return BlockEvent.builder()
                .metadata(EventMetadata.builder()
                        .era(Era.CONWAY)
                        .epochNumber(200)
                        .epochSlot(205347)
                        .blockTime(1654041600)
                        .noOfTxs(3)
                        .slotLeader("12946a3fe080dd99af599bfff10a05cd3de19bd38ed85b25dee35dd5")
                        .build())
                .block(com.bloxbean.cardano.yaci.core.model.Block.builder()
                        .header(BlockHeader.builder()
                                .headerBody(HeaderBody.builder()
                                        .slot(86880)
                                        .blockNumber(200)
                                        .blockHash("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937")
                                        .prevHash("d4b8de7a11d929a323373cbab6c1a9bdc931beffff11db111cf9d57356ee1937")
                                        .issuerVkey("618b625df30de53895ff29e7a3770dca56c2ff066d4aa05a6971905deecef6db")
                                        .vrfVkey("ea49e4652c460b9ee6daafefc999ca667fbe5eb5d7a7aeabbdff6fe19c1a3c9f")
                                        .nonceVrf(VrfCert.builder()
                                                ._1("c5fd63bee2f2ca0168f970c486eb7bfcf7cc30b1087e64f1c4b9fae591e35c431aff49fe3f4226a77595d2bb71a322d242d4feadca148a7cbc4406f7fbccd00d67470fb740b982f1f045249f91ef1605")
                                                ._2("e8690cbbd225df7e2f3815d7c65f47275b362d6f5e696ad5b4c51646c43f5ac4a7182315c1cc5837ed32e97bf90c8dcf0a40540ebf0862ec7a01d84560964a9f")
                                                .build())
                                        .leaderVrf(VrfCert.builder()
                                                ._1("71dac7e1b3cdf1398fe1ee81defa9b3a21691f79e14f1f6383eccba91122c85d7b175bfe6e3501d1ca41b11984d1cc6964d7b13b13c73357940dbe3768b4cf3efa44cfd882316492ca048d70424b6405")
                                                ._2("dca201faa1ea8db34b1a24debaa320e10c739e51ba00a7e60b3fd625d893045402ae6b0134277535e1a34f08f8198d9d7d38076836e810b3f7abda3b5ecd1246")
                                                .build())
                                        .vrfResult(VrfCert.builder()
                                                ._1("224e651efc0c8a22828d60517c16e4e2762588503d8ad6da7746ac8fd9f81342913bca6cd5bb9a76311ecafe71105ff2a621c693424accae9831f46917cd524a6fcebf70383a51d6f45fb557222bf307")
                                                ._2("a51905dc0a01d438ee2c29ddeb09684dd31e5af09135fa9530e4dca7073b8e82a927a427976ecd9851422db8a8d749288d6fc4cc0c5696dc8428874c4f00a84c")
                                                .build())
                                        .blockBodySize(0)
                                        .blockBodyHash("1033376be025cb705fd8dd02eda11cc73975a062b5d14ffd74d6ff69e69a2ff7")
                                        .protocolVersion(ProtocolVersion.builder()
                                                ._1(3)
                                                ._2(0)
                                                .build())
                                        .operationalCert(OperationalCert.builder()
                                                .sigma("2afe39cf023b5f25961b2e4b9148d533c6677c9c48b81e5d5a2cf52e32040f77683e81f0722baa5dea2ba0a425c5b43e4bb912c34a2f826ef4cc639cb3778a0f")
                                                .sequenceNumber(0)
                                                .kesPeriod(0)
                                                .hotVKey("54914d6210ca26a038509e3f5b01eeec60719766ec2f839e2c8b2edcb39e4847")
                                                .build())
                                        .build())
                                .build())
                        .transactionBodies(new ArrayList<>())
                        .build())
                .build();
    }
}
