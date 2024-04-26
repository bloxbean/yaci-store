package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "voting_procedure")
@IdClass(LatestVotingProcedureId.class)
public class LatestVotingProcedureEntity extends BlockAwareEntity {
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "idx")
    private long index;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "voter_hash",
                    column = @Column(name = "voter_hash", insertable = false, updatable = false)),
            @AttributeOverride(
                    name = "gov_action_tx_hash",
                    column = @Column(name = "gov_action_tx_hash", insertable = false, updatable = false)),
            @AttributeOverride(
                    name = "gov_action_index",
                    column = @Column(name = "gov_action_index", insertable = false, updatable = false))
    })
    private LatestVotingProcedureId latestVotingProcedureId;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "voter_type")
    @Enumerated(EnumType.STRING)
    private VoterType voterType;

    @Id
    @Column(name = "voter_hash")
    private String voterHash;

    @Id
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Id
    @Column(name = "gov_action_index")
    private Integer govActionIndex;

    @Column(name = "vote")
    @Enumerated(EnumType.STRING)
    private Vote vote;

    @Column(name = "anchor_url")
    private String anchorUrl;

    @Column(name = "anchor_hash")
    private String anchorHash;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "repeat_vote")
    private Boolean repeatVote;
}
