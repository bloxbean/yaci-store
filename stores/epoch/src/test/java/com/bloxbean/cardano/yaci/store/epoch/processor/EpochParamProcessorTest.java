package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.model.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.epoch.storage.ProtocolParamsProposalStorage;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpochParamProcessorTest {

    @Mock
    private EpochParamStorage epochParamStorage;

    @Mock
    private ProtocolParamsProposalStorage protocolParamsProposalStorage;

    @Mock
    private EraGenesisProtocolParamsUtil eraGenesisProtocolParamsUtil;

    @Mock
    private ProposalStateClient proposalStateClient;

    @InjectMocks
    private EpochParamProcessor epochParamProcessor;

    @Captor
    private ArgumentCaptor<EpochParam> argCaptor;

    EpochParamProcessorTest() {
    }

    @Test
    void givenEpochChangeEvent_whenPreviousEpochIsNullAndEpochEqualsMaxEpoch_shouldReturn() {
        PreEpochTransitionEvent epochChangeEvent = PreEpochTransitionEvent.builder()
                .epoch(28)
                .previousEpoch(null)
                .era(Era.Byron)
                .previousEra(Era.Byron)
                .metadata(EventMetadata.builder()
                        .slot(12961)
                        .block(177070)
                        .blockTime(1666342887)
                        .protocolMagic(1)
                        .build())
                .build();

        when(epochParamStorage.getMaxEpoch()).thenReturn(28);

        epochParamProcessor.handleEpochChangeEvent(epochChangeEvent);
        Mockito.verify(epochParamStorage, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenEpochChangeEvent_whenMaxEpochIsNotNullAndMaxEpochPlusOneIsNotEqualToEpoch_shouldReturn() {
        PreEpochTransitionEvent epochChangeEvent = PreEpochTransitionEvent.builder()
                .epoch(28)
                .previousEpoch(28)
                .era(Era.Shelley)
                .previousEra(Era.Byron)
                .metadata(EventMetadata.builder()
                        .slot(12961)
                        .block(177070)
                        .blockTime(1666342887)
                        .protocolMagic(1)
                        .build())
                .build();

        when(epochParamStorage.getMaxEpoch()).thenReturn(30);

        epochParamProcessor.handleEpochChangeEvent(epochChangeEvent);
        Mockito.verify(epochParamStorage, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenEpochChangeEvent_shouldSaveEpochParam() {
        PreEpochTransitionEvent epochChangeEvent = PreEpochTransitionEvent.builder()
                .epoch(28)
                .previousEpoch(27)
                .era(Era.Alonzo)
                .previousEra(Era.Byron)
                .metadata(EventMetadata.builder()
                        .slot(12961)
                        .block(177070)
                        .blockTime(1666342887)
                        .protocolMagic(1)
                        .build())
                .build();

        Mockito.when(epochParamStorage.getMaxEpoch()).thenReturn(27);
        epochParamProcessor.handleEpochChangeEvent(epochChangeEvent);

        Mockito.verify(epochParamStorage, Mockito.times(1)).save(argCaptor.capture());

        Mockito.verify(epochParamStorage).getProtocolParams(27);
        Mockito.verify(protocolParamsProposalStorage).getProtocolParamsProposalsByTargetEpoch(27);

        EpochParam epochParam = argCaptor.getValue();

        assertThat(epochParam.getEpoch()).isEqualTo(28);
        assertThat(epochParam.getSlot()).isEqualTo(12961);
        assertThat(epochParam.getBlockNumber()).isEqualTo(177070);
        assertThat(epochParam.getBlockTime()).isEqualTo(1666342887);
    }

    @Test
    void givenEpochChangeEvent_whenEraIsConwayOrLater_ProtocolParamChangeActionEnacted_shouldUpdatePPram() {
        PreEpochTransitionEvent epochChangeEvent = PreEpochTransitionEvent.builder()
                .epoch(28)
                .previousEpoch(27)
                .era(Era.Conway)
                .previousEra(Era.Babbage)
                .metadata(EventMetadata.builder()
                        .slot(1000000)
                        .block(10000)
                        .blockTime(1666342887)
                        .protocolMagic(1)
                        .build())
                .build();

        Mockito.when(epochParamStorage.getMaxEpoch()).thenReturn(27);

        Map<String, long[]> oldCostModels = new HashMap<>();
        oldCostModels.put("PlutusV1", new long[] {0,1});
        oldCostModels.put("PlutusV3", new long[] {2,3});

        Mockito.when(epochParamStorage.getProtocolParams(epochChangeEvent.getPreviousEpoch()))
                .thenReturn(
                        Optional.of(
                                EpochParam.builder()
                                                .params(ProtocolParams
                                                        .builder()
                                                        .costModels(oldCostModels)
                                                        .maxTxSize(1000)
                                                        .drepActivity(10)
                                                        .build())
                                        .build()
                        )
                );
        Mockito.when(proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epochChangeEvent.getPreviousEpoch())).thenReturn(
                List.of(
                        GovActionProposal.builder()
                                .govAction(
                                        ParameterChangeAction.builder()
                                                .protocolParamUpdate(ProtocolParamUpdate
                                                        .builder()
                                                        .costModels(Map.of(2,
                                                                "9f1a000189b41901a401011903e818ad00011903e819ea350401192baf18201a000312591920a404193e801864193e801864193e801864193e801864193e801864193e80186418641864193e8018641a000170a718201a00020782182019f016041a0001194a18b2000119568718201a0001643519030104021a00014f581a0001e143191c893903831906b419022518391a00014f580001011903e819a7a90402195fe419733a1826011a000db464196a8f0119ca3f19022e011999101903e819ecb2011a00022a4718201a000144ce1820193bc318201a0001291101193371041956540a197147184a01197147184a0119a9151902280119aecd19021d0119843c18201a00010a9618201a00011aaa1820191c4b1820191cdf1820192d1a18201a00014f581a0001e143191c893903831906b419022518391a00014f5800011a0001614219020700011a000122c118201a00014f581a0001e143191c893903831906b419022518391a00014f580001011a00014f581a0001e143191c893903831906b419022518391a00014f5800011a000e94721a0003414000021a0004213c19583c041a00163cad19fc3604194ff30104001a00022aa818201a000189b41901a401011a00013eff182019e86a1820194eae182019600c1820195108182019654d182019602f18201a0290f1e70a1a032e93af1937fd0a1a0298e40b1966c40a193e801864193e8018641a000eaf1f121a002a6e06061a0006be98011a0321aac7190eac121a00041699121a048e466e1922a4121a0327ec9a121a001e743c18241a0031410f0c1a000dbf9e011a09f2f6d31910d318241a0004578218241a096e44021967b518241a0473cee818241a13e62472011a0f23d40118481a00212c5618481a0022814619fc3b041a00032b00192076041a0013be0419702c183f00011a000f59d919aa6718fb00011a000187551902d61902cf00011a000187551902d61902cf00011a000187551902d61902cf00011a0001a5661902a800011a00017468011a00044a391949a000011a0002bfe2189f01011a00026b371922ee00011a00026e9219226d00011a0001a3e2190ce2011a00019e4919028f011a001df8bb195fc803ff")
                                                        )
                                                        .costModelsHash("e5393f1bbc6ed875c925918ee63acb34ca2cf3534312cf531227265538e6c076")
                                                        .build()).build())
                                .build(),
                        GovActionProposal.builder()
                                .govAction(
                                        ParameterChangeAction.builder()
                                                .protocolParamUpdate(ProtocolParamUpdate
                                                        .builder()
                                                        .drepActivity(20)
                                                        .build()).build())
                                .build()
                        ));

        epochParamProcessor.handleEpochChangeEvent(epochChangeEvent);

        Mockito.verify(epochParamStorage, Mockito.times(1)).save(argCaptor.capture());

        Mockito.verify(epochParamStorage).getProtocolParams(27);
        Mockito.verify(protocolParamsProposalStorage).getProtocolParamsProposalsByTargetEpoch(27);

        EpochParam epochParam = argCaptor.getValue();

        var costModels = epochParam.getParams().getCostModels();
        assertThat(costModels.get("PlutusV1")).isEqualTo(new long[]{0,1});
        assertThat(costModels.get("PlutusV3")).isEqualTo(new long[]{100788, 420, 1, 1, 1000, 173, 0, 1, 1000, 59957, 4, 1, 11183, 32, 201305, 8356, 4, 16000, 100, 16000, 100, 16000, 100, 16000, 100, 16000, 100, 16000, 100, 100, 100, 16000, 100, 94375, 32, 132994, 32, 61462, 4, 72010, 178, 0, 1, 22151, 32, 91189, 769, 4, 2, 85848, 123203, 7305, -900, 1716, 549, 57, 85848, 0, 1, 1, 1000, 42921, 4, 2, 24548, 29498, 38, 1, 898148, 27279, 1, 51775, 558, 1, 39184, 1000, 60594, 1, 141895, 32, 83150, 32, 15299, 32, 76049, 1, 13169, 4, 22100, 10, 28999, 74, 1, 28999, 74, 1, 43285, 552, 1, 44749, 541, 1, 33852, 32, 68246, 32, 72362, 32, 7243, 32, 7391, 32, 11546, 32, 85848, 123203, 7305, -900, 1716, 549, 57, 85848, 0, 1, 90434, 519, 0, 1, 74433, 32, 85848, 123203, 7305, -900, 1716, 549, 57, 85848, 0, 1, 1, 85848, 123203, 7305, -900, 1716, 549, 57, 85848, 0, 1, 955506, 213312, 0, 2, 270652, 22588, 4, 1457325, 64566, 4, 20467, 1, 4, 0, 141992, 32, 100788, 420, 1, 1, 81663, 32, 59498, 32, 20142, 32, 24588, 32, 20744, 32, 25933, 32, 24623, 32, 43053543, 10, 53384111, 14333, 10, 43574283, 26308, 10, 16000, 100, 16000, 100, 962335, 18, 2780678, 6, 442008, 1, 52538055, 3756, 18, 267929, 18, 76433006, 8868, 18, 52948122, 18, 1995836, 36, 3227919, 12, 901022, 1, 166917843, 4307, 36, 284546, 36, 158221314, 26549, 36, 74698472, 36, 333849714, 1, 254006273, 72, 2174038, 72, 2261318, 64571, 4, 207616, 8310, 4, 1293828, 28716, 63, 0, 1, 1006041, 43623, 251, 0, 1, 100181, 726, 719, 0, 1, 100181, 726, 719, 0, 1, 100181, 726, 719, 0, 1, 107878, 680, 0, 1, 95336, 1, 281145, 18848, 0, 1, 180194, 159, 1, 1, 158519, 8942, 0, 1, 159378, 8813, 0, 1, 107490, 3298, 1, 106057, 655, 1, 1964219, 24520, 3});
        assertThat(epochParam.getParams().getCostModelsHash()).isEqualTo("e5393f1bbc6ed875c925918ee63acb34ca2cf3534312cf531227265538e6c076");
        assertThat(epochParam.getParams().getDrepActivity()).isEqualTo(20);
        assertThat(epochParam.getParams().getMaxTxSize()).isEqualTo(1000);
    }
    //TODO -- Add more test cases without mock
}
