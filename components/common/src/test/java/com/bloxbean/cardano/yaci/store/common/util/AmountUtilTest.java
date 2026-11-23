package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.model.Amount;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static org.assertj.core.api.Assertions.assertThat;

class AmountUtilTest {
    private static final String POLICY = "10bebfc941207811e62882f02c0a69c0086f86186a4ef6ec1b5a2b0c";

    @Test
    void dropsZeroQuantityNativeAssetsOnly() {
        List<Amount> amounts = List.of(
                amount(LOVELACE, 2_000_000),
                amount(POLICY + ".54554e41", 0),
                amount(POLICY + ".4c4f5244", 5),
                amount(POLICY + ".4255524e", -3));

        assertThat(AmountUtil.withoutZeroQuantityAssets(amounts))
                .extracting(Amount::getUnit)
                .containsExactly(LOVELACE, POLICY + ".4c4f5244", POLICY + ".4255524e");
    }

    @Test
    void keepsZeroLovelace() {
        assertThat(AmountUtil.withoutZeroQuantityAssets(List.of(amount(LOVELACE, 0))))
                .extracting(Amount::getUnit)
                .containsExactly(LOVELACE);
    }

    @Test
    void nullListBecomesEmpty() {
        assertThat(AmountUtil.withoutZeroQuantityAssets(null)).isEmpty();
    }

    private Amount amount(String unit, long quantity) {
        return Amount.builder().unit(unit).quantity(BigInteger.valueOf(quantity)).build();
    }
}
