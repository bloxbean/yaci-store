package com.bloxbean.cardano.yaci.store.epoch.processor;

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

    @InjectMocks
    private LocalEpochParamsScheduler localProtocolParamsSchduler;

    @Test
    void testScheduleFetchAndSetCurrentProtocolParams() {
        localProtocolParamsSchduler = new LocalEpochParamsScheduler(protocolParamService);

        localProtocolParamsSchduler.scheduleFetchAndSetCurrentProtocolParams();
        Mockito.verify(protocolParamService, Mockito.times(1)).fetchAndSetCurrentProtocolParams();
    }

}
