package com.bloxbean.cardano.yaci.indexer.blocks.model;

import com.bloxbean.carano.yaci.indexer.common.model.BaseEntity;
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
    private long block;

    @Column(name = "slot")
    private long slot;

    @Column(name = "epoch")
    private long epoch;

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
    private long blockBodySize;

    @Column(name="body_hash")
    private String blockBodyHash;

    @Column(name = "protocol_version")
    private String protocolVersion;

    @Column(name = "no_of_txs")
    private int noOfTxs;
}
