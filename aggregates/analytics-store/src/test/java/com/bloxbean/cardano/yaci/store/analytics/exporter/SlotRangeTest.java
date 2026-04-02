package com.bloxbean.cardano.yaci.store.analytics.exporter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlotRangeTest {

    @Test
    void shouldCreateValidSlotRange() {
        SlotRange range = new SlotRange(10, 20);

        assertThat(range.startSlot()).isEqualTo(10);
        assertThat(range.endSlot()).isEqualTo(20);
    }

    @Test
    void shouldAllowNegativeOneAsStartSlot_forGenesisData() {
        SlotRange range = new SlotRange(-1, 100);

        assertThat(range.startSlot()).isEqualTo(-1);
        assertThat(range.endSlot()).isEqualTo(100);
    }

    @Test
    void shouldCalculateSlotCount_withGenesisStartSlot() {
        SlotRange range = new SlotRange(-1, 100);

        assertThat(range.getSlotCount()).isEqualTo(101);
    }

    @Test
    void shouldCalculateSlotCount_withPositiveStartSlot() {
        SlotRange range = new SlotRange(10, 20);

        assertThat(range.getSlotCount()).isEqualTo(10);
    }

    @Test
    void shouldRejectStartSlotLessThanNegativeOne() {
        assertThatThrownBy(() -> new SlotRange(-2, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start slot cannot be less than -1");
    }

    @Test
    void shouldRejectNegativeEndSlot() {
        assertThatThrownBy(() -> new SlotRange(-1, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End slot cannot be negative");
    }

    @Test
    void shouldRejectEndSlotNotGreaterThanStartSlot() {
        assertThatThrownBy(() -> new SlotRange(10, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End slot must be greater than start slot");
    }

    @Test
    void shouldRejectEndSlotLessThanStartSlot() {
        assertThatThrownBy(() -> new SlotRange(20, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End slot must be greater than start slot");
    }
}
