package com.bloxbean.cardano.yaci.store.staking.storage;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRegistrationEvent;
import com.bloxbean.cardano.yaci.store.staking.processor.PoolStatusProcessor;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * A custom PoolStorage written before findRecentPoolRegistrationBefore existed does not implement it.
 */
class PoolStorageCompatibilityTest {

    @Test
    void anOlderCustomStorageFailsWithAClearMessageInsteadOfUsingTheOldQuery() {
        // Calls the interface default for the method it does not implement
        PoolStorage oldStorage = mock(PoolStorage.class, CALLS_REAL_METHODS);
        var depositParamService = mock(DepositParamService.class);
        when(depositParamService.getPoolDeposit(anyInt())).thenReturn(BigInteger.valueOf(500_000_000));
        var processor = new PoolStatusProcessor(oldStorage, depositParamService, mock(ApplicationEventPublisher.class));

        var metadata = EventMetadata.builder().slot(100L).epochNumber(1).blockHash("block-100").build();
        var certs = new ArrayList<>(List.of(PoolRegistration.builder()
                .poolId("pool1").txHash("tx1").txIndex(0).certIndex(0).slot(100L).build()));

        assertThatThrownBy(() -> processor.handlePoolRegistration(new PoolRegistrationEvent(metadata, certs)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("must implement PoolStorage.findRecentPoolRegistrationBefore");
        verify(oldStorage, never()).findRecentPoolRegistration(any(), any());
        verify(oldStorage, never()).save(any());
    }
}
