package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.yaci.store.submit.controller.TxUtilController;
import com.bloxbean.cardano.yaci.store.submit.domain.OgmiosUtxo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScalusAdditionalUtxoMapperTest {
    private static final String PLUTUS_V1_CBOR = "4e4d01000033222220051200120011";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromAdditionalUtxoSetAcceptsBlockfrostTupleShape() throws Exception {
        String requestJson = """
                {
                  "cbor": "abcd",
                  "additionalUtxoSet": [
                    [
                      { "txId": "tx2", "index": 1 },
                      {
                        "address": "addr_test1...",
                        "value": {
                          "coins": "1000000",
                          "assets": {
                            "policyasset": "7"
                          }
                        },
                        "datumHash": "datum-hash",
                        "datum": "d87980"
                      }
                    ]
                  ]
                }
                """;
        TxUtilController.EvaluateRequest request = objectMapper.readValue(requestJson, TxUtilController.EvaluateRequest.class);

        var cclUtxos = ScalusAdditionalUtxoMapper.fromAdditionalUtxoSet(
                OgmiosUtxo.fromAdditionalUtxoSet(request.getAdditionalUtxoSet()), new ReferenceScriptSupplier());

        assertThat(cclUtxos).singleElement().satisfies(cclUtxo -> {
            assertThat(cclUtxo.getTxHash()).isEqualTo("tx2");
            assertThat(cclUtxo.getOutputIndex()).isEqualTo(1);
            assertThat(cclUtxo.getDataHash()).isEqualTo("datum-hash");
            assertThat(cclUtxo.getInlineDatum()).isEqualTo("d87980");
            assertThat(cclUtxo.getAmount())
                    .anySatisfy(amount -> {
                        assertThat(amount.getUnit()).isEqualTo("lovelace");
                        assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(1_000_000));
                    })
                    .anySatisfy(amount -> {
                        assertThat(amount.getUnit()).isEqualTo("policyasset");
                        assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(7));
                    });
        });
    }

    @Test
    void fromAdditionalUtxoSetAcceptsOgmiosTupleShapeAndRegistersPlutusScript() throws Exception {
        String policyId = "a".repeat(56);
        String assetName = "74657374";
        String additionalUtxosJson = """
                [
                  [
                    {
                      "transaction": { "id": "tx3" },
                      "output": { "index": 3 }
                    },
                    {
                      "address": "addr_test1...",
                      "value": {
                        "ada": { "lovelace": 10000000 },
                        "%s": { "%s": 42 }
                      },
                      "datum": "d87980",
                      "script": {
                        "language": "plutus:v1",
                        "cbor": "%s"
                      }
                    }
                  ]
                ]
                """.formatted(policyId, assetName, PLUTUS_V1_CBOR);
        List<OgmiosUtxo> additionalUtxos = OgmiosUtxo.fromAdditionalUtxoSet(objectMapper.readTree(additionalUtxosJson));
        ReferenceScriptSupplier scriptSupplier = new ReferenceScriptSupplier();

        var cclUtxos = ScalusAdditionalUtxoMapper.fromAdditionalUtxoSet(additionalUtxos, scriptSupplier);

        assertThat(cclUtxos).singleElement().satisfies(cclUtxo -> {
            assertThat(cclUtxo.getTxHash()).isEqualTo("tx3");
            assertThat(cclUtxo.getOutputIndex()).isEqualTo(3);
            assertThat(cclUtxo.getInlineDatum()).isEqualTo("d87980");
            assertThat(cclUtxo.getReferenceScriptHash()).isNotBlank();
            assertThat(scriptSupplier.getScript(cclUtxo.getReferenceScriptHash()).getCborHex()).isEqualTo(PLUTUS_V1_CBOR);
            assertThat(cclUtxo.getAmount())
                    .anySatisfy(amount -> {
                        assertThat(amount.getUnit()).isEqualTo("lovelace");
                        assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(10_000_000));
                    })
                    .anySatisfy(amount -> {
                        assertThat(amount.getUnit()).isEqualTo(policyId + assetName);
                        assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(42));
                    });
        });
    }

    @Test
    void fromAdditionalUtxoSetAcceptsFlatOgmiosUtxoShape() throws Exception {
        String flatUtxoJson = """
                [
                  {
                    "transaction": { "id": "tx5" },
                    "index": 5,
                    "address": "addr_test1...",
                    "value": {
                      "lovelace": 2000000,
                      "policy.asset": "11"
                    },
                    "script": {
                      "plutus:v1": "%s"
                    }
                  }
                ]
                """.formatted(PLUTUS_V1_CBOR);
        List<OgmiosUtxo> additionalUtxos = OgmiosUtxo.fromAdditionalUtxoSet(objectMapper.readTree(flatUtxoJson));
        ReferenceScriptSupplier scriptSupplier = new ReferenceScriptSupplier();

        var cclUtxos = ScalusAdditionalUtxoMapper.fromAdditionalUtxoSet(additionalUtxos, scriptSupplier);

        assertThat(cclUtxos).singleElement().satisfies(cclUtxo -> {
            assertThat(cclUtxo.getTxHash()).isEqualTo("tx5");
            assertThat(cclUtxo.getOutputIndex()).isEqualTo(5);
            assertThat(cclUtxo.getReferenceScriptHash()).isNotBlank();
            assertThat(scriptSupplier.getScript(cclUtxo.getReferenceScriptHash()).getCborHex()).isEqualTo(PLUTUS_V1_CBOR);
            assertThat(cclUtxo.getAmount())
                    .anySatisfy(amount -> {
                        assertThat(amount.getUnit()).isEqualTo("lovelace");
                        assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(2_000_000));
                    })
                    .anySatisfy(amount -> {
                        assertThat(amount.getUnit()).isEqualTo("policyasset");
                        assertThat(amount.getQuantity()).isEqualTo(BigInteger.valueOf(11));
                    });
        });
    }

    @Test
    void fromAdditionalUtxoSetRejectsMissingRequiredFields() throws Exception {
        assertThatThrownBy(() -> OgmiosUtxo.fromAdditionalUtxoSet(objectMapper.readTree("""
                [[{"index": 0}, {"address": "addr_test1...", "value": {"coins": 1}}]]
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transaction id");

        assertThatThrownBy(() -> OgmiosUtxo.fromAdditionalUtxoSet(objectMapper.readTree("""
                [[{"txId": "tx4"}, {"address": "addr_test1...", "value": {"coins": 1}}]]
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("index");

        assertThatThrownBy(() -> OgmiosUtxo.fromAdditionalUtxoSet(objectMapper.readTree("""
                [[{"txId": "tx4", "index": 0}, {"value": {"coins": 1}}]]
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("address");

        assertThatThrownBy(() -> OgmiosUtxo.fromAdditionalUtxoSet(objectMapper.readTree("""
                [[{"txId": "tx4", "index": 0}, {"address": "addr_test1..."}]]
                """)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("value");
    }
}
