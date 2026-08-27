package com.bloxbean.cardano.yaci.store.governance.storage.impl;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMember;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeMemberStorage;
import com.bloxbean.cardano.yaci.store.governance.storage.CommitteeRegistrationStorage;
import org.jooq.DSLContext;
import org.jooq.conf.RenderQuotedNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jooq.DefaultConfigurationCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.COMMITTEE_DEREGISTRATION;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.COMMITTEE_MEMBER;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.COMMITTEE_REGISTRATION;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(CommitteeMemberStorageImplTest.JooqTestConfig.class)
class CommitteeMemberStorageImplTest {

    @Autowired
    private DSLContext dsl;

    @Autowired
    private CommitteeMemberStorage committeeMemberStorage;

    @Autowired
    private CommitteeRegistrationStorage committeeRegistrationStorage;

    @BeforeEach
    void setUp() {
        dsl.deleteFrom(COMMITTEE_DEREGISTRATION).execute();
        dsl.deleteFrom(COMMITTEE_REGISTRATION).execute();
        dsl.deleteFrom(COMMITTEE_MEMBER).execute();
    }

    @Test
    void getActiveCommitteeMembersDetailsIncludesMemberAtExpiryEpoch() {
        saveCommitteeMember("cold-key-1", 10, 20, 100);
        saveCommitteeRegistration("cold-key-1", "hot-key-1", 19, 1900, 0);

        List<CommitteeMemberDetails> votingMembers = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(20);
        List<CommitteeMemberDetails> ratificationMembers =
                committeeMemberStorage.getActiveCommitteeMembersDetailsForRatificationByEpoch(20);

        assertActiveMember(votingMembers, "cold-key-1", "hot-key-1", 20);
        assertActiveMember(ratificationMembers, "cold-key-1", "hot-key-1", 20);
    }

    @Test
    void getActiveCommitteeMembersDetailsExcludesMemberAfterExpiryEpoch() {
        saveCommitteeMember("cold-key-2", 10, 19, 100);
        saveCommitteeRegistration("cold-key-2", "hot-key-2", 19, 1900, 0);

        List<CommitteeMemberDetails> votingMembers = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(20);
        List<CommitteeMemberDetails> ratificationMembers =
                committeeMemberStorage.getActiveCommitteeMembersDetailsForRatificationByEpoch(20);

        assertThat(votingMembers).isEmpty();
        assertThat(ratificationMembers).isEmpty();
    }

    @Test
    void getActiveCommitteeMembersDetailsForRatificationByEpochExcludesCurrentEpochRegistration() {
        saveCommitteeMember("cold-key-3", 10, 20, 100);
        saveCommitteeRegistration("cold-key-3", "hot-key-3", 20, 2000, 0);

        List<CommitteeMemberDetails> votingMembers = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(20);
        List<CommitteeMemberDetails> ratificationMembers =
                committeeMemberStorage.getActiveCommitteeMembersDetailsForRatificationByEpoch(20);

        assertThat(votingMembers).singleElement()
                .extracting(CommitteeMemberDetails::getColdKey)
                .isEqualTo("cold-key-3");
        assertThat(ratificationMembers).isEmpty();
    }

    private void assertActiveMember(List<CommitteeMemberDetails> members, String coldKey, String hotKey, int expiredEpoch) {
        assertThat(members).singleElement().satisfies(member -> {
            assertThat(member.getColdKey()).isEqualTo(coldKey);
            assertThat(member.getHotKey()).isEqualTo(hotKey);
            assertThat(member.getExpiredEpoch()).isEqualTo(expiredEpoch);
        });
    }

    private void saveCommitteeMember(String coldKey, int startEpoch, int expiredEpoch, long slot) {
        committeeMemberStorage.saveAll(List.of(CommitteeMember.builder()
                .hash(coldKey)
                .slot(slot)
                .credType(CredentialType.ADDR_KEYHASH)
                .startEpoch(startEpoch)
                .expiredEpoch(expiredEpoch)
                .epoch(startEpoch)
                .build()));
    }

    private void saveCommitteeRegistration(String coldKey, String hotKey, int epoch, long slot, int certIndex) {
        committeeRegistrationStorage.saveAll(List.of(CommitteeRegistration.builder()
                .txHash("tx-" + coldKey + "-" + certIndex)
                .certIndex(certIndex)
                .txIndex(0)
                .slot(slot)
                .coldKey(coldKey)
                .hotKey(hotKey)
                .credType(StakeCredType.ADDR_KEYHASH)
                .epoch(epoch)
                .build()));
    }

    @TestConfiguration
    static class JooqTestConfig {
        @Bean
        DefaultConfigurationCustomizer jooqSettings() {
            return configuration -> configuration.settings()
                    .withRenderQuotedNames(RenderQuotedNames.NEVER);
        }
    }
}
