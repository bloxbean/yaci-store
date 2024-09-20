package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.epoch.service.LocalEpochParamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalEpochParamsSchedulerTest {
    @Mock
    private LocalEpochParamService protocolParamService;
    @Mock
    private StoreProperties storeProperties;

    @InjectMocks
    private LocalEpochParamsScheduler localProtocolParamsSchduler;

    @Test
    void testScheduleFetchAndSetCurrentProtocolParams_AutoSyncEnabled() {
        Mockito.when(storeProperties.isSyncAutoStart()).thenReturn(true);

        localProtocolParamsSchduler = new LocalEpochParamsScheduler(protocolParamService, storeProperties);

        localProtocolParamsSchduler.scheduleFetchAndSetCurrentProtocolParams();
        Mockito.verify(protocolParamService, Mockito.times(1)).fetchAndSetCurrentProtocolParams();
    }

    @Test
    void testScheduleFetchAndSetCurrentProtocolParams_AutoSyncDisabled() {
        Mockito.when(storeProperties.isSyncAutoStart()).thenReturn(false);
        Mockito.when(protocolParamService.getEra()).thenReturn(Era.Shelley);
        localProtocolParamsSchduler = new LocalEpochParamsScheduler(protocolParamService, storeProperties);
        
        localProtocolParamsSchduler.scheduleFetchAndSetCurrentProtocolParams();
        Mockito.verify(protocolParamService, Mockito.never()).fetchAndSetCurrentProtocolParams();
    }
}
