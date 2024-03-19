package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
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
@Table(name = "committee_registration")
@IdClass(CommitteeRegistrationId.class)
public class CommitteeRegistrationEntityJpa extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private long certIndex;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "cold_key")
    private String coldKey;

    @Column(name = "hot_key")
    private String hotKey;

    @Column(name = "cred_type")
    @Enumerated(EnumType.STRING)
    private StakeCredType credType;

    @Column(name = "epoch")
    private Integer epoch;
}
