package com.bloxbean.cardano.yaci.store.metadata.processor;

import com.bloxbean.cardano.client.metadata.helper.MetadataToJsonNoSchemaConverter;
import com.bloxbean.cardano.yaci.core.model.AuxData;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.events.AuxDataEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.domain.TxAuxData;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MetadataProcessorTest {
    @Mock
    private TxMetadataStorage metadataStorage;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private MetadataProcessor metadataProcessor;

    @Captor
    private ArgumentCaptor<List<TxMetadataLabel>> txMetadataLabelsCaptor;

    @Test
    void testHandleAuxDataEvent_shouldCreateCorrectLabelBodyAndCborValue() {
        // test with tx 6d6aa47789923e36154550c3beadce98c51462ad9006d1af217f58ba5b45ba85,
        // block 498748, slot 17525700, epoch 44, preprod
        // cbor from yaci: a5006763617264616e6f010e02542512a00e9653fe49a44a5886202e24d77eeb998f03830e182a643133333704a20e182a636b65796576616c7565
        // cbor will pass test: a5006763617264616e6f010e02542512a00e9653fe49a44a5886202e24d77eeb998f03830e182a643133333704a2636b65796576616c75650e182a
        List<TxAuxData> txAuxDataList = List.of(TxAuxData.builder()
                .txHash("6d6aa47789923e36154550c3beadce98c51462ad9006d1af217f58ba5b45ba85")
                .auxData(new AuxData("a5006763617264616e6f010e02542512a00e9653fe49a44a5886202e24d77eeb998f03830e182a643133333704a2636b65796576616c75650e182a",
                        "{\"0\":\"cardano\",\"1\":14,\"2\":\"0x2512a00e9653fe49a44a5886202e24d77eeb998f\",\"3\":[14,42,\"1337\"],\"4\":{\"14\":42,\"key\":\"value\"}}",
                        null, new ArrayList<>(), new ArrayList<>()))
                .build());

        AuxDataEvent auxDataEvent = AuxDataEvent.builder()
                .metadata(EventMetadata.builder()
                        .slot(17525700)
                        .blockTime(1666303721)
                        .blockHash("2cefda52f214ff137327e3fd90ebc7acc623664dc5d820d1c1cceb36399b22ea")
                        .prevBlockHash("e6887297f30d50f87a3458feb12679fe01d3ad1d9483ea59ce8f7af58517854c") //
                        .slotLeader("47ab3e3ef91004d7fa4db7a886753ad50631381312bc7a9b2f5d16da")
                        .epochNumber(44)
                        .block(498748)
                        .epochSlot(159300)
                        .era(Era.Babbage)
                        .protocolMagic(1)
                        .build())
                .txAuxDataList(txAuxDataList)
                .build();
        /*
            expected data (key, body, cbor) (expected data referenced from cardano-db-sync)
             [
                {
                    "key" : 0,
                    "json" : "\"cardano\"",
                    "cbor" : "a1006763617264616e6f"
                },
                {
                    "key" : 1,
                    "json" : "14",
                    "cbor" : "a1010e"
                },
                {
                    "key" : 2,
                    "json" : "\"0x2512a00e9653fe49a44a5886202e24d77eeb998f\"",
                    "cbor" : "a102542512a00e9653fe49a44a5886202e24d77eeb998f"
                },
                {
                    "key" : 3,
                    "json" : "[14, 42, \"1337\"]",
                    "cbor" : "a103830e182a6431333337"
                }
                {
                    "key" : 4,
                    "json" : "{\"14\": 42, \"key\": \"value\"}",
                    "cbor" : "a104a2636b65796576616c75650e182a"
                }
            ]
         */
        metadataProcessor.handleAuxDataEvent(auxDataEvent);

        Mockito.verify(metadataStorage, Mockito.times(1)).saveAll(txMetadataLabelsCaptor.capture());

        List<TxMetadataLabel> metadataLabelsSaved = txMetadataLabelsCaptor.getValue();

        assertThat(metadataLabelsSaved).hasSize(5);
        assertThat(metadataLabelsSaved).map(TxMetadataLabel::getLabel).contains("0", "1", "2", "3", "4");

        for (var metadataLabel : metadataLabelsSaved) {
            switch (metadataLabel.getLabel()) {
                case "0":
                    assertThat(metadataLabel.getBody()).isEqualToIgnoringWhitespace("\"cardano\"");
                    assertThat(metadataLabel.getCbor()).isEqualTo("a1006763617264616e6f");
                    break;
                case "1":
                    assertThat(metadataLabel.getBody()).isEqualToIgnoringWhitespace("14");
                    assertThat(metadataLabel.getCbor()).isEqualTo("a1010e");
                    break;
                case "2":
                    assertThat(metadataLabel.getBody()).isEqualToIgnoringWhitespace("\"0x2512a00e9653fe49a44a5886202e24d77eeb998f\"");
                    assertThat(metadataLabel.getCbor()).isEqualTo("a102542512a00e9653fe49a44a5886202e24d77eeb998f");
                    break;
                case "3":
                    assertThat(metadataLabel.getBody()).isEqualToIgnoringWhitespace("[14, 42, \"1337\"]");
                    assertThat(metadataLabel.getCbor()).isEqualTo("a103830e182a6431333337");
                    break;
                case "4":
                    assertThat(metadataLabel.getBody()).isEqualToIgnoringWhitespace("{\"14\": 42, \"key\": \"value\"}");
                    assertThat(metadataLabel.getCbor()).isEqualTo("a104a2636b65796576616c75650e182a"); // fail here
                    break;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(MetadataToJsonNoSchemaConverter.cborHexToJson("a104a20e182a636b65796576616c7565"));
        System.out.println(MetadataToJsonNoSchemaConverter.cborHexToJson("a104a2636b65796576616c75650e182a"));
    }
}
