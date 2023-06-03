package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingStorage;
import com.bloxbean.cardano.yaci.store.staking.util.AddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StakeRegProcessor {
    private final StakingStorage stakingStorage;

    @EventListener
    @Transactional
    public void processStakeRegistration(CertificateEvent certificateEvent) {
        EventMetadata eventMetadata = certificateEvent.getMetadata();

        for (TxCertificates txCertificates: certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();
            List<Certificate> certificates = txCertificates.getCertificates();

            List<StakeRegistrationDetail> stakeRegDeRegs = new ArrayList<>();
            List<Delegation> delegations = new ArrayList<>();

            int index = 0;
            for (Certificate certificate: certificates) {
                if (certificate.getType() == CertificateType.STAKE_REGISTRATION) {
                    StakeRegistration stakeRegistration = (StakeRegistration) certificate;
                    Address address =
                            AddressUtil.getRewardAddress(stakeRegistration.getStakeCredential(), eventMetadata.isMainnet());

                    StakeRegistrationDetail stakeRegDeReg = StakeRegistrationDetail.builder()
                            .credential(stakeRegistration.getStakeCredential().getHash())
                            .address(address.toBech32())
                            .slot(eventMetadata.getSlot())
                            .txHash(txHash)
                            .certIndex(index)
                            .type(CertificateType.STAKE_REGISTRATION)
                            .epoch(eventMetadata.getEpochNumber())
                            .slot(eventMetadata.getSlot())
                            .block(eventMetadata.getBlock())
                            .blockHash(eventMetadata.getBlockHash())
                            .blockTime(eventMetadata.getBlockTime())
                            .build();
                    stakeRegDeRegs.add(stakeRegDeReg);
                } else if (certificate.getType() == CertificateType.STAKE_DEREGISTRATION) {
                    StakeDeregistration stakeDeregistration = (StakeDeregistration) certificate;
                    Address address =
                            AddressUtil.getRewardAddress(stakeDeregistration.getStakeCredential(), eventMetadata.isMainnet());

                    StakeRegistrationDetail stakeRegDeReg = StakeRegistrationDetail.builder()
                            .credential(stakeDeregistration.getStakeCredential().getHash())
                            .address(address.toBech32())
                            .slot(eventMetadata.getSlot())
                            .txHash(txHash)
                            .certIndex(index)
                            .type(CertificateType.STAKE_DEREGISTRATION)
                            .epoch(eventMetadata.getEpochNumber())
                            .slot(eventMetadata.getSlot())
                            .block(eventMetadata.getBlock())
                            .blockHash(eventMetadata.getBlockHash())
                            .blockTime(eventMetadata.getBlockTime())
                            .build();
                    stakeRegDeRegs.add(stakeRegDeReg);
                } else if (certificate.getType() == CertificateType.STAKE_DELEGATION) {
                    StakeDelegation stakeDelegation = (StakeDelegation) certificate;
                    Address address =
                            AddressUtil.getRewardAddress(stakeDelegation.getStakeCredential(), eventMetadata.isMainnet());

                    Delegation delegation = Delegation.builder()
                            .credential(stakeDelegation.getStakeCredential().getHash())
                            .address(address.toBech32())
                            .slot(eventMetadata.getSlot())
                            .txHash(txHash)
                            .certIndex(index)
                            .poolId(stakeDelegation.getStakePoolId().getPoolKeyHash())
                            .epoch(eventMetadata.getEpochNumber())
                            .slot(eventMetadata.getSlot())
                            .block(eventMetadata.getBlock())
                            .blockHash(eventMetadata.getBlockHash())
                            .blockTime(eventMetadata.getBlockTime())
                            .build();
                    delegations.add(delegation);
                }

                index++;
            }

            if (stakeRegDeRegs.size() > 0)
                stakingStorage.saveRegistrations(stakeRegDeRegs);
            if (delegations.size() > 0)
                stakingStorage.saveDelegations(delegations);
        }
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = stakingStorage.deleteRegistrationsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} staking_registrations records", count);

        count = stakingStorage.deleteDelegationsBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} staking_delegations records", count);
    }
}
