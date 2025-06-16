package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

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
@IdClass(VotingProcedureId.class)
public class VotingProcedureEntity extends BlockAwareEntity {

    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "idx")
    private long index;

    @Column(name = "slot")
    private Long slot;

    @Id
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
}

