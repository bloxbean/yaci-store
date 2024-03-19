package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.certs.StakeCredType;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "drep_registration")
@IdClass(DRepRegistrationId.class)
public class DRepRegistrationEntityJpa extends BlockAwareEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private long certIndex;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private CertificateType type;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "deposit")
    private BigInteger deposit;

    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "drep_id")
    private String drepId;

    @Column(name = "anchor_url")
    private String anchorUrl;

    @Column(name = "anchor_hash")
    private String anchorHash;

    @Column(name = "cred_type")
    @Enumerated(EnumType.STRING)
    private StakeCredType credType;

    @Column(name = "epoch")
    private Integer epoch;
}
