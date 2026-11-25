package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.model.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.storage.impl.repository.Cip113RegistryNodeRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReason;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenRecorder;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.invalidtoken.InvalidTokenReport;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113MainnetNodes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Cip113Processor")
class Cip113ProcessorTest {

    private static final long SLOT = 199_716_706L;
    private static final String TX = "8ee8f76ee5c8c119bd1ee61344eb07f813b8318f0d62cc969ed89791f23497e4";
    private static final String OTHER_CREDENTIAL = "11".repeat(28);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private Cip113Configuration configuration;
    @Mock
    private Cip113RegistryNodeRepository repository;
    @Mock
    private InvalidTokenRecorder invalidTokenRecorder;

    private Cip113Processor processor;

    @BeforeEach
    void setUp() {
        when(configuration.isEnabled()).thenReturn(true);
        when(configuration.isRegistryPolicyId(any())).thenAnswer(i -> REGISTRY_POLICY_ID.equals(i.getArgument(0)));
        processor = new Cip113Processor(configuration, new Cip113RegistryNodeParser(), repository, invalidTokenRecorder, CLOCK);
    }

    @Test
    @DisplayName("stores a mainnet node with every field of its datum and the output that carries it")
    void storesNode() {
        processor.processTransaction(event(nodeOutput(TX, 3, 2, POLICY_01C2, POLICY_01C2_DATUM)));

        Cip113RegistryNode node = singleSaved();
        assertThat(node.getKey()).isEqualTo(POLICY_01C2);
        assertThat(node.getRegistryPolicyId()).isEqualTo(REGISTRY_POLICY_ID);
        assertThat(node.getNext()).isEqualTo("b025efe5b44b43ed154419c66b0efc9cf148fd98f464e261018c89e8");
        assertThat(node.getSlot()).isEqualTo(SLOT);
        assertThat(node.getTxHash()).isEqualTo(TX);
        assertThat(node.getTxIndex()).isEqualTo(3);
        assertThat(node.getOutputIndex()).isEqualTo(2);
        assertThat(node.getMintingLogicScript()).isEqualTo("4e9ba4785eebbeb77fe694c8451f1abec411f7774c09bbacfa3ba1ac");
        assertThat(node.getTransferLogicScript()).isEqualTo("ddf0363f0d73f54778e792e16bad802df5fa50061319b1f01d280a37");
        assertThat(node.getThirdPartyTransferLogicScript()).isEqualTo("446fc9584df8316cfab440c60d140e2007f236cdb65180917a374fb8");
        assertThat(node.getUnfrackingLogicScript()).isNull();
        assertThat(node.getGlobalStatePolicyId()).isEqualTo("218ba0c5b7156b13c51e41096e34f0310823adf0e0d3fc70a741f3c2");
        assertThat(node.getDatum()).isEqualTo(POLICY_01C2_DATUM);
        assertThat(node.getLastSyncedAt()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 0));
        verifyNoInteractions(invalidTokenRecorder);
    }

    @Test
    @DisplayName("stores the origin node, whose key and token name are empty")
    void storesOriginNode() {
        processor.processTransaction(event(nodeOutput(TX, 0, 1, "", ORIGIN_DATUM)));

        Cip113RegistryNode node = singleSaved();
        assertThat(node.getKey()).isEmpty();
        assertThat(node.getNext()).isEqualTo(POLICY_01C2);
        assertThat(node.getMintingLogicScript()).isNull();
        assertThat(node.getGlobalStatePolicyId()).isNull();
    }

    @Test
    @DisplayName("ignores an output without a registry NFT, even at the registry address")
    void ignoresOutputWithoutRegistryNft() {
        String otherPolicy = "22".repeat(28);
        processor.processTransaction(event(output(TX, 0, 0, REGISTRY_POLICY_ID, otherPolicy + POLICY_01C2, BigInteger.ONE,
                POLICY_01C2_DATUM)));

        verifyNoInteractions(repository, invalidTokenRecorder);
    }

    @Test
    @DisplayName("stores the nodes of every configured registry, each with its own registry")
    void storesTheRegistryOfEachNode() {
        String registryB = "bb".repeat(28);
        when(configuration.isRegistryPolicyId(any()))
                .thenAnswer(i -> REGISTRY_POLICY_ID.equals(i.getArgument(0)) || registryB.equals(i.getArgument(0)));

        processor.processTransaction(event(
                nodeOutput(TX, 0, 0, POLICY_01C2, POLICY_01C2_DATUM),
                output(TX, 0, 1, registryB, registryB + POLICY_01C2, BigInteger.ONE, POLICY_01C2_DATUM)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cip113RegistryNode>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Cip113RegistryNode::getRegistryPolicyId)
                .containsExactly(REGISTRY_POLICY_ID, registryB);
        verifyNoInteractions(invalidTokenRecorder);
    }

    @Test
    @DisplayName("ignores a registry policy asset held more than once: a node NFT is unique")
    void ignoresQuantityAboveOne() {
        processor.processTransaction(event(output(TX, 0, 0, REGISTRY_POLICY_ID, REGISTRY_POLICY_ID + POLICY_01C2,
                BigInteger.TWO, POLICY_01C2_DATUM)));

        verifyNoInteractions(repository, invalidTokenRecorder);
    }

    @Test
    @DisplayName("does nothing when CIP-113 has no registry")
    void disabled() {
        when(configuration.isEnabled()).thenReturn(false);

        processor.processTransaction(event(nodeOutput(TX, 0, 2, POLICY_01C2, POLICY_01C2_DATUM)));

        verifyNoInteractions(repository, invalidTokenRecorder);
    }

    @Test
    @DisplayName("records a node whose datum is not a registry node as registry_node_unparseable, and stores nothing")
    void unparseable() {
        processor.processTransaction(event(nodeOutput(TX, 0, 2, POLICY_01C2, "d8799f4040ff")));

        verify(repository, never()).saveAll(any());
        InvalidTokenReport report = singleReport();
        assertThat(report.standard()).isEqualTo(InvalidTokenReason.CIP_113);
        assertThat(report.reasonCode()).isEqualTo(InvalidTokenReason.REGISTRY_NODE_UNPARSEABLE);
        assertThat(report.policyId()).isEqualTo(REGISTRY_POLICY_ID);
        assertThat(report.assetName()).isEqualTo(POLICY_01C2);
        assertThat(report.slot()).isEqualTo(SLOT);
        assertThat(report.txHash()).isEqualTo(TX);
        assertThat(report.outputIndex()).isEqualTo(2);
        assertThat(report.details()).containsEntry("datum_prefix", "d8799f4040ff");
    }

    @Test
    @DisplayName("records a registry NFT outside the registry's script address as registry_node_spoofed")
    void spoofedAddress() {
        processor.processTransaction(event(output(TX, 0, 0, OTHER_CREDENTIAL, REGISTRY_POLICY_ID + POLICY_01C2,
                BigInteger.ONE, POLICY_01C2_DATUM)));

        verify(repository, never()).saveAll(any());
        assertThat(singleReport().reasonCode()).isEqualTo(InvalidTokenReason.REGISTRY_NODE_SPOOFED);
    }

    @Test
    @DisplayName("records a datum whose key is not the token name of the NFT as registry_node_spoofed")
    void spoofedKey() {
        processor.processTransaction(event(nodeOutput(TX, 0, 0, POLICY_B6F4, POLICY_01C2_DATUM)));

        verify(repository, never()).saveAll(any());
        InvalidTokenReport report = singleReport();
        assertThat(report.reasonCode()).isEqualTo(InvalidTokenReason.REGISTRY_NODE_SPOOFED);
        assertThat(report.assetName()).isEqualTo(POLICY_B6F4);
    }

    @Test
    @DisplayName("stores the nodes of a transaction that inserts one: the covering node and the new node")
    void storesEveryNodeOfTheBlock() {
        processor.processTransaction(event(
                nodeOutput(TX, 0, 0, POLICY_01C2, POLICY_01C2_DATUM),
                nodeOutput(TX, 0, 1, POLICY_B6F4, POLICY_B6F4_DATUM)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cip113RegistryNode>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Cip113RegistryNode::getKey).containsExactly(POLICY_01C2, POLICY_B6F4);
    }

    @Test
    @DisplayName("a failure on one output is recorded as processing_failure, and the other outputs are still stored")
    void failureOnOneOutputDoesNotStopTheOthers() {
        Cip113RegistryNodeParser parser = spy(new Cip113RegistryNodeParser());
        doThrow(new IllegalStateException("boom")).when(parser).parseDetailed(POLICY_B6F4_DATUM);
        processor = new Cip113Processor(configuration, parser, repository, invalidTokenRecorder, CLOCK);

        processor.processTransaction(event(
                nodeOutput(TX, 0, 0, POLICY_B6F4, POLICY_B6F4_DATUM),
                nodeOutput(TX, 0, 1, POLICY_01C2, POLICY_01C2_DATUM)));

        assertThat(singleSaved().getKey()).isEqualTo(POLICY_01C2);
        assertThat(singleReport().reasonCode()).isEqualTo(InvalidTokenReason.PROCESSING_FAILURE);
    }

    private Cip113RegistryNode singleSaved() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Cip113RegistryNode>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).hasSize(1);
        return saved.getValue().getFirst();
    }

    private InvalidTokenReport singleReport() {
        ArgumentCaptor<InvalidTokenReport> report = ArgumentCaptor.forClass(InvalidTokenReport.class);
        verify(invalidTokenRecorder).report(report.capture());
        return report.getValue();
    }

    private static AddressUtxoEvent event(AddressUtxo... outputs) {
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(SLOT).build())
                .txInputOutputs(List.of(TxInputOutput.builder().txHash(TX).outputs(List.of(outputs)).build()))
                .build();
    }
}
