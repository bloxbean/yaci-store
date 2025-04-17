package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.Credential;
import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.core.model.governance.actions.UpdateCommittee;
import com.bloxbean.cardano.yaci.store.client.governance.ProposalStateClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GenesisCommitteeMember;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.GenesisBlockEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreAdaPotJobProcessingEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.governance.GovernanceStoreConfiguration.STORE_GOVERNANCE_ENABLED;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_GOVERNANCE_ENABLED)
@Slf4j
public class CommitteeMemberProcessor {
    private final StoreProperties storeProperties;
    private final CommitteeMemberStorage committeeMemberStorage;
    private final CommitteeMemberStorageReader committeeMemberStorageReader;
    private final ProposalStateClient proposalStateClient;

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(PreEpochTransitionEvent event) {
        if (event.getEra().getValue() < Era.Conway.getValue()) {
            return;
        }

        Era prevEra = event.getPreviousEra();
        Era newEra = event.getEra();
        long protocolMagic = event.getMetadata().getProtocolMagic();
        long slot = event.getMetadata().getSlot();
        Integer epoch = event.getEpoch();

        // store data from genesis file
        if (newEra.equals(Era.Conway) && prevEra != Era.Conway) {
            boolean isCommitteeMemberDataPresent = committeeMemberStorageReader.findAll(0, 1, null).size() > 0;
            if (isCommitteeMemberDataPresent) {
                log.debug("Committee members already exists. Skipping storing committee members from genesis file");
                return;
            }

            var committeeMembers = getGenesisCommitteeMembers(protocolMagic);
            if (committeeMembers != null && !committeeMembers.isEmpty()) {
                var committeeMembersToSave = committeeMembers.stream().map(committeeMember ->
                                buildCommitteeMember(committeeMember, epoch, epoch, slot))
                        .collect(Collectors.toList());
                committeeMemberStorage.saveAll(committeeMembersToSave);
            }
        }
    }

    private List<GenesisCommitteeMember> getGenesisCommitteeMembers(long protocolMagic) {
        String conwayGenesisFile = storeProperties.getConwayGenesisFile();

        if (StringUtil.isEmpty(conwayGenesisFile))
            return new ConwayGenesis(protocolMagic).getCommitteeMembers();
        else
            return new ConwayGenesis(new File(conwayGenesisFile)).getCommitteeMembers();
    }

    private CommitteeMember buildCommitteeMember(GenesisCommitteeMember committeeMember, Integer startEpoch, Integer epoch, Long slot) {
        return CommitteeMember.builder()
                .hash(committeeMember.getHash())
                .startEpoch(startEpoch)
                .expiredEpoch(committeeMember.getExpiredEpoch())
                .credType(committeeMember.getHasScript() ? CredentialType.SCRIPTHASH : CredentialType.ADDR_KEYHASH)
                .epoch(epoch)
                .slot(slot)
                .build();
    }

    /**
     * This will be invoked for custom devnet like Yaci DevKit devnet which directly starts from Conway
     * @param genesisBlockEvent
     */
    @EventListener
    @Transactional
    public void handleGenesisBlockEvent(GenesisBlockEvent genesisBlockEvent) {
        if (genesisBlockEvent.getEra().getValue() < Era.Conway.getValue())
            return;

        boolean isCommitteeMemberDataPresent = committeeMemberStorageReader.findAll(0, 1, null).size() > 0;
        if (isCommitteeMemberDataPresent) {
            log.debug("Committee members already exists. Skipping storing committee members from genesis file");
            return;
        }

        var committeeMembers = getGenesisCommitteeMembers(storeProperties.getProtocolMagic());
        if (committeeMembers != null && !committeeMembers.isEmpty()) {
            var committeeMembersToSave = committeeMembers.stream().map(committeeMember ->
                            buildCommitteeMember(committeeMember, genesisBlockEvent.getEpoch(), genesisBlockEvent.getEpoch(), genesisBlockEvent.getSlot()))
                    .collect(Collectors.toList());
            committeeMemberStorage.saveAll(committeeMembersToSave);
        }

    }

    @EventListener
    @Transactional
    public void handlePreAdaPotJobProcessingEvent(PreAdaPotJobProcessingEvent event) {
        int epoch = event.getEpoch();
        long slot = event.getSlot();

        List<GovActionProposal> ratifiedProposalsInPrevEpoch =
                proposalStateClient.getProposalsByStatusAndEpoch(GovActionStatus.RATIFIED, epoch - 1);
        List<CommitteeMember> updatedCommitteeMembers = new ArrayList<>();

        for (var proposal : ratifiedProposalsInPrevEpoch) {
            if (!(proposal.getGovAction() instanceof UpdateCommittee updateCommittee)) {
                continue;
            }

            var currentCommitteeMembers = committeeMemberStorage.getCommitteeMembersByEpoch(epoch - 1);

            Set<String> membersForRemovalHashes = updateCommittee.getMembersForRemoval()
                    .stream().map(Credential::getHash).collect(Collectors.toSet());

            // Remove members
            currentCommitteeMembers.stream()
                    .filter(member -> !membersForRemovalHashes.contains(member.getHash()))
                    .map(member -> CommitteeMember.builder()
                            .startEpoch(member.getStartEpoch())
                            .expiredEpoch(member.getExpiredEpoch())
                            .hash(member.getHash())
                            .credType(member.getCredType())
                            .epoch(epoch)
                            .slot(slot)
                            .build())
                    .forEach(updatedCommitteeMembers::add);

            // Add new members
            updateCommittee.getNewMembersAndTerms().forEach((credential, term) ->
                    updatedCommitteeMembers.add(CommitteeMember.builder()
                            .hash(credential.getHash())
                            .startEpoch(epoch)
                            .expiredEpoch(epoch + term)
                            .credType(credential.getType().equals(StakeCredType.ADDR_KEYHASH)
                                    ? CredentialType.ADDR_KEYHASH
                                    : CredentialType.SCRIPTHASH)
                            .epoch(epoch)
                            .slot(slot)
                            .build())
            );
        }

        if (!updatedCommitteeMembers.isEmpty()) {
            committeeMemberStorage.saveAll(updatedCommitteeMembers);
        }
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = committeeMemberStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} committee_member records", count);
    }
}
