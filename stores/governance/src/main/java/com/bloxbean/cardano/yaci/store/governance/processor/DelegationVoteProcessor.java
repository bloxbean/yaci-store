package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.governance.DRepId;
import com.bloxbean.cardano.yaci.core.model.certs.*;
import com.bloxbean.cardano.yaci.core.model.governance.Drep;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.store.events.CertificateEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxCertificates;
import com.bloxbean.cardano.yaci.store.governance.domain.DelegationVote;
import com.bloxbean.cardano.yaci.store.governance.storage.DelegationVoteStorage;
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
public class DelegationVoteProcessor {
    private final DelegationVoteStorage delegationVoteStorage;

    @EventListener
    @Transactional
    public void handleDelegationVote(CertificateEvent certificateEvent) {
        EventMetadata eventMetadata = certificateEvent.getMetadata();

        List<DelegationVote> delegationVotes = new ArrayList<>();
        for (TxCertificates txCertificates : certificateEvent.getTxCertificatesList()) {
            String txHash = txCertificates.getTxHash();
            int index = 0;
            for (var certificate : txCertificates.getCertificates()) {
                DelegationVote delegationVote = switch (certificate.getType()) {
                    case VOTE_DELEG_CERT -> {
                        VoteDelegCert voteDelegCert = (VoteDelegCert) certificate;
                        yield buildDelegationVote(voteDelegCert.getDrep(), voteDelegCert.getStakeCredential(), txHash, index, eventMetadata);
                    }
                    case VOTE_REG_DELEG_CERT -> {
                        VoteRegDelegCert voteRegDelegCert = (VoteRegDelegCert) certificate;
                        yield buildDelegationVote(voteRegDelegCert.getDrep(), voteRegDelegCert.getStakeCredential(), txHash, index, eventMetadata);
                    }
                    case STAKE_VOTE_DELEG_CERT -> {
                        StakeVoteDelegCert stakeVoteDelegCert = (StakeVoteDelegCert) certificate;
                        yield buildDelegationVote(stakeVoteDelegCert.getDrep(), stakeVoteDelegCert.getStakeCredential(), txHash, index, eventMetadata);
                    }
                    case STAKE_VOTE_REG_DELEG_CERT -> {
                        StakeVoteRegDelegCert stakeVoteRegDelegCert = (StakeVoteRegDelegCert) certificate;
                        yield buildDelegationVote(stakeVoteRegDelegCert.getDrep(), stakeVoteRegDelegCert.getStakeCredential(), txHash, index, eventMetadata);
                    }
                    default -> null;
                };

                if (delegationVote != null) {
                    delegationVotes.add(delegationVote);
                }
                index++;
            }
        }

        if (!delegationVotes.isEmpty()) {
            delegationVoteStorage.saveAll(delegationVotes);
        }
    }

    private DelegationVote buildDelegationVote(Drep drep, StakeCredential stakeCredential, String txHash,
                                               int certIndex, EventMetadata eventMetadata) {
        DelegationVote delegationVote = DelegationVote.builder()
                .drepHash(drep.getHash())
                .txHash(txHash)
                .certIndex(certIndex)
                .slot(eventMetadata.getSlot())
                .blockNumber(eventMetadata.getBlock())
                .blockTime(eventMetadata.getBlockTime())
                .epoch(eventMetadata.getEpochNumber())
                .drepType(drep.getType())
                .credType(stakeCredential.getType())
                .credential(stakeCredential.getHash())
                .build();

        if (drep.getHash() != null) {
            if (drep.getType() == DrepType.ADDR_KEYHASH) {
                delegationVote.setDrepId(DRepId.fromKeyHash(drep.getHash()));
            } else if (drep.getType() == DrepType.SCRIPTHASH) {
                delegationVote.setDrepId(DRepId.fromScriptHash(drep.getHash()));
            }
        }

        Address address = AddressProvider.getRewardAddress(toCCLCredential(stakeCredential),
                eventMetadata.isMainnet() ? Networks.mainnet() : Networks.testnet());

        delegationVote.setAddress(address.toBech32());

        return delegationVote;
    }

    private Credential toCCLCredential(StakeCredential credential) {
        if (credential.getType() == StakeCredType.ADDR_KEYHASH) {
            return Credential.fromKey(credential.getHash());
        } else if (credential.getType() == StakeCredType.SCRIPTHASH) {
            return Credential.fromScript(credential.getHash());
        } else {
            throw new IllegalArgumentException("Invalid credential type");
        }
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = delegationVoteStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} delegation_vote records", count);
    }
}
