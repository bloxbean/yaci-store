package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
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
@Table(name = "drep")
@IdClass(DRepId.class)
public class DRepEntity extends BlockAwareEntity {
    @Id
    @Column(name = "drep_id")
    private String drepId;

    @Column(name = "drep_hash")
    private String drepHash;

    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private Integer certIndex;

    @Column(name = "tx_index")
    private Integer txIndex;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "cert_type")
    @Enumerated(EnumType.STRING)
    private CertificateType certType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DRepStatus status;

    @Column(name = "deposit")
    private BigInteger deposit;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "active_epoch")
    private Integer activeEpoch;

    @Column(name = "inactive_epoch")
    private Integer inactiveEpoch;

    @Column(name = "retire_epoch")
    private Integer retireEpoch;

    @Column(name = "registration_slot")
    private Long registrationSlot;

    @Column(name = "block_hash")
    private String blockHash;
}
