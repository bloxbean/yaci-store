package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.PoolParams;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.events.GenesisBalance;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.GenesisStaking;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorage;
import org.cardanofoundation.rewards.calculation.domain.EpochCalculationResult;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenesisPoolProcessorTest {
    private static final BigInteger POOL_DEPOSIT = BigInteger.valueOf(500_000_000L);
    private static final BigInteger KEY_DEPOSIT = BigInteger.valueOf(2_000_000L);
    private static final String POOL_ID = "7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57";
    private static final String POOL_ID_2 = "8301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc58";
    private static final String STAKE_HASH = "295b987135610616f3c74e11c94d77b6ced5ccc93a7d719cfb135062";
    private static final String STAKE_HASH_2 = "395b987135610616f3c74e11c94d77b6ced5ccc93a7d719cfb135063";

    @Mock
    private StoreProperties storeProperties;
    @Mock
    private PoolStorage poolStorage;
    @Mock
    private PoolCertificateStorage poolCertificateStorage;
    @Mock
    private StakingCertificateStorage stakingCertificateStorage;
    @Mock
    private GenesisConfig genesisConfig;
    @Mock
    private AdaPotService adaPotService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DSLContext dslContext;
    @Mock
    private DepositParamService depositParamService;

    @InjectMocks
    private GenesisPoolProcessor genesisPoolProcessor;

    @Captor
    private ArgumentCaptor<List<PoolRegistration>> poolRegistrationsCaptor;
    @Captor
    private ArgumentCaptor<List<Pool>> poolsCaptor;
    @Captor
    private ArgumentCaptor<List<Delegation>> delegationsCaptor;
    @Captor
    private ArgumentCaptor<List<StakeRegistrationDetail>> stakeRegistrationsCaptor;
    @Captor
    private ArgumentCaptor<EpochCalculationResult> epochCalculationResultCaptor;

    @Test
    void handleGenesisPoolRegistration_booksPoolDepositsAndStakeRegistrations() {
        when(storeProperties.isMainnet()).thenReturn(false);
        when(depositParamService.getPoolDeposit(0)).thenReturn(POOL_DEPOSIT);

        genesisPoolProcessor.handleGenesisPoolRegistration(genesisBlockEvent(genesisStaking()));

        verify(poolCertificateStorage).savePoolRegistrations(poolRegistrationsCaptor.capture());
        assertThat(poolRegistrationsCaptor.getValue())
                .extracting(PoolRegistration::getCertIndex)
                .containsExactly(0, 1);

        verify(poolStorage).save(poolsCaptor.capture());
        assertThat(poolsCaptor.getValue())
                .extracting(Pool::getAmount)
                .containsExactly(POOL_DEPOSIT, POOL_DEPOSIT);
        assertThat(poolsCaptor.getValue())
                .extracting(Pool::getStatus)
                .containsOnly(PoolStatusType.REGISTRATION);

        verify(stakingCertificateStorage).saveDelegations(delegationsCaptor.capture());
        assertThat(delegationsCaptor.getValue())
                .extracting(Delegation::getCertIndex)
                .containsExactly(0, 1);

        verify(stakingCertificateStorage).saveRegistrations(stakeRegistrationsCaptor.capture());
        assertThat(stakeRegistrationsCaptor.getValue())
                .extracting(StakeRegistrationDetail::getCertIndex)
                .containsExactly(0, 1);
        assertThat(stakeRegistrationsCaptor.getValue())
                .extracting(StakeRegistrationDetail::getType)
                .containsOnly(CertificateType.STAKE_REGISTRATION);
        assertThat(stakeRegistrationsCaptor.getValue())
                .extracting(StakeRegistrationDetail::getTxHash)
                .containsOnly("Genesis");
    }

    @Test
    void handleGenesisEpochStake_booksGenesisDepositsIntoEpochZeroWithoutReducingReserves() {
        when(storeProperties.isMainnet()).thenReturn(false);
        when(depositParamService.getPoolDeposit(0)).thenReturn(POOL_DEPOSIT);
        when(depositParamService.getKeyDeposit(0)).thenReturn(KEY_DEPOSIT);
        when(genesisConfig.getMaxLovelaceSupply()).thenReturn(BigInteger.valueOf(20_000_000_000L));
        when(dslContext.batch(any(Collection.class)).execute()).thenReturn(new int[]{1, 1});

        genesisPoolProcessor.handleGenesisEpochStake(genesisBlockEvent(genesisStaking()));

        BigInteger expectedGenesisDeposits = POOL_DEPOSIT.multiply(BigInteger.valueOf(2))
                .add(KEY_DEPOSIT.multiply(BigInteger.valueOf(2)));
        verify(adaPotService).createAdaPot(0, 0L);
        verify(adaPotService).updateAdaPotDeposit(0, expectedGenesisDeposits);
        verify(adaPotService).updateAdaPot(org.mockito.ArgumentMatchers.eq(0), epochCalculationResultCaptor.capture());
        assertThat(epochCalculationResultCaptor.getValue().getReserves())
                .isEqualTo(BigInteger.valueOf(20_000_000_000L)
                        .subtract(BigInteger.valueOf(3_000_000_000L)));
    }

    @Test
    void handleGenesisEpochStake_emptyStakingKeepsPublicNetworkBehaviour() {
        when(genesisConfig.getMaxLovelaceSupply()).thenReturn(BigInteger.valueOf(20_000_000_000L));

        genesisPoolProcessor.handleGenesisEpochStake(genesisBlockEvent(new GenesisStaking(List.of(), List.of())));

        verify(adaPotService).createAdaPot(0, 0L);
        verify(adaPotService).updateAdaPotDeposit(0, BigInteger.ZERO);
        verify(adaPotService).updateAdaPot(org.mockito.ArgumentMatchers.eq(0), epochCalculationResultCaptor.capture());
        assertThat(epochCalculationResultCaptor.getValue().getReserves())
                .isEqualTo(BigInteger.valueOf(17_000_000_000L));
        verifyNoInteractions(depositParamService);
        verify(poolStorage, never()).save(any());
        verify(stakingCertificateStorage, never()).saveRegistrations(any());
        verify(stakingCertificateStorage, never()).saveDelegations(any());
    }

    private GenesisBlockEvent genesisBlockEvent(GenesisStaking genesisStaking) {
        return GenesisBlockEvent.builder()
                .era(Era.Shelley)
                .epoch(0)
                .slot(-1)
                .block(-1)
                .blockHash("genesis-hash")
                .blockTime(1_700_000_000L)
                .genesisStaking(genesisStaking)
                .genesisBalances(List.of(
                        new GenesisBalance("addr_test1vr8utk9ldjke5c6kjuvjs23rzuw2n9ptdzqlr36qa74pt4q04dv2q", "tx1", BigInteger.valueOf(1_000_000_000L)),
                        new GenesisBalance("addr_test1vqq2fxv2umyhttkxyxp8x0dlpdt3k6cwng5pxj3vty2x3tq9ys9fp", "tx2", BigInteger.valueOf(2_000_000_000L))))
                .build();
    }

    private GenesisStaking genesisStaking() {
        return new GenesisStaking(
                List.of(poolParams(POOL_ID, "e011a14edf73b08a0a27cb98b2c57eb37c780df18fcfcf6785ed5df84a"),
                        poolParams(POOL_ID_2, "e012a14edf73b08a0a27cb98b2c57eb37c780df18fcfcf6785ed5df84b")),
                List.of(new GenesisStaking.Stake(STAKE_HASH, POOL_ID),
                        new GenesisStaking.Stake(STAKE_HASH_2, POOL_ID_2)));
    }

    private PoolParams poolParams(String poolId, String rewardAccount) {
        return PoolParams.builder()
                .operator(poolId)
                .vrfKeyHash("c2b62ffa92ad18ffc117ea3abeb161a68885000a466f9c71db5e4731d6630061")
                .pledge(BigInteger.ZERO)
                .cost(BigInteger.valueOf(340_000_000L))
                .margin(UnitInterval.fromString("1/5"))
                .rewardAccount(rewardAccount)
                .poolOwners(Set.of())
                .relays(List.of())
                .build();
    }
}
