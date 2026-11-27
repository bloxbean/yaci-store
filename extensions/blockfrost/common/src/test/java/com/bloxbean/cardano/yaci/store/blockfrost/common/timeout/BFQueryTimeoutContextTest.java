package com.bloxbean.cardano.yaci.store.blockfrost.common.timeout;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BFQueryTimeoutContextTest {

    @AfterEach
    void tearDown() {
        BFQueryTimeoutContext.clear();
    }

    @Test
    void inactiveByDefault() {
        assertThat(BFQueryTimeoutContext.isActive()).isFalse();
        assertThat(BFQueryTimeoutContext.isTimedOut()).isFalse();

        // Marking without a budget (e.g. on an indexer thread) is a no-op
        BFQueryTimeoutContext.markTimedOut();
        assertThat(BFQueryTimeoutContext.isTimedOut()).isFalse();
    }

    @Test
    void remainingSeconds_roundsUp() {
        BFQueryTimeoutContext.begin(Duration.ofMillis(1500));

        assertThat(BFQueryTimeoutContext.remainingSeconds()).isEqualTo(2);
    }

    @Test
    void remainingSeconds_isAtLeastOneWhileBudgetLasts() {
        BFQueryTimeoutContext.begin(Duration.ofMillis(200));

        assertThat(BFQueryTimeoutContext.remainingSeconds()).isEqualTo(1);
    }

    @Test
    void remainingSeconds_throwsAndMarksOnceExhausted() throws InterruptedException {
        BFQueryTimeoutContext.begin(Duration.ofMillis(1));
        Thread.sleep(5);

        assertThatThrownBy(BFQueryTimeoutContext::remainingSeconds).isInstanceOf(QueryTimeoutException.class);
        assertThat(BFQueryTimeoutContext.isTimedOut()).isTrue();
    }

    @Test
    void clear_removesBudgetAndFlag() {
        BFQueryTimeoutContext.begin(Duration.ofSeconds(30));
        BFQueryTimeoutContext.markTimedOut();

        BFQueryTimeoutContext.clear();

        assertThat(BFQueryTimeoutContext.isActive()).isFalse();
        assertThat(BFQueryTimeoutContext.isTimedOut()).isFalse();
    }
}
