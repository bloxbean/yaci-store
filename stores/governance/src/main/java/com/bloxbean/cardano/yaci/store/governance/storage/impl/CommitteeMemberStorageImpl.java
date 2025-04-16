package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.mapper.CommitteeMemberMapper;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.repository.CommitteeMemberRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.*;
import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
public class CommitteeMemberStorageImpl implements CommitteeMemberStorage {
    private final CommitteeMemberRepository committeeMemberRepository;
    private final CommitteeMemberMapper committeeMemberMapper;
    private final DSLContext dsl;

    @Override
    public void saveAll(List<CommitteeMember> committeeMembers) {
        committeeMemberRepository.saveAll(committeeMembers.stream()
                .map(committeeMemberMapper::toCommitteeMemberEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public List<CommitteeMember> getCommitteeMembersByEpoch(int epoch) {
        return committeeMemberRepository.findCommitteeMembersByEpoch(epoch)
                .stream()
                .map(committeeMemberMapper::toCommitteeMember)
                .collect(Collectors.toList());
    }
//    @Override
//    public List<CommitteeMemberDetails> getActiveCommitteeMembersDetailsByEpoch(int epoch) {
//        var latestRegistration = dsl.select(
//                        COMMITTEE_REGISTRATION.COLD_KEY,
//                        max(COMMITTEE_REGISTRATION.SLOT).as("latest_slot"),
//                        max(COMMITTEE_REGISTRATION.TX_INDEX).as("latest_tx_index"),
//                        max(COMMITTEE_REGISTRATION.CERT_INDEX).as("latest_cert_index")
//                )
//                .from(COMMITTEE_REGISTRATION)
//                .where(COMMITTEE_REGISTRATION.EPOCH.le(epoch))
//                .groupBy(COMMITTEE_REGISTRATION.COLD_KEY)
//                .asTable("latest_registration");
//
//        var latestCommitteeMembers = dsl.select(
//                        COMMITTEE_MEMBER.HASH,
//                        COMMITTEE_MEMBER.CRED_TYPE,
//                        COMMITTEE_MEMBER.START_EPOCH,
//                        COMMITTEE_MEMBER.EXPIRED_EPOCH
//                )
//                .from(COMMITTEE_MEMBER)
//                .where(COMMITTEE_MEMBER.EPOCH.eq(
//                        select(max(COMMITTEE_MEMBER.EPOCH))
//                                .from(COMMITTEE_MEMBER)
//                                .where(COMMITTEE_MEMBER.EPOCH.le(epoch))
//                                )
//                ).and(COMMITTEE_MEMBER.EXPIRED_EPOCH.gt(epoch))
//                .asTable("latest_committee_member");
//
//        return dsl.select(
//                        COMMITTEE_REGISTRATION.COLD_KEY,
//                        COMMITTEE_REGISTRATION.HOT_KEY,
//                        COMMITTEE_REGISTRATION.CRED_TYPE,
//                        latestCommitteeMembers.field(COMMITTEE_MEMBER.START_EPOCH),
//                        latestCommitteeMembers.field(COMMITTEE_MEMBER.EXPIRED_EPOCH)
//                )
//                .from(COMMITTEE_REGISTRATION)
//                .join(latestRegistration)
//                .on(COMMITTEE_REGISTRATION.COLD_KEY.eq(latestRegistration.field(COMMITTEE_REGISTRATION.COLD_KEY)))
//                .and(COMMITTEE_REGISTRATION.SLOT.eq(latestRegistration.field("latest_slot", Long.class)))
//                .and(COMMITTEE_REGISTRATION.TX_INDEX.eq(latestRegistration.field("latest_tx_index", Integer.class)))
//                .and(COMMITTEE_REGISTRATION.CERT_INDEX.eq(latestRegistration.field("latest_cert_index", Integer.class)))
//                .join(latestCommitteeMembers)
//                .on(COMMITTEE_REGISTRATION.COLD_KEY.eq(latestCommitteeMembers.field(COMMITTEE_MEMBER.HASH)))
//                .whereNotExists(
//                        dsl.selectOne()
//                                .from(COMMITTEE_DEREGISTRATION)
//                                .where(COMMITTEE_DEREGISTRATION.COLD_KEY.eq(COMMITTEE_REGISTRATION.COLD_KEY))
//                                .and(
//                                        COMMITTEE_DEREGISTRATION.SLOT.gt(COMMITTEE_REGISTRATION.SLOT)
//                                                .or(
//                                                        COMMITTEE_DEREGISTRATION.SLOT.eq(COMMITTEE_REGISTRATION.SLOT)
//                                                                .and(COMMITTEE_DEREGISTRATION.TX_INDEX.gt(COMMITTEE_REGISTRATION.TX_INDEX))
//                                                )
//                                                .or(
//                                                        COMMITTEE_DEREGISTRATION.SLOT.eq(COMMITTEE_REGISTRATION.SLOT)
//                                                                .and(COMMITTEE_DEREGISTRATION.TX_INDEX.eq(COMMITTEE_REGISTRATION.TX_INDEX))
//                                                                .and(COMMITTEE_DEREGISTRATION.CERT_INDEX.gt(COMMITTEE_REGISTRATION.CERT_INDEX))
//                                                )
//                                )
//                                .and(COMMITTEE_DEREGISTRATION.EPOCH.le(epoch))
//                )
//                .fetch(record -> new CommitteeMemberDetails(
//                        record.get(COMMITTEE_REGISTRATION.COLD_KEY),
//                        record.get(COMMITTEE_REGISTRATION.HOT_KEY),
//                        CredentialType.valueOf(record.get(COMMITTEE_REGISTRATION.CRED_TYPE)),
//                        record.get(latestCommitteeMembers.field(COMMITTEE_MEMBER.START_EPOCH)),
//                        record.get(latestCommitteeMembers.field(COMMITTEE_MEMBER.EXPIRED_EPOCH))
//                ));
//    }

    // todo: add integration test
    @Override
    public List<CommitteeMemberDetails> getActiveCommitteeMembersDetailsByEpoch(int epoch) {
        // Latest committee_registration per cold_key using ROW_NUMBER()
        var orderedRegistration = dsl
                .select(
                        COMMITTEE_REGISTRATION.COLD_KEY,
                        COMMITTEE_REGISTRATION.HOT_KEY,
                        COMMITTEE_REGISTRATION.CRED_TYPE,
                        COMMITTEE_REGISTRATION.SLOT,
                        COMMITTEE_REGISTRATION.TX_INDEX,
                        COMMITTEE_REGISTRATION.CERT_INDEX,
                        rowNumber()
                                .over()
                                .partitionBy(COMMITTEE_REGISTRATION.COLD_KEY)
                                .orderBy(
                                        COMMITTEE_REGISTRATION.SLOT.desc(),
                                        COMMITTEE_REGISTRATION.TX_INDEX.desc(),
                                        COMMITTEE_REGISTRATION.CERT_INDEX.desc()
                                ).as("rn")
                )
                .from(COMMITTEE_REGISTRATION)
                .where(COMMITTEE_REGISTRATION.EPOCH.le(epoch))
                .asTable("ordered_registration");

        var latestRegistration = dsl
                .selectFrom(orderedRegistration)
                .where(orderedRegistration.field("rn", Integer.class).eq(1))
                .asTable("latest_registration");

        // Latest committee_member epoch that’s <= epoch and not expired
        var latestCommitteeMembers = dsl
                .select(
                        COMMITTEE_MEMBER.HASH,
                        COMMITTEE_MEMBER.CRED_TYPE,
                        COMMITTEE_MEMBER.START_EPOCH,
                        COMMITTEE_MEMBER.EXPIRED_EPOCH
                )
                .from(COMMITTEE_MEMBER)
                .where(COMMITTEE_MEMBER.EPOCH.eq(
                        select(max(COMMITTEE_MEMBER.EPOCH))
                                .from(COMMITTEE_MEMBER)
                                .where(COMMITTEE_MEMBER.EPOCH.le(epoch))
                ))
                .and(COMMITTEE_MEMBER.EXPIRED_EPOCH.gt(epoch)) // todo: verify 'greater than' or 'greater or equals'
                .asTable("latest_committee_member");

        // Main query joining the above, and filtering out deregistered members
        return dsl.select(
                        latestRegistration.field(COMMITTEE_REGISTRATION.COLD_KEY),
                        latestRegistration.field(COMMITTEE_REGISTRATION.HOT_KEY),
                        latestRegistration.field(COMMITTEE_REGISTRATION.CRED_TYPE),
                        latestCommitteeMembers.field(COMMITTEE_MEMBER.START_EPOCH),
                        latestCommitteeMembers.field(COMMITTEE_MEMBER.EXPIRED_EPOCH)
                )
                .from(latestRegistration)
                .join(latestCommitteeMembers)
                .on(latestRegistration.field(COMMITTEE_REGISTRATION.COLD_KEY)
                        .eq(latestCommitteeMembers.field(COMMITTEE_MEMBER.HASH)))
                .whereNotExists(
                        dsl.selectOne()
                                .from(COMMITTEE_DEREGISTRATION)
                                .where(COMMITTEE_DEREGISTRATION.COLD_KEY
                                        .eq(latestRegistration.field(COMMITTEE_REGISTRATION.COLD_KEY)))
                                .and(COMMITTEE_DEREGISTRATION.EPOCH.le(epoch))
                                .and(
                                        COMMITTEE_DEREGISTRATION.SLOT.gt(latestRegistration.field(COMMITTEE_REGISTRATION.SLOT))
                                                .or(
                                                        COMMITTEE_DEREGISTRATION.SLOT.eq(latestRegistration.field(COMMITTEE_REGISTRATION.SLOT))
                                                                .and(COMMITTEE_DEREGISTRATION.TX_INDEX.gt(latestRegistration.field(COMMITTEE_REGISTRATION.TX_INDEX)))
                                                )
                                                .or(
                                                        COMMITTEE_DEREGISTRATION.SLOT.eq(latestRegistration.field(COMMITTEE_REGISTRATION.SLOT))
                                                                .and(COMMITTEE_DEREGISTRATION.TX_INDEX.eq(latestRegistration.field(COMMITTEE_REGISTRATION.TX_INDEX)))
                                                                .and(COMMITTEE_DEREGISTRATION.CERT_INDEX.gt(latestRegistration.field(COMMITTEE_REGISTRATION.CERT_INDEX)))
                                                )
                                )
                )
                .fetch(record -> new CommitteeMemberDetails(
                        record.get(latestRegistration.field(COMMITTEE_REGISTRATION.COLD_KEY)),
                        record.get(latestRegistration.field(COMMITTEE_REGISTRATION.HOT_KEY)),
                        CredentialType.valueOf(record.get(latestRegistration.field(COMMITTEE_REGISTRATION.CRED_TYPE))),
                        record.get(latestCommitteeMembers.field(COMMITTEE_MEMBER.START_EPOCH)),
                        record.get(latestCommitteeMembers.field(COMMITTEE_MEMBER.EXPIRED_EPOCH))
                ));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return committeeMemberRepository.deleteBySlotGreaterThan(slot);
    }
}
