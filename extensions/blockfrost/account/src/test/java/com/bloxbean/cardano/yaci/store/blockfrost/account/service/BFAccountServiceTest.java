package com.bloxbean.cardano.yaci.store.blockfrost.account.service;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.yaci.store.blockfrost.account.dto.BFAccountRegistrationDto;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.BFAccountStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.account.storage.impl.model.AccountRegistration;
import com.bloxbean.cardano.yaci.store.client.epoch.EpochParamClient;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BFAccountServiceTest {
    private static final String STAKE_ADDRESS = "stake_test1uztg6yppa0t30rslkrneva5c9qju40rhndjnuy356kxw83s6n95nu";

    @Mock
    private BFAccountStorageReader storageReader;

    @Mock
    private ObjectProvider<EpochParamClient> epochParamClient;

    @Mock
    private EpochParamClient epochParamClientBean;

    @InjectMocks
    private BFAccountService service;

    @BeforeEach
    void setUp() {
        when(storageReader.findRegistrations(anyString(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(
                        new AccountRegistration("tx1", "STAKE_REGISTRATION", 100L, 1000L, 10L),
                        new AccountRegistration("tx2", "STAKE_DEREGISTRATION", 200L, 2000L, 20L)));
    }

    @Test
    void findRegistrations_WhenKeyDepositAvailable_SetsItOnRegistrationsOnly() {
        ProtocolParams protocolParams = new ProtocolParams();
        protocolParams.setKeyDeposit("2000000");

        when(epochParamClient.getIfAvailable()).thenReturn(epochParamClientBean);
        when(epochParamClientBean.getLatestProtocolParams()).thenReturn(Optional.of(protocolParams));

        List<BFAccountRegistrationDto> registrations =
                service.findRegistrations(STAKE_ADDRESS, 1, 100, Order.asc);

        assertThat(registrations)
                .extracting(BFAccountRegistrationDto::getAction, BFAccountRegistrationDto::getDeposit)
                .containsExactly(
                        Tuple.tuple("registered", "2000000"),
                        Tuple.tuple("deregistered", null));
    }

    @Test
    void findRegistrations_WhenProtocolParamsMissing_LeavesDepositNull() {
        when(epochParamClient.getIfAvailable()).thenReturn(epochParamClientBean);
        when(epochParamClientBean.getLatestProtocolParams()).thenReturn(Optional.empty());

        List<BFAccountRegistrationDto> registrations =
                service.findRegistrations(STAKE_ADDRESS, 1, 100, Order.asc);

        assertThat(registrations).extracting(BFAccountRegistrationDto::getDeposit).containsOnlyNulls();
    }

    @Test
    void findRegistrations_WhenEpochStoreDisabled_LeavesDepositNull() {
        when(epochParamClient.getIfAvailable()).thenReturn(null);

        List<BFAccountRegistrationDto> registrations =
                service.findRegistrations(STAKE_ADDRESS, 1, 100, Order.asc);

        assertThat(registrations).extracting(BFAccountRegistrationDto::getDeposit).containsOnlyNulls();
    }
}
