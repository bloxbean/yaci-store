package com.bloxbean.cardano.yaci.store.epoch.processor;

import com.bloxbean.cardano.yaci.store.epoch.service.LocalProtocolParamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalProtocolParamsSchedulerTest {
    @Mock
    private LocalProtocolParamService protocolParamService;

    @InjectMocks
    private LocalProtocolParamsSchduler localProtocolParamsSchduler;

    @Test
    void testScheduleFetchAndSetCurrentProtocolParams() {
        localProtocolParamsSchduler = new LocalProtocolParamsSchduler(protocolParamService);

        localProtocolParamsSchduler.scheduleFetchAndSetCurrentProtocolParams();
        Mockito.verify(protocolParamService, Mockito.times(1)).fetchAndSetCurrentProtocolParams();
    }

}
