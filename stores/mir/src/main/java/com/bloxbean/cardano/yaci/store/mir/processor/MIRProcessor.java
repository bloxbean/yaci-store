package com.bloxbean.cardano.yaci.store.mir.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.yaci.core.model.certs.Certificate;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.certs.MoveInstataneous;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardAmt;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardEvent;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.mir.domain.MirPot;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.common.util.CCLUtil.toCCLCredential;
import static com.bloxbean.cardano.yaci.store.mir.MIRStoreConfiguration.STORE_MIR_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_MIR_ENABLED)
@Slf4j
public class MIRProcessor {
    private final MIRStorage mirStorage;

    private final ApplicationEventPublisher publisher;

    @EventListener
    @Transactional
    public void handleMIR(CertificateEvent certificateEvent) {
        if (certificateEvent.getTxCertificatesList() == null
                || certificateEvent.getTxCertificatesList().isEmpty())
            return;

        List<MoveInstataneousReward> moveInstataneousRewards = new ArrayList<>();
        for (TxCertificates txCertificates : certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();
            List<Certificate> certificates = txCertificates.getCertificates();

            int index = 0;
            for (Certificate certificate : certificates) {
                if (certificate.getType() == CertificateType.MOVE_INSTATENEOUS_REWARDS_CERT) {
                    MoveInstataneous moveInstataneous = (MoveInstataneous) certificate;
                    List<MoveInstataneousReward> rewards = handleTxMIRCertificate(certificateEvent.getMetadata(), txHash, moveInstataneous, index);
                    if (rewards != null && !rewards.isEmpty())
                        moveInstataneousRewards.addAll(rewards);
                }
                index++;
            }
        }

        if (!moveInstataneousRewards.isEmpty())
            mirStorage.save(moveInstataneousRewards);

        //Publish reward event
        getRewardEvent(certificateEvent, moveInstataneousRewards)
                .ifPresent(publisher::publishEvent);

    }

    private Optional<InstantRewardEvent> getRewardEvent(CertificateEvent certificateEvent, List<MoveInstataneousReward> moveInstataneousRewards) {
        var rewardAmts = moveInstataneousRewards.stream()
                .map(moveInstataneousReward -> {
                    InstantRewardType rewardType;
                    switch (moveInstataneousReward.getPot()) {
                        case treasury:
                            rewardType = InstantRewardType.treasury;
                            break;
                        case reserves:
                            rewardType = InstantRewardType.reserves;
                            break;
                        default:
                            rewardType = null;
                    }

                    return InstantRewardAmt.builder()
                            .rewardType(rewardType)
                            .txHash(moveInstataneousReward.getTxHash())
                            .amount(moveInstataneousReward.getAmount())
                            .address(moveInstataneousReward.getAddress())
                            .build();
                }).toList();

        if (rewardAmts.isEmpty())
            return Optional.empty();
        else {
            var rewardEvent = InstantRewardEvent.builder()
                    .metadata(certificateEvent.getMetadata())
                    .rewards(rewardAmts)
                    .build();
            return Optional.of(rewardEvent);
        }
    }

    private List<MoveInstataneousReward> handleTxMIRCertificate(EventMetadata eventMetadata, String txHash, MoveInstataneous mirCert, int certIndex) {
        List<MoveInstataneousReward> moveInstataneousRewards = new ArrayList<>();

        if (mirCert.getStakeCredentialCoinMap() == null || mirCert.getStakeCredentialCoinMap().isEmpty()) {
            var moveInstataneousReward = getMoveInstataneousRewardsDetails(eventMetadata, txHash, mirCert, certIndex);
            moveInstataneousReward.setAmount(mirCert.getAccountingPotCoin());
            moveInstataneousRewards.add(moveInstataneousReward);
        } else {
            var stakeCredCoinEntries = mirCert.getStakeCredentialCoinMap().entrySet();
            for (var stakeCredCoinEntry : stakeCredCoinEntries) {
                Address address = AddressProvider.getRewardAddress(toCCLCredential(stakeCredCoinEntry.getKey()), eventMetadata.isMainnet() ? Networks.mainnet() : Networks.testnet());
                var moveInstataneousReward = getMoveInstataneousRewardsDetails(eventMetadata, txHash, mirCert, certIndex);
                moveInstataneousReward.setCredential(stakeCredCoinEntry.getKey().getHash());
                moveInstataneousReward.setAddress(address.getAddress());
                moveInstataneousReward.setAmount(stakeCredCoinEntry.getValue());
                moveInstataneousRewards.add(moveInstataneousReward);
            }
        }

        return moveInstataneousRewards;
    }

    //Get MIR details without amounts from the certificate
    private static MoveInstataneousReward getMoveInstataneousRewardsDetails(EventMetadata eventMetadata, String txHash, MoveInstataneous mirCert, int certIndex) {
        var moveInstataneousReward = new MoveInstataneousReward();
        if (mirCert.isTreasury()) {
            moveInstataneousReward.setPot(MirPot.treasury);
        } else if (mirCert.isReserves()) {
            moveInstataneousReward.setPot(MirPot.reserves);
        } else {
            log.error("Invalid MIR certificate. Neither treasury nor reserves");
            return null;
        }

        moveInstataneousReward.setTxHash(txHash);
        moveInstataneousReward.setCertIndex(certIndex);
        moveInstataneousReward.setEpoch(eventMetadata.getEpochNumber());
        moveInstataneousReward.setSlot(eventMetadata.getSlot());
        moveInstataneousReward.setBlockHash(eventMetadata.getBlockHash());
        moveInstataneousReward.setBlockNumber(eventMetadata.getBlock());
        moveInstataneousReward.setBlockTime(eventMetadata.getBlockTime());

        return moveInstataneousReward;
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = mirStorage.rollbackMIRs(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} MIR records", count);
    }
}
