package com.bloxbean.cardano.yaci.store.blockfrost.pools.service;

import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolDelegatorDto;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolHistoryDto;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.BFPoolsStorageReader;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BFPoolsServiceTest {

    // Pass hex directly: BFPoolIdUtil.toHex(hex) is a passthrough for valid 56-char hex,
    // so mock expectations on storageReader use the exact same value.
    private static final String POOL_HEX     = "0f292fcaa02b8b2f9b3c8f9fd8e0bb21abedb692a6d5cc1de5f59424";
    private static final String UNKNOWN_HEX  = "11111111111111111111111111111111111111111111111111111111";

    @Mock private BFPoolsStorageReader storageReader;
    @Mock private StoreProperties storeProperties;
    @Mock private ObjectProvider<EpochStakeStorageReader> epochStakeStorageProvider;
    @Mock private EpochStakeStorageReader epochStakeStorageReader;

    private BFPoolsService service;

    @BeforeEach
    void setUp() {
        service = new BFPoolsService(storageReader, storeProperties, epochStakeStorageProvider);
    }

    // =========================================================================
    // getPoolHistory — adapot OFF path
    // =========================================================================

    @Test
    void givenAdapotDisabled_getPoolHistory_shouldCallBaseAndReturnNullAdapotFields() {
        // Given
        when(epochStakeStorageProvider.getIfAvailable()).thenReturn(null); // adapot OFF
        when(storageReader.getVrfKeyByPoolId(POOL_HEX)).thenReturn(Optional.of("vrfkey"));
        var baseRow = BFPoolHistoryDto.builder()
                .epoch(10).blocks(3).fees("100000")
                .build(); // activeStake/rewards/etc null
        when(storageReader.getPoolHistoryBase(eq(POOL_HEX), eq(0), eq(5), eq("asc")))
                .thenReturn(List.of(baseRow));

        // When
        List<BFPoolHistoryDto> result = service.getPoolHistory(POOL_HEX, 1, 5, "asc");

        // Then
        verify(storageReader, times(1)).getPoolHistoryBase(POOL_HEX, 0, 5, "asc");
        verify(storageReader, never()).getPoolHistoryFull(any(), anyInt(), anyInt(), any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEpoch()).isEqualTo(10);
        assertThat(result.get(0).getBlocks()).isEqualTo(3);
        assertThat(result.get(0).getFees()).isEqualTo("100000");
        assertThat(result.get(0).getActiveStake()).isNull();
        assertThat(result.get(0).getRewards()).isNull();
        assertThat(result.get(0).getActiveSize()).isNull();
        assertThat(result.get(0).getDelegatorsCount()).isNull();
    }

    // =========================================================================
    // getPoolHistory — adapot ON path
    // =========================================================================

    @Test
    void givenAdapotEnabled_getPoolHistory_shouldCallFullAndReturnAllFields() {
        // Given
        when(epochStakeStorageProvider.getIfAvailable()).thenReturn(epochStakeStorageReader); // adapot ON
        when(storageReader.getVrfKeyByPoolId(POOL_HEX)).thenReturn(Optional.of("vrfkey"));
        var fullRow = BFPoolHistoryDto.builder()
                .epoch(20).blocks(5).fees("200000")
                .activeStake("5000000000").activeSize(0.001).delegatorsCount(42).rewards("1000000")
                .build();
        when(storageReader.getPoolHistoryFull(eq(POOL_HEX), eq(0), eq(10), eq("desc")))
                .thenReturn(List.of(fullRow));

        // When
        List<BFPoolHistoryDto> result = service.getPoolHistory(POOL_HEX, 1, 10, "desc");

        // Then
        verify(storageReader, times(1)).getPoolHistoryFull(POOL_HEX, 0, 10, "desc");
        verify(storageReader, never()).getPoolHistoryBase(any(), anyInt(), anyInt(), any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActiveStake()).isEqualTo("5000000000");
        assertThat(result.get(0).getRewards()).isEqualTo("1000000");
        assertThat(result.get(0).getDelegatorsCount()).isEqualTo(42);
    }

    // =========================================================================
    // getPoolHistory — 404 for unknown pool
    // =========================================================================

    @Test
    void givenUnknownPool_getPoolHistory_shouldThrow404() {
        // Given
        when(storageReader.getVrfKeyByPoolId(UNKNOWN_HEX)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getPoolHistory(UNKNOWN_HEX, 1, 10, "asc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // =========================================================================
    // getPoolDelegators — adapot OFF path
    // =========================================================================

    @Test
    void givenAdapotDisabled_getPoolDelegators_shouldCallBaseAndReturnNullLiveStake() {
        // Given
        when(epochStakeStorageProvider.getIfAvailable()).thenReturn(null); // adapot OFF
        when(storageReader.getVrfKeyByPoolId(POOL_HEX)).thenReturn(Optional.of("vrfkey"));
        var baseRow = BFPoolDelegatorDto.builder().address("stake1uxyz...").build(); // liveStake null
        when(storageReader.getPoolDelegatorsBase(eq(POOL_HEX), eq(0), eq(5), eq("asc")))
                .thenReturn(List.of(baseRow));

        // When
        List<BFPoolDelegatorDto> result = service.getPoolDelegators(POOL_HEX, 1, 5, "asc");

        // Then
        verify(storageReader, times(1)).getPoolDelegatorsBase(POOL_HEX, 0, 5, "asc");
        verify(storageReader, never()).getPoolDelegatorsFull(any(), anyInt(), anyInt(), any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAddress()).isEqualTo("stake1uxyz...");
        assertThat(result.get(0).getLiveStake()).isNull();
    }

    // =========================================================================
    // getPoolDelegators — adapot ON path
    // =========================================================================

    @Test
    void givenAdapotEnabled_getPoolDelegators_shouldCallFullAndReturnLiveStake() {
        // Given
        when(epochStakeStorageProvider.getIfAvailable()).thenReturn(epochStakeStorageReader); // adapot ON
        when(storageReader.getVrfKeyByPoolId(POOL_HEX)).thenReturn(Optional.of("vrfkey"));
        var fullRow = BFPoolDelegatorDto.builder()
                .address("stake1uxyz...")
                .liveStake("50000000000")
                .build();
        when(storageReader.getPoolDelegatorsFull(eq(POOL_HEX), eq(0), eq(10), eq("asc")))
                .thenReturn(List.of(fullRow));

        // When
        List<BFPoolDelegatorDto> result = service.getPoolDelegators(POOL_HEX, 1, 10, "asc");

        // Then
        verify(storageReader, times(1)).getPoolDelegatorsFull(POOL_HEX, 0, 10, "asc");
        verify(storageReader, never()).getPoolDelegatorsBase(any(), anyInt(), anyInt(), any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLiveStake()).isEqualTo("50000000000");
    }

    // =========================================================================
    // getPoolDelegators — 404 for unknown pool
    // =========================================================================

    @Test
    void givenUnknownPool_getPoolDelegators_shouldThrow404() {
        // Given
        when(storageReader.getVrfKeyByPoolId(UNKNOWN_HEX)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getPoolDelegators(UNKNOWN_HEX, 1, 10, "asc"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // =========================================================================
    // Pagination: 1-based controller page → 0-based storage page
    // =========================================================================

    @Test
    void givenPage2_getPoolHistory_shouldPassPage1ToStorage() {
        // Given — page=2 should become p=1 in storage call
        when(epochStakeStorageProvider.getIfAvailable()).thenReturn(null);
        when(storageReader.getVrfKeyByPoolId(POOL_HEX)).thenReturn(Optional.of("vrfkey"));
        when(storageReader.getPoolHistoryBase(eq(POOL_HEX), eq(1), eq(10), eq("asc")))
                .thenReturn(List.of());

        // When
        service.getPoolHistory(POOL_HEX, 2, 10, "asc");

        // Then — storage was called with 0-based page=1
        verify(storageReader).getPoolHistoryBase(POOL_HEX, 1, 10, "asc");
    }

    @Test
    void givenPage3_getPoolDelegators_shouldPassPage2ToStorage() {
        // Given — page=3 should become p=2 in storage call
        when(epochStakeStorageProvider.getIfAvailable()).thenReturn(null);
        when(storageReader.getVrfKeyByPoolId(POOL_HEX)).thenReturn(Optional.of("vrfkey"));
        when(storageReader.getPoolDelegatorsBase(eq(POOL_HEX), eq(2), eq(5), eq("desc")))
                .thenReturn(List.of());

        // When
        service.getPoolDelegators(POOL_HEX, 3, 5, "desc");

        // Then
        verify(storageReader).getPoolDelegatorsBase(POOL_HEX, 2, 5, "desc");
    }
}
