package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stake_registration")
@IdClass(StakeRegistrationId.class)
public class StakeRegistrationEntity extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "cert_index")
    private long certIndex;

    @Column(name = "credential")
    private String credential;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private CertificateType type;

    @Column(name = "address")
    private String address;

    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block")
    private Long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "block_time")
    private long blockTime;
}
