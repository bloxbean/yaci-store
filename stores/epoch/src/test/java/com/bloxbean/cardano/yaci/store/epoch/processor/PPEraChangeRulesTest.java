package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PPEraChangeRulesTest {

    private final PPEraChangeRules rules = new PPEraChangeRules();

    @Test
    void apply_directConwayStart_appliesAlonzoAndBabbageRules() {
        ProtocolParams pp = ProtocolParams.builder()
                .minUtxo(BigInteger.valueOf(1000000))
                .adaPerUtxoByte(BigInteger.valueOf(34482))
                .build();

        rules.apply(Era.Conway, null, pp);

        assertThat(pp.getMinUtxo()).isNull();
        assertThat(pp.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(4310)); //34482 / 8
    }

    @Test
    void apply_directBabbageStart_appliesAlonzoAndBabbageRules() {
        ProtocolParams pp = ProtocolParams.builder()
                .minUtxo(BigInteger.valueOf(1000000))
                .adaPerUtxoByte(BigInteger.valueOf(34482))
                .build();

        rules.apply(Era.Babbage, null, pp);

        assertThat(pp.getMinUtxo()).isNull();
        assertThat(pp.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(4310));
    }

    @Test
    void apply_directAlonzoStart_appliesOnlyAlonzoRule() {
        ProtocolParams pp = ProtocolParams.builder()
                .minUtxo(BigInteger.valueOf(1000000))
                .adaPerUtxoByte(BigInteger.valueOf(34482))
                .build();

        rules.apply(Era.Alonzo, null, pp);

        assertThat(pp.getMinUtxo()).isNull();
        //Word-based value is correct in Alonzo; must not be divided by 8 here
        assertThat(pp.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(34482));
    }

    @Test
    void apply_babbageFromAlonzoTransition_appliesBabbageRuleOnce() {
        ProtocolParams pp = ProtocolParams.builder()
                .adaPerUtxoByte(BigInteger.valueOf(34482))
                .build();

        rules.apply(Era.Babbage, Era.Alonzo, pp);

        assertThat(pp.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(4310));
    }

    @Test
    void apply_sameEra_isNoOp() {
        ProtocolParams pp = ProtocolParams.builder()
                .minUtxo(BigInteger.valueOf(1000000))
                .adaPerUtxoByte(BigInteger.valueOf(34482))
                .build();

        rules.apply(Era.Conway, Era.Conway, pp);

        assertThat(pp.getMinUtxo()).isEqualTo(BigInteger.valueOf(1000000));
        assertThat(pp.getAdaPerUtxoByte()).isEqualTo(BigInteger.valueOf(34482));
    }
}
