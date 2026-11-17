package com.bloxbean.cardano.yaci.store.common.util;

import com.bloxbean.cardano.yaci.core.types.NonNegativeInterval;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UnitIntervalUtilTest {

    @Test
    void toBigDecimal_exactDivision() {
        assertThat(UnitIntervalUtil.toBigDecimal(NonNegativeInterval.fromString("577/10000"))).isEqualTo(new BigDecimal("0.0577"));
        assertThat(UnitIntervalUtil.toBigDecimal(NonNegativeInterval.fromString("721/10000000"))).isEqualTo(new BigDecimal("0.0000721"));
        assertThat(UnitIntervalUtil.toBigDecimal(UnitInterval.fromString("3/10"))).isEqualTo(new BigDecimal("0.3"));
    }

    @Test
    void toBigDecimal_nonTerminating_roundsToScale10() {
        assertThat(UnitIntervalUtil.toBigDecimal(UnitInterval.fromString("2/3"))).isEqualTo(new BigDecimal("0.6666666667"));
        assertThat(UnitIntervalUtil.toBigDecimal(UnitInterval.fromString("1/30"))).isEqualTo(new BigDecimal("0.0333333333"));
    }

    @Test
    void toBigDecimal_null() {
        assertThat(UnitIntervalUtil.toBigDecimal(null)).isNull();
    }
}
