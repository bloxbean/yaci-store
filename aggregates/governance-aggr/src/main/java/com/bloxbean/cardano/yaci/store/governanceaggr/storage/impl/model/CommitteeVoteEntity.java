package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "committee_vote")
@IdClass(CommitteeVoteId.class)
public class CommitteeVoteEntity {

    @Id
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Id
    @Column(name = "gov_action_index")
    private int govActionIndex;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "yes_cnt")
    private int yesCnt;

    @Column(name = "no_cnt")
    private int noCnt;

    @Column(name = "abstain_cnt")
    private int abstainCnt;

    @Column(name = "voter_hash")
    private String voterHash;

    @Column(name = "vote")
    @Enumerated(EnumType.STRING)
    private Vote vote;

    @Column(name = "status")
    private String status;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;
}
