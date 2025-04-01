package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.certs.Certificate;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.types.UnitInterval;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRegistrationEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.event.PoolRetirementEvent;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolCertificateStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.UnitIntervalUtil.safeRatio;
import static com.bloxbean.cardano.yaci.store.staking.StakingStoreConfiguration.STORE_STAKING_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_STAKING_ENABLED)
@Slf4j
public class PoolRegistrationProcessor {
    private final PoolCertificateStorage poolStorage;
    private final ApplicationEventPublisher publisher;
    private final StoreProperties storeProperties;

    @EventListener
    @Transactional
    public void processPoolRegistration(CertificateEvent certificateEvent) {
        EventMetadata eventMetadata = certificateEvent.getMetadata();

        for (TxCertificates txCertificates: certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();
            int txIndex = txCertificates.getTxIndex();
            List<Certificate> certificates = txCertificates.getCertificates();

            List<PoolRegistration> poolRegistrations = new ArrayList<>();
            List<PoolRetirement> poolRetirements = new ArrayList<>();

            int index = 0;
            for (Certificate certificate: certificates) {
                if (certificate.getType() == CertificateType.POOL_REGISTRATION) {
                    com.bloxbean.cardano.yaci.core.model.certs.PoolRegistration poolRegistrationCert
                            = (com.bloxbean.cardano.yaci.core.model.certs.PoolRegistration) certificate;

                    Address rewardAddress = new Address(HexUtil.decodeHexString(poolRegistrationCert.getPoolParams().getRewardAccount()));
                    String rewardAddressBech32 = rewardAddress.toBech32();
                    if (storeProperties.isMainnet() && rewardAddressBech32.startsWith("stake_test")) { //testnet addr in mainnet
                        //convert it to mainnet address
                        var stakeCred = rewardAddress.getDelegationCredential().orElse(null);
                        if (stakeCred != null) {
                            rewardAddressBech32 = AddressProvider.getRewardAddress(stakeCred, Networks.mainnet()).toBech32();
                        }
                    }

                    //This value is just for display purpose.
                    //For calc like reward calc, numerator and denominator are used to avoid rounding issue
                    UnitInterval marginUnitInterval = poolRegistrationCert.getPoolParams().getMargin();
                    double margin = safeRatio(marginUnitInterval).doubleValue();

                    PoolRegistration poolRegistration = PoolRegistration.builder()
                            .txHash(txHash)
                            .certIndex(index)
                            .txIndex(txIndex)
                            .poolId(poolRegistrationCert.getPoolParams().getOperator())
                            .vrfKeyHash(poolRegistrationCert.getPoolParams().getVrfKeyHash())
                            .pledge(poolRegistrationCert.getPoolParams().getPledge())
                            .cost(poolRegistrationCert.getPoolParams().getCost())
                            .margin(margin)
                            .marginNumerator(marginUnitInterval.getNumerator())
                            .marginDenominator(marginUnitInterval.getDenominator())
                            .rewardAccount(rewardAddressBech32)
                            .poolOwners(poolRegistrationCert.getPoolParams().getPoolOwners())
                            .relays(poolRegistrationCert.getPoolParams().getRelays())
                            .metadataUrl(poolRegistrationCert.getPoolParams().getPoolMetadataUrl())
                            .metadataHash(poolRegistrationCert.getPoolParams().getPoolMetadataHash())
                            .epoch(eventMetadata.getEpochNumber())
                            .slot(eventMetadata.getSlot())
                            .blockNumber(eventMetadata.getBlock())
                            .blockHash(eventMetadata.getBlockHash())
                            .blockTime(eventMetadata.getBlockTime())
                            .build();

                    poolRegistrations.add(poolRegistration);
                } else if (certificate.getType() == CertificateType.POOL_RETIREMENT) {
                    com.bloxbean.cardano.yaci.core.model.certs.PoolRetirement poolRetirementCert = (com.bloxbean.cardano.yaci.core.model.certs.PoolRetirement) certificate;

                    PoolRetirement poolRetirement = PoolRetirement.builder()
                            .txHash(txHash)
                            .certIndex(index)
                            .txIndex(txIndex)
                            .poolId(poolRetirementCert.getPoolKeyHash())
                            .retirementEpoch((int)poolRetirementCert.getEpoch())
                            .epoch(eventMetadata.getEpochNumber())
                            .slot(eventMetadata.getSlot())
                            .blockNumber(eventMetadata.getBlock())
                            .blockHash(eventMetadata.getBlockHash())
                            .blockTime(eventMetadata.getBlockTime())
                            .build();

                    poolRetirements.add(poolRetirement);
                }
                index++;
            }

            if (poolRegistrations.size() > 0)
                poolStorage.savePoolRegistrations(poolRegistrations);
            if (poolRetirements.size() > 0)
                poolStorage.savePoolRetirements(poolRetirements);

            //publish events
            if (poolRegistrations.size() > 0)
                publisher.publishEvent(new PoolRegistrationEvent(eventMetadata, poolRegistrations));

            if (poolRetirements.size() > 0)
                publisher.publishEvent(new PoolRetirementEvent(eventMetadata, poolRetirements));
        }
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = poolStorage.deleteRegistrationsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} pool_registration records", count);

        count = poolStorage.deleteRetirementsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} pool_retirement records", count);
    }
}
