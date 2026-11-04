package com.bloxbean.cardano.yaci.store.blockfrost.transaction.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.BFTxOutputDto;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.BFTxUtxosDto;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model.TxUtxosRaw;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class BFTransactionMapperTest {
    private static final String TX_HASH = "4851475f3020f0a2fb5781aa308498cf7edbc4d6a8986d58f4f5bc39cc1c383c";

    private BFTransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BFTransactionMapperImpl();
        mapper.objectMapper = new ObjectMapper();
    }

    @Test
    void collateralReturnOfValidTx_isLovelaceOnly() {
        TxUtxosRaw raw = TxUtxosRaw.builder()
                .txInvalid(false)
                .inputs(List.of())
                .outputs(List.of())
                .collateralReturnRefJson("{\"tx_hash\":\"" + TX_HASH + "\",\"output_index\":2}")
                .collateralReturnDataJson("""
                        {"address":"addr_test1qz",
                         "amounts":[
                           {"unit":"lovelace","policy_id":null,"asset_name":"lovelace","quantity":2000000},
                           {"unit":"7e8bdaf2b2b919a3a4b94002cafb50086c0c845fe535d07a77ab7f77.6b7be97a","policy_id":"7e8bdaf2b2b919a3a4b94002cafb50086c0c845fe535d07a77ab7f77","asset_name":"k","quantity":1}
                         ],
                         "dataHash":null,"inlineDatum":null,"referenceScriptHash":null}
                        """)
                .build();

        BFTxUtxosDto dto = mapper.toUtxosDto(TX_HASH, raw);

        assertThat(dto.getOutputs()).hasSize(1);
        BFTxOutputDto collateralReturn = dto.getOutputs().get(0);
        assertThat(collateralReturn.getCollateral()).isTrue();
        assertThat(collateralReturn.getOutputIndex()).isEqualTo(2);
        assertThat(collateralReturn.getAmount())
                .extracting("unit", "quantity")
                .containsExactly(tuple("lovelace", "2000000"));
    }
}
