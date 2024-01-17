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
@Table(name = "committee_de_registration")
@IdClass(CommitteeDeRegistrationId.class)
public class CommitteeDeRegistrationEntity extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private long certIndex;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "anchor_url")
    private String anchorUrl;

    @Column(name = "anchor_hash")
    private String anchorHash;

    @Column(name = "cold_key")
    private String coldKey;
}
