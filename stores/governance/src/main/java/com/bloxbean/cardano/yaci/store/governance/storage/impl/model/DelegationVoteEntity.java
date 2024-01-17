package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "delegation_vote")
@IdClass(DelegationVoteId.class)
public class DelegationVoteEntity extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private long certIndex;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "address")
    private String address;

    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "drep_view")
    private String drepView;
}
