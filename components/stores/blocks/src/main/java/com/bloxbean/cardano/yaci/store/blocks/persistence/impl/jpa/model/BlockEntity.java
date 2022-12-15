package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "block")
public class BlockEntity extends BaseEntity {
    @Id
    @Column(name = "hash")
    private String blockHash;

    @Column(name = "number")
    private Long block;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "epoch")
    private Long epoch;

    @Column(name = "era")
    private Integer era;

    @Column(name = "prev_hash")
    private String prevHash;

    @Column(name = "issuer_vkey")
    private String issuerVkey;

    @Column(name = "vrf_vkey")
    private String vrfVkey;

    @Type(type = "json")
    @Column(name = "nonce_vrf")
    private Vrf nonceVrf;

    @Type(type = "json")
    @Column(name="leader_vrf")
    private Vrf leaderVrf;

    @Type(type = "json")
    @Column(name="vrf_result")
    private Vrf vrfResult;

    @Column(name = "body_size")
    private Long blockBodySize;

    @Column(name="body_hash")
    private String blockBodyHash;

    @Column(name = "protocol_version")
    private String protocolVersion;

    @Column(name = "no_of_txs")
    private Integer noOfTxs;
}
