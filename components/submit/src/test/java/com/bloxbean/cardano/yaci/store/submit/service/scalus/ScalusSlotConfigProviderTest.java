package com.bloxbean.cardano.yaci.store.submit.service.scalus;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScalusSlotConfigProviderTest {
    private EraService eraService;
    private GenesisConfig genesisConfig;
    private ObjectProvider<EraService> eraServiceProvider;
    private ObjectProvider<GenesisConfig> genesisConfigProvider;

    @BeforeEach
    void setup() {
        eraService = mock(EraService.class);
        genesisConfig = mock(GenesisConfig.class);
        eraServiceProvider = mock(ObjectProvider.class);
        genesisConfigProvider = mock(ObjectProvider.class);

        when(eraServiceProvider.getIfAvailable()).thenReturn(eraService);
        when(genesisConfigProvider.getIfAvailable()).thenReturn(genesisConfig);
    }

    @Test
    void getSlotConfigDerivesMainnetCanonicalValuesFromEraAndGenesisServices() throws Exception {
        when(eraService.shelleyEraStartTime()).thenReturn(1_596_059_091L);
        when(eraService.getFirstNonByronSlot()).thenReturn(4_492_800L);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);

        var slotConfig = new ScalusSlotConfigProvider(eraServiceProvider, genesisConfigProvider).getSlotConfig();

        assertThat(slotConfig.zeroTime()).isEqualTo(1_596_059_091_000L);
        assertThat(slotConfig.zeroSlot()).isEqualTo(4_492_800L);
        assertThat(slotConfig.slotLength()).isEqualTo(1_000L);
    }

    @Test
    void getSlotConfigCachesDerivedValue() throws Exception {
        when(eraService.shelleyEraStartTime()).thenReturn(1_596_059_091L);
        when(eraService.getFirstNonByronSlot()).thenReturn(4_492_800L);
        when(genesisConfig.slotDuration(Era.Shelley)).thenReturn(1.0);
        var provider = new ScalusSlotConfigProvider(eraServiceProvider, genesisConfigProvider);

        var first = provider.getSlotConfig();
        var second = provider.getSlotConfig();

        assertThat(second).isSameAs(first);
        verify(eraService, times(1)).shelleyEraStartTime();
        verify(eraService, times(1)).getFirstNonByronSlot();
        verify(genesisConfig, times(1)).slotDuration(Era.Shelley);
    }
}
