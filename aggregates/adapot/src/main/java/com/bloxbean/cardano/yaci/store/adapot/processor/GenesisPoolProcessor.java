package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.service.AdaPotService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.GenesisStaking;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.Pool;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolStatusType;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.EpochCalculationResult;
import org.jooq.DSLContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;

/**
 * This processor is only used for devnets. (e.g; YaciDevKit DevNet)
 * This processor helps to setup pools, delegation, epoch_stake and adapot entry for custom devnet when pool/stake entries
 * are part of shelley-genesis file.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenesisPoolProcessor {
    private final StoreProperties storeProperties;
    private final PoolStorage poolStorage;
    private final PoolCertificateStorage poolCertificateStorage;
    private final StakingCertificateStorage stakingCertificateStorage;
    private final GenesisConfig genesisConfig;
    private final AdaPotService adaPotService;
    private final DSLContext dslContext;

    @EventListener
    @Transactional
    public void handleGenesisPoolRegistration(GenesisBlockEvent genesisBlockEvent) {
        var genesisStaking = genesisBlockEvent.getGenesisStaking();
        if (genesisStaking == null)
            return;

        var poolParamsList = genesisStaking.getPools();
        if (poolParamsList == null)
            return;

        String genesisTxHash = "Genesis";

        //Handle Pool registration
        var poolRegistrations = poolParamsList.stream()
                .map(poolParams -> {
                    Address rewardAddress = new Address(HexUtil.decodeHexString(poolParams.getRewardAccount()));
                    String rewardAddressBech32 = rewardAddress.toBech32();

                    PoolRegistration poolRegistration = PoolRegistration.builder()
                            .txHash(genesisTxHash)
                            .certIndex(0)
                            .txIndex(0)
                            .poolId(poolParams.getOperator())
                            .vrfKeyHash(poolParams.getVrfKeyHash())
                            .pledge(poolParams.getPledge())
                            .cost(poolParams.getCost())
                            .margin(Double.parseDouble(poolParams.getMargin()))
                            .rewardAccount(rewardAddressBech32)
                            .poolOwners(poolParams.getPoolOwners())
                            .relays(poolParams.getRelays())
                            .metadataUrl(poolParams.getPoolMetadataUrl())
                            .metadataHash(poolParams.getPoolMetadataHash())
                            .epoch(genesisBlockEvent.getEpoch())
                            .slot(genesisBlockEvent.getSlot())
                            .blockNumber(genesisBlockEvent.getBlock())
                            .blockHash(genesisBlockEvent.getBlockHash())
                            .blockTime(genesisBlockEvent.getBlockTime())
                            .build();
                    return poolRegistration;
                }).toList();

        if (poolRegistrations != null && !poolRegistrations.isEmpty())
            poolCertificateStorage.savePoolRegistrations(poolRegistrations);

        //Pools table
        List<Pool> pools = poolParamsList.stream()
                .map(poolParams -> {
                    Pool pool = Pool.builder()
                            .poolId(poolParams.getOperator())
                            .txHash(genesisTxHash)
                            .certIndex(0)
                            .txIndex(0)
                            .status(PoolStatusType.REGISTRATION)
                            .amount(BigInteger.ZERO)
                            .epoch(genesisBlockEvent.getEpoch())
                            .activeEpoch(0)
                            .registrationSlot(genesisBlockEvent.getSlot()) //This is the registration slot
                            .slot(genesisBlockEvent.getSlot())
                            .blockNumber(genesisBlockEvent.getBlock())
                            .blockHash(genesisBlockEvent.getBlockHash())
                            .blockTime(genesisBlockEvent.getBlockTime())
                            .build();
                    return pool;
                }).toList();

        if (pools != null && !pools.isEmpty())
            poolStorage.save(pools);

        //Update delegations
        List<GenesisStaking.Stake> stakes = genesisStaking.getStakes();
        List<Delegation> delegations = stakes.stream()
                .map(stake -> {
                    Address address = AddressProvider.getRewardAddress(Credential.fromKey(stake.getStakeKeyHash()),
                            storeProperties.isMainnet() ? Networks.mainnet() : Networks.testnet());
                    Delegation delegation = Delegation.builder()
                            .credential(stake.getStakeKeyHash())
                            .credentialType(CredentialType.ADDR_KEYHASH)
                            .address(address.toBech32())
                            .slot(genesisBlockEvent.getSlot())
                            .txHash(genesisTxHash)
                            .certIndex(0)
                            .txIndex(0)
                            .poolId(stake.getPoolHash())
                            .epoch(genesisBlockEvent.getEpoch())
                            .slot(genesisBlockEvent.getSlot())
                            .blockNumber(genesisBlockEvent.getBlock())
                            .blockHash(genesisBlockEvent.getBlockHash())
                            .blockTime(genesisBlockEvent.getBlockTime())
                            .build();
                    return delegation;
                }).toList();

        if (delegations != null && !delegations.isEmpty()) {
            stakingCertificateStorage.saveDelegations(delegations);
        }
    }

    @EventListener
    @Transactional
    public void handleGenesisEpochStake(GenesisBlockEvent genesisBlockEvent) {
        var genesisStaking = genesisBlockEvent.getGenesisStaking();
        if (genesisStaking == null)
            return;

        var poolParamsList = genesisStaking.getPools();
        if (poolParamsList == null)
            return;

        List<GenesisStaking.Stake> stakes = genesisStaking.getStakes();
        List<EpochStake> epochStakes = stakes.stream()
                .map(stake -> {
                    String poolHash = stake.getPoolHash();
                    Address address = AddressProvider.getRewardAddress(Credential.fromKey(stake.getStakeKeyHash()),
                            storeProperties.isMainnet() ? Networks.mainnet() : Networks.testnet());

                    //Find genesis balance for the stake address
                    var stakeAddrGenesisBalanceAmt = genesisBlockEvent.getGenesisBalances()
                            .stream().filter(genesisBalance -> {
                                try {
                                    String genAddr = genesisBalance.getAddress();
                                    String stakeAddr = AddressProvider.getStakeAddress(new Address(genAddr)).toBech32();
                                    if (address.toBech32().equals(stakeAddr))
                                        return true;
                                } catch (Exception e) {

                                }
                                return false;
                            }).findFirst()
                            .map(genesisBalance -> genesisBalance.getBalance())
                            .orElse(BigInteger.ZERO);

                    EpochStake epochStake = EpochStake.builder()
                            .poolId(poolHash)
                            .address(address.toBech32())
                            .epoch(genesisBlockEvent.getEpoch() - 1)
                            .activeEpoch(genesisBlockEvent.getEpoch() + 1)
                            .delegationEpoch(genesisBlockEvent.getEpoch())
                            .amount(stakeAddrGenesisBalanceAmt)
                            .build();

                    return epochStake;
                }).toList();

        if (epochStakes != null && !epochStakes.isEmpty()) {
            var inserts = epochStakes.stream()
                    .map(epochStake -> {
                        return dslContext.insertInto(EPOCH_STAKE)
                                .set(EPOCH_STAKE.POOL_ID, epochStake.getPoolId())
                                .set(EPOCH_STAKE.ADDRESS, epochStake.getAddress())
                                .set(EPOCH_STAKE.EPOCH, epochStake.getEpoch())
                                .set(EPOCH_STAKE.ACTIVE_EPOCH, epochStake.getActiveEpoch())
                                .set(EPOCH_STAKE.DELEGATION_EPOCH, epochStake.getDelegationEpoch())
                                .set(EPOCH_STAKE.AMOUNT, epochStake.getAmount())
                                .onDuplicateKeyIgnore();
                    }).toList();

            dslContext.batch(inserts).execute();
        }

        //calculate reward for epoch 0
        if (genesisBlockEvent.getEra().getValue() >= Era.Shelley.value) {
            var totalInitialBalance = genesisBlockEvent.getGenesisBalances()
                    .stream()
                    .map(genesisBalance -> genesisBalance.getBalance())
                    .reduce(BigInteger::add).orElse(BigInteger.ZERO);

            var reserves = genesisConfig.getMaxLovelaceSupply().subtract(totalInitialBalance);

            EpochCalculationResult result = EpochCalculationResult.builder()
                    .treasury(BigInteger.ZERO)
                    .reserves(reserves)
                    .totalDistributedRewards(BigInteger.ZERO)
                    .totalPoolRewardsPot(BigInteger.ZERO)
                    .totalUndistributedRewards(BigInteger.ZERO)
                    .build();
            adaPotService.updateAdaPot(0, result);
        }
    }
}
