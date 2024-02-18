package com.bloxbean.cardano.yaci.store.mir.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.certs.Certificate;
import com.bloxbean.cardano.yaci.core.model.certs.MoveInstataneous;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredential;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.mir.domain.MirPot;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import org.jooq.exception.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MIRProcessorTest {
    @Mock
    private MIRStorage mirStorage;

    @Mock
    private ApplicationEventPublisher publisher;

    @InjectMocks
    private MIRProcessor mirProcessor;

    @Captor
    private ArgumentCaptor<List<MoveInstataneousReward>> mirProcessorArgCaptor;

    @Test
    void givenCertificateEvent_whenTxCertificatesListIsNull_shouldReturn() throws IOException {
        CertificateEvent certificateEvent = CertificateEvent.builder()
                .txCertificatesList(null)
                .build();

        mirProcessor.handleMIR(certificateEvent);

        Mockito.verify(mirStorage, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenCertificateEvent_whenTxCertificatesListIsEmpty_shouldReturn() throws IOException {
        CertificateEvent certificateEvent = CertificateEvent.builder()
                .txCertificatesList(List.of())
                .build();

        mirProcessor.handleMIR(certificateEvent);

        Mockito.verify(mirStorage, Mockito.never()).save(Mockito.any());
    }

    @Test
    void givenCertificateEvent_shouldSaveMoveInstataneousRewardList() throws IOException {
        CertificateEvent certificateEvent = CertificateEvent.builder()
                .txCertificatesList(txCertificates())
                .metadata(eventMetadata())
                .build();

        mirProcessor.handleMIR(certificateEvent);

        Mockito.verify(mirStorage, Mockito.times(1)).save(mirProcessorArgCaptor.capture());

        List<MoveInstataneousReward> moveInstataneousRewardList = mirProcessorArgCaptor.getValue();

        assertThat(moveInstataneousRewardList).hasSize(2);

        for (int i = 0; i < 2; i++) {
            var moveInstataneousReward = moveInstataneousRewardList.get(i);

            assertThat(moveInstataneousReward.getTxHash()).isEqualTo("b9ebe459c3ba8e890f951dacb50cba6fa02cf099c6308c7abd26cf616bf26ca5");
            assertThat(moveInstataneousReward.getEpoch()).isEqualTo(28);
            assertThat(moveInstataneousReward.getSlot()).isEqualTo(950410);
            assertThat(moveInstataneousReward.getBlockHash()).isEqualTo("49b7ea012f77d2ab0722ac50e3f884012c63c63d5dc5dcadca24d36d0ea82512");
            assertThat(moveInstataneousReward.getBlockNumber()).isEqualTo(174391);
            assertThat(moveInstataneousReward.getBlockTime()).isEqualTo(1666287619);
            assertThat(moveInstataneousReward.getCertIndex()).isEqualTo(i);
        }

        assertThat(moveInstataneousRewardList.get(0).getPot()).isEqualTo(MirPot.TREASURY);
        assertThat(moveInstataneousRewardList.get(0).getAddress()).isEqualTo("stake17xtg6yppa0t30rslkrneva5c9qju40rhndjnuy356kxw83s53nkqp");
        assertThat(moveInstataneousRewardList.get(0).getCredential()).isEqualTo("968d1021ebd7178e1fb0e79676982825cabc779b653e1234d58ce3c6");
        assertThat(moveInstataneousRewardList.get(0).getAmount()).isEqualTo(500);

        assertThat(moveInstataneousRewardList.get(1).getPot()).isEqualTo(MirPot.RESERVES);
        assertThat(moveInstataneousRewardList.get(1).getAddress()).isNull();
        assertThat(moveInstataneousRewardList.get(1).getCredential()).isNull();
        assertThat(moveInstataneousRewardList.get(1).getAmount()).isEqualTo(1000);
    }

    private List<TxCertificates> txCertificates() {
        List<TxCertificates> txCertificates = new ArrayList<>();

        var firstCert = mirCertWhenTheFundsAreMovedToStakeCredentials();
        var secondCert = mirCertWhenTheFundsAreGivenToTheOtherAccountingPot();

        txCertificates.add(TxCertificates.builder()
                .certificates(List.of(firstCert, secondCert))
                .txHash("b9ebe459c3ba8e890f951dacb50cba6fa02cf099c6308c7abd26cf616bf26ca5")
                .build());

        return txCertificates;
    }

    private Certificate mirCertWhenTheFundsAreGivenToTheOtherAccountingPot() {
        return MoveInstataneous.builder()
                .accountingPotCoin(BigInteger.valueOf(1000))
                .reserves(true)
                .stakeCredentialCoinMap(Map.of())
                .treasury(false)
                .build();
    }

    private Certificate mirCertWhenTheFundsAreMovedToStakeCredentials() {
        Map<StakeCredential, BigInteger> stakeCredentialCoinMap = new HashMap<>();
        stakeCredentialCoinMap.put(StakeCredential.builder()
                .hash("968d1021ebd7178e1fb0e79676982825cabc779b653e1234d58ce3c6")
                .type(StakeCredType.SCRIPTHASH)
                .build(), BigInteger.valueOf(500));

        return MoveInstataneous.builder()
                .accountingPotCoin(null)
                .reserves(false)
                .stakeCredentialCoinMap(stakeCredentialCoinMap)
                .treasury(true)
                .build();
    }

    private EventMetadata eventMetadata() {
        return EventMetadata.builder()
                .block(174391)
                .era(Era.Shelley)
                .blockHash("49b7ea012f77d2ab0722ac50e3f884012c63c63d5dc5dcadca24d36d0ea82512")
                .blockTime(1666287619)
                .epochNumber(28)
                .epochSlot(150019)
                .mainnet(true)
                .parallelMode(true)
                .prevBlockHash("c48c4878a556592949f82dc77479a5375736800742c3f1ada7f06a18dd9bb751")
                .remotePublish(true)
                .protocolMagic(1)
                .slotLeader("a57cbcb8ecdf24f469928da924b5bc6e4cbc3b57859577211a0daf6f")
                .syncMode(true)
                .slot(950410)
                .noOfTxs(0)
                .build();
    }
}
