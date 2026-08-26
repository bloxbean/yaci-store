package com.bloxbean.cardano.yaci.store.staking.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.events.internal.BatchBlocksProcessedEvent;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import com.bloxbean.cardano.yaci.store.staking.domain.event.StakeRegDeregEvent;
import com.bloxbean.cardano.yaci.store.staking.service.DepositParamService;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorage;
import com.bloxbean.cardano.yaci.store.staking.storage.StakingCertificateStorageReader;
import com.bloxbean.cardano.yaci.store.staking.util.AddressUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.staking.StakingStoreConfiguration.STORE_STAKING_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_STAKING_ENABLED)
@Slf4j
public class StakeRegProcessor {
    private final StakingCertificateStorage stakingStorage;
    private final StakingCertificateStorageReader stakingStorageReader;
    private final ApplicationEventPublisher publisher;
    private final DepositParamService depositParamService;

    @EventListener
    @Transactional
    public void processStakeRegistration(CertificateEvent certificateEvent) {
        EventMetadata eventMetadata = certificateEvent.getMetadata();

        List<StakeRegistrationDetail> stakeRegDeRegs = new ArrayList<>();
        List<Delegation> delegations = new ArrayList<>();
        Map<String, Optional<BigInteger>> activeDeposits = new HashMap<>();

        for (TxCertificates txCertificates : certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();
            int txIndex = txCertificates.getTxIndex();
            List<Certificate> certificates = txCertificates.getCertificates();

            int index = 0;
            for (Certificate certificate : certificates) {
                var certType = certificate.getType();
                StakeRegistration stakeRegistration;
                StakeDeregistration stakeDeregistration;
                StakeDelegation stakeDelegation;
                StakeRegistrationDetail stakeRegistrationDetail;
                Delegation delegation;

                switch (certType) {
                    case STAKE_REGISTRATION, REG_CERT, VOTE_REG_DELEG_CERT:
                        if (certType == CertificateType.STAKE_REGISTRATION) {
                            stakeRegistration = (StakeRegistration) certificate;
                        } else if (certType == CertificateType.REG_CERT) {
                            stakeRegistration = StakeRegistration.builder()
                                    .stakeCredential(((RegCert) certificate).getStakeCredential()).build();
                        } else {
                            stakeRegistration = StakeRegistration.builder()
                                    .stakeCredential(((VoteRegDelegCert) certificate).getStakeCredential())
                                    .build();
                        }
                        stakeRegistrationDetail = buildStakeRegistrationDetail(
                                stakeRegistration, txHash, index, txIndex,
                                resolveRegistrationDeposit(certificate, eventMetadata.getEpochNumber()), eventMetadata);

                        stakeRegDeRegs.add(stakeRegistrationDetail);
                        activeDeposits.put(stakeRegistration.getStakeCredential().getHash(),
                                Optional.ofNullable(stakeRegistrationDetail.getDeposit()));
                        break;

                    case STAKE_DEREGISTRATION, UNREG_CERT:
                        if (certType == CertificateType.STAKE_DEREGISTRATION) {
                            stakeDeregistration = (StakeDeregistration) certificate;
                        } else {
                            stakeDeregistration = StakeDeregistration.builder()
                                    .stakeCredential(((UnregCert) certificate).getStakeCredential())
                                    .build();
                        }
                        stakeRegistrationDetail = buildStakeRegistrationDetail(
                                stakeDeregistration, txHash, index, txIndex,
                                resolveDeregistrationDeposit(certificate, stakeDeregistration.getStakeCredential(),
                                        txIndex, index, eventMetadata, activeDeposits), eventMetadata);

                        stakeRegDeRegs.add(stakeRegistrationDetail);
                        activeDeposits.put(stakeDeregistration.getStakeCredential().getHash(), Optional.empty());
                        break;

                    case STAKE_DELEGATION, STAKE_VOTE_DELEG_CERT:
                        if (certType == CertificateType.STAKE_VOTE_DELEG_CERT) {
                            var stakeVoteDelegCert = (StakeVoteDelegCert) certificate;
                            stakeDelegation = StakeDelegation.builder()
                                    .stakeCredential(stakeVoteDelegCert.getStakeCredential())
                                    .stakePoolId(StakePoolId.fromHexPoolId(stakeVoteDelegCert.getPoolKeyHash()))
                                    .build();
                        } else {
                            stakeDelegation = (StakeDelegation) certificate;
                        }

                        delegation = buildDelegation(stakeDelegation, txHash, index, txIndex, eventMetadata);
                        delegations.add(delegation);

                        break;

                    case STAKE_REG_DELEG_CERT, STAKE_VOTE_REG_DELEG_CERT:
                        if (certType == CertificateType.STAKE_REG_DELEG_CERT) {
                            var stakeRegDelegCert = (StakeRegDelegCert) certificate;
                            stakeDelegation = StakeDelegation.builder()
                                    .stakeCredential(stakeRegDelegCert.getStakeCredential())
                                    .stakePoolId(StakePoolId.builder().poolKeyHash(stakeRegDelegCert.getPoolKeyHash()).build())
                                    .build();
                            stakeRegistration = StakeRegistration.builder()
                                    .stakeCredential(stakeRegDelegCert.getStakeCredential())
                                    .build();
                        } else {
                            var stakeVoteRegDelegCert = (StakeVoteRegDelegCert) certificate;
                            stakeDelegation = StakeDelegation.builder()
                                    .stakeCredential(stakeVoteRegDelegCert.getStakeCredential())
                                    .stakePoolId(StakePoolId.builder().poolKeyHash(stakeVoteRegDelegCert.getPoolKeyHash()).build())
                                    .build();
                            stakeRegistration = StakeRegistration.builder()
                                    .stakeCredential(stakeVoteRegDelegCert.getStakeCredential())
                                    .build();
                        }

                        delegation = buildDelegation(stakeDelegation, txHash, index, txIndex, eventMetadata);
                        stakeRegistrationDetail = buildStakeRegistrationDetail(
                                stakeRegistration, txHash, index, txIndex,
                                resolveRegistrationDeposit(certificate, eventMetadata.getEpochNumber()), eventMetadata);

                        stakeRegDeRegs.add(stakeRegistrationDetail);
                        activeDeposits.put(stakeRegistration.getStakeCredential().getHash(),
                                Optional.ofNullable(stakeRegistrationDetail.getDeposit()));
                        delegations.add(delegation);
                        break;
                    default:
                        break;
                }

                index++;
            }
        }

        if (!stakeRegDeRegs.isEmpty()) {
            stakingStorage.saveRegistrations(stakeRegDeRegs);

        }
        if (!delegations.isEmpty())
            stakingStorage.saveDelegations(delegations);

        //publish events
        if (!stakeRegDeRegs.isEmpty()) {
            publisher.publishEvent(new StakeRegDeregEvent(eventMetadata, stakeRegDeRegs));
        }
    }

    /**
     * Parallel block partitions can persist a legacy deregistration before the partition containing
     * its active registration commits. Once every partition in the batch has completed, retry only
     * unresolved rows from that batch against the now-complete lifecycle history. An unresolved
     * deposit at this point is fatal because downstream deposit snapshots require complete amounts.
     */
    @EventListener
    @Transactional
    public void reconcileDeregistrationDeposits(BatchBlocksProcessedEvent event) {
        if (event.getBlockCaches() == null || event.getBlockCaches().isEmpty()) {
            return;
        }

        long fromSlot = event.getBlockCaches().stream()
                .mapToLong(batchBlock -> batchBlock.getMetadata().getSlot())
                .min()
                .orElseThrow();
        long toSlot = event.getBlockCaches().stream()
                .mapToLong(batchBlock -> batchBlock.getMetadata().getSlot())
                .max()
                .orElseThrow();

        List<StakeRegistrationDetail> unresolvedDeregistrations =
                stakingStorageReader.findUnresolvedDeregistrations(fromSlot, toSlot);
        if (unresolvedDeregistrations.isEmpty()) {
            return;
        }

        List<StakeRegistrationDetail> resolvedDeregistrations = new ArrayList<>();
        List<StakeRegistrationDetail> stillUnresolvedDeregistrations = new ArrayList<>();
        for (StakeRegistrationDetail deregistration : unresolvedDeregistrations) {
            Optional<BigInteger> deposit = stakingStorageReader.getRegistrationBefore(
                            deregistration.getAddress(), deregistration.getSlot(),
                            deregistration.getTxIndex(), deregistration.getCertIndex())
                    .filter(registration -> registration.getType() == CertificateType.STAKE_REGISTRATION)
                    .map(StakeRegistrationDetail::getDeposit);

            if (deposit.isPresent()) {
                deregistration.setDeposit(deposit.get());
                resolvedDeregistrations.add(deregistration);
            } else {
                stillUnresolvedDeregistrations.add(deregistration);
            }
        }

        if (!stillUnresolvedDeregistrations.isEmpty()) {
            StakeRegistrationDetail firstUnresolved = stillUnresolvedDeregistrations.getFirst();
            String message = ("Unable to resolve deposits for %d legacy stake deregistration certificate(s) " +
                    "in slots %d-%d. First unresolved certificate: tx_hash=%s, cert_index=%d")
                    .formatted(stillUnresolvedDeregistrations.size(), fromSlot, toSlot,
                            firstUnresolved.getTxHash(), firstUnresolved.getCertIndex());
            throw new IllegalStateException(message);
        }

        if (!resolvedDeregistrations.isEmpty()) {
            stakingStorage.saveRegistrations(resolvedDeregistrations);
            log.debug("Reconciled deposits for {} legacy stake deregistrations in slots {}-{}",
                    resolvedDeregistrations.size(), fromSlot, toSlot);
        }
    }

    private BigInteger resolveRegistrationDeposit(Certificate certificate, int epoch) {
        BigInteger coin = switch (certificate.getType()) {
            case REG_CERT -> ((RegCert) certificate).getCoin();
            case STAKE_REG_DELEG_CERT -> ((StakeRegDelegCert) certificate).getCoin();
            case VOTE_REG_DELEG_CERT -> ((VoteRegDelegCert) certificate).getCoin();
            case STAKE_VOTE_REG_DELEG_CERT -> ((StakeVoteRegDelegCert) certificate).getCoin();
            default -> null;
        };
        if (coin != null) {
            return coin;
        }
        return depositParamService.getKeyDeposit(epoch);
    }

    /**
     * Legacy deregistration certificates omit the refund amount. The ledger refunds the deposit
     * recorded by the active registration, not the current protocol parameter. Registrations in
     * this event are not persisted until the batch is saved, so resolve them from in-event state
     * before querying the latest stored lifecycle row.
     */
    private BigInteger resolveDeregistrationDeposit(Certificate certificate,
                                                    StakeCredential stakeCredential,
                                                    int txIndex,
                                                    int certIndex,
                                                    EventMetadata eventMetadata,
                                                    Map<String, Optional<BigInteger>> activeDeposits) {
        if (certificate.getType() == CertificateType.UNREG_CERT) {
            return ((UnregCert) certificate).getCoin();
        }

        String credential = stakeCredential.getHash();
        if (!activeDeposits.containsKey(credential)) {
            String stakeAddress = AddressUtil.getRewardAddress(stakeCredential, eventMetadata.isMainnet()).toBech32();
            Optional<BigInteger> storedDeposit = stakingStorageReader
                    .getRegistrationBefore(stakeAddress, eventMetadata.getSlot(), txIndex, certIndex)
                    .filter(registration -> registration.getType() == CertificateType.STAKE_REGISTRATION)
                    .map(StakeRegistrationDetail::getDeposit);
            activeDeposits.put(credential, storedDeposit);
        }

        return activeDeposits.get(credential).orElse(null);
    }

    private StakeRegistrationDetail buildStakeRegistrationDetail(StakeRegistration stakeRegistration,
                                                                 String txHash,
                                                                 int certIndex,
                                                                 int txIndex,
                                                                 BigInteger deposit,
                                                                 EventMetadata eventMetadata) {
        Address address =
                AddressUtil.getRewardAddress(stakeRegistration.getStakeCredential(), eventMetadata.isMainnet());

        return StakeRegistrationDetail.builder()
                .credential(stakeRegistration.getStakeCredential().getHash())
                .credentialType(getCredType(stakeRegistration.getStakeCredential())) //TODO -- add to db
                .address(address.toBech32())
                .slot(eventMetadata.getSlot())
                .txHash(txHash)
                .certIndex(certIndex)
                .txIndex(txIndex)
                .type(CertificateType.STAKE_REGISTRATION)
                .deposit(deposit)
                .epoch(eventMetadata.getEpochNumber())
                .slot(eventMetadata.getSlot())
                .blockNumber(eventMetadata.getBlock())
                .blockHash(eventMetadata.getBlockHash())
                .blockTime(eventMetadata.getBlockTime())
                .build();
    }

    private StakeRegistrationDetail buildStakeRegistrationDetail(StakeDeregistration stakeDeregistration,
                                                                 String txHash,
                                                                 int certIndex,
                                                                 int txIndex,
                                                                 BigInteger deposit,
                                                                 EventMetadata eventMetadata) {
        Address address =
                AddressUtil.getRewardAddress(stakeDeregistration.getStakeCredential(), eventMetadata.isMainnet());

        return StakeRegistrationDetail.builder()
                .credential(stakeDeregistration.getStakeCredential().getHash())
                .credentialType(getCredType(stakeDeregistration.getStakeCredential()))
                .address(address.toBech32())
                .slot(eventMetadata.getSlot())
                .txHash(txHash)
                .certIndex(certIndex)
                .txIndex(txIndex)
                .type(CertificateType.STAKE_DEREGISTRATION)
                .deposit(deposit)
                .epoch(eventMetadata.getEpochNumber())
                .slot(eventMetadata.getSlot())
                .blockNumber(eventMetadata.getBlock())
                .blockHash(eventMetadata.getBlockHash())
                .blockTime(eventMetadata.getBlockTime())
                .build();
    }

    private Delegation buildDelegation(StakeDelegation stakeDelegation,
                                       String txHash,
                                       int certIndex,
                                       int txIndex,
                                       EventMetadata eventMetadata) {
        Address address =
                AddressUtil.getRewardAddress(stakeDelegation.getStakeCredential(), eventMetadata.isMainnet());

        return Delegation.builder()
                .credential(stakeDelegation.getStakeCredential().getHash())
                .credentialType(getCredType(stakeDelegation.getStakeCredential()))
                .address(address.toBech32())
                .slot(eventMetadata.getSlot())
                .txHash(txHash)
                .certIndex(certIndex)
                .txIndex(txIndex)
                .poolId(stakeDelegation.getStakePoolId().getPoolKeyHash())
                .epoch(eventMetadata.getEpochNumber())
                .slot(eventMetadata.getSlot())
                .blockNumber(eventMetadata.getBlock())
                .blockHash(eventMetadata.getBlockHash())
                .blockTime(eventMetadata.getBlockTime())
                .build();
    }

    private com.bloxbean.cardano.yaci.core.model.CredentialType getCredType(StakeCredential stakeCredential) {
        if (stakeCredential.getType() == StakeCredType.ADDR_KEYHASH)
            return com.bloxbean.cardano.yaci.core.model.CredentialType.ADDR_KEYHASH;
        else if (stakeCredential.getType() == StakeCredType.SCRIPTHASH)
            return com.bloxbean.cardano.yaci.core.model.CredentialType.SCRIPTHASH;
        else
            return null;
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
