package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.processor;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.Cip113Configuration;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.entity.Cip113RegistryNode;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.parser.Cip113RegistryNodeParser;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.repository.Cip113RegistryNodeRepository;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip113.service.Cip113RegistryService;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cip113EventListener")
class Cip113EventListenerTest {

    private static final String REGISTRY_NFT_POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String REGISTERED_POLICY_ID = "deadbeefcafebabedeadbeefcafebabedeadbeefcafebabedeadbeef";
    private static final String TRANSFER_LOGIC = "1111111111111111111111111111111111111111111111111111111111";
    private static final String THIRD_PARTY_LOGIC = "2222222222222222222222222222222222222222222222222222222222";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    @Mock
    private Cip113RegistryNodeRepository repository;

    private Cip113Configuration config;
    private Cip113EventListener listener;

    @BeforeEach
    void setUp() {
        config = new Cip113Configuration();
        config.setRegistryNftPolicyIds(List.of(REGISTRY_NFT_POLICY_ID));
        config.init();

        Cip113RegistryNodeParser parser = new Cip113RegistryNodeParser();
        Cip113RegistryService registryService = new Cip113RegistryService(repository, config);
        listener = new Cip113EventListener(config, parser, repository, registryService);
    }

    @Nested
    @DisplayName("Valid registry node UTxOs")
    class ValidRegistryNode {

        @Test
        void savesEntityWithCorrectFields() throws Exception {
            String datum = buildRegistryNodeDatum(REGISTERED_POLICY_ID, "ffffffffffff",
                    TRANSFER_LOGIC, THIRD_PARTY_LOGIC, "");

            listener.processTransaction(buildEvent(100L, REGISTRY_NFT_POLICY_ID, REGISTERED_POLICY_ID, datum, TX_HASH));

            ArgumentCaptor<Cip113RegistryNode> captor = ArgumentCaptor.forClass(Cip113RegistryNode.class);
            verify(repository).save(captor.capture());

            Cip113RegistryNode saved = captor.getValue();
            assertThat(saved.getPolicyId()).isEqualTo(REGISTERED_POLICY_ID);
            assertThat(saved.getSlot()).isEqualTo(100L);
            assertThat(saved.getTxHash()).isEqualTo(TX_HASH);
            assertThat(saved.getTransferLogicScript()).isEqualTo(TRANSFER_LOGIC);
            assertThat(saved.getThirdPartyTransferLogicScript()).isEqualTo(THIRD_PARTY_LOGIC);
            assertThat(saved.getDatum()).isEqualTo(datum);
        }
    }

    @Nested
    @DisplayName("Skipped UTxOs")
    class SkippedUtxos {

        @Test
        void skipsWhenNoPolicyIdsConfigured() {
            config.setRegistryNftPolicyIds(List.of());
            config.init();
            listener.processTransaction(buildEvent(100L, REGISTRY_NFT_POLICY_ID, REGISTERED_POLICY_ID, "d8799f40ff", TX_HASH));
            verifyNoInteractions(repository);
        }

        @Test
        void skipsNonMatchingPolicyId() throws Exception {
            String otherPolicy = "9999999999999999999999999999999999999999999999999999999999";
            String datum = buildRegistryNodeDatum(REGISTERED_POLICY_ID, "ff", TRANSFER_LOGIC, THIRD_PARTY_LOGIC, "");
            listener.processTransaction(buildEvent(100L, otherPolicy, REGISTERED_POLICY_ID, datum, TX_HASH));
            verifyNoInteractions(repository);
        }

        @Test
        void skipsUtxoWithoutInlineDatum() {
            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .inlineDatum(null)
                    .amounts(List.of(Amt.builder()
                            .policyId(REGISTRY_NFT_POLICY_ID)
                            .unit(REGISTRY_NFT_POLICY_ID + REGISTERED_POLICY_ID)
                            .quantity(BigInteger.ONE).build()))
                    .build();

            AddressUtxoEvent event = AddressUtxoEvent.builder()
                    .metadata(EventMetadata.builder().slot(100L).build())
                    .txInputOutputs(List.of(TxInputOutput.builder()
                            .txHash(TX_HASH).outputs(List.of(utxo)).inputs(List.of()).build()))
                    .build();

            listener.processTransaction(event);
            verifyNoInteractions(repository);
        }

        @Test
        void skipsNftWithQuantityGreaterThanOne() throws Exception {
            String datum = buildRegistryNodeDatum(REGISTERED_POLICY_ID, "ff", TRANSFER_LOGIC, THIRD_PARTY_LOGIC, "");

            AddressUtxo utxo = AddressUtxo.builder()
                    .txHash(TX_HASH)
                    .inlineDatum(datum)
                    .amounts(List.of(Amt.builder()
                            .policyId(REGISTRY_NFT_POLICY_ID)
                            .unit(REGISTRY_NFT_POLICY_ID + REGISTERED_POLICY_ID)
                            .quantity(BigInteger.TWO).build()))
                    .build();

            AddressUtxoEvent event = AddressUtxoEvent.builder()
                    .metadata(EventMetadata.builder().slot(100L).build())
                    .txInputOutputs(List.of(TxInputOutput.builder()
                            .txHash(TX_HASH).outputs(List.of(utxo)).inputs(List.of()).build()))
                    .build();

            listener.processTransaction(event);
            verifyNoInteractions(repository);
        }

        @Test
        void skipsInvalidDatum() {
            listener.processTransaction(buildEvent(100L, REGISTRY_NFT_POLICY_ID, REGISTERED_POLICY_ID, "deadbeef", TX_HASH));
            verifyNoInteractions(repository);
        }
    }

    private AddressUtxoEvent buildEvent(long slot, String nftPolicyId, String nftAssetName,
                                         String inlineDatum, String txHash) {
        AddressUtxo utxo = AddressUtxo.builder()
                .txHash(txHash)
                .inlineDatum(inlineDatum)
                .amounts(List.of(Amt.builder()
                        .policyId(nftPolicyId)
                        .unit(nftPolicyId + nftAssetName)
                        .quantity(BigInteger.ONE).build()))
                .build();

        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
                .txInputOutputs(List.of(TxInputOutput.builder()
                        .txHash(txHash).outputs(List.of(utxo)).inputs(List.of()).build()))
                .build();
    }

    private static String buildRegistryNodeDatum(String key, String next,
                                                  String transferLogic, String thirdPartyLogic,
                                                  String globalState) throws Exception {
        ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                BytesPlutusData.of(HexUtil.decodeHexString(key)),
                BytesPlutusData.of(HexUtil.decodeHexString(next)),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(transferLogic))),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(thirdPartyLogic))),
                BytesPlutusData.of(globalState.isEmpty() ? new byte[0] : HexUtil.decodeHexString(globalState))
        );
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));
    }

}
