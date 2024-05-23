package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.GenesisCommitteeMember;
import com.bloxbean.cardano.yaci.store.common.genesis.ConwayGenesis;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommitteeMemberProcessor {
    private final StoreProperties storeProperties;
    private final CommitteeMemberStorage committeeMemberStorage;

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
        Era prevEra = epochChangeEvent.getPreviousEra();
        Era newEra = epochChangeEvent.getEra();
        long protocolMagic = epochChangeEvent.getEventMetadata().getProtocolMagic();
        long slot = epochChangeEvent.getEventMetadata().getSlot();

        if (newEra.equals(Era.Conway) && prevEra != Era.Conway) {
            var committeeMembers = getGenesisCommitteeMembers(protocolMagic);
            if (committeeMembers != null && !committeeMembers.isEmpty()) {
                var committeeMembersToSave = committeeMembers.stream().map(committeeMember -> buildCommitteeMember(committeeMember, slot))
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

    private CommitteeMember buildCommitteeMember(GenesisCommitteeMember committeeMember, Long slot) {
        return CommitteeMember.builder()
                .hash(committeeMember.getHash())
                .expiredEpoch(committeeMember.getExpiredEpoch())
                .credType(committeeMember.getHasScript() ? CredentialType.SCRIPTHASH : CredentialType.ADDR_KEYHASH)
                .slot(slot)
                .build();
    }

    @EventListener
    @Transactional
    //TODO -- tests
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = committeeMemberStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} committee_member records", count);
    }
}
