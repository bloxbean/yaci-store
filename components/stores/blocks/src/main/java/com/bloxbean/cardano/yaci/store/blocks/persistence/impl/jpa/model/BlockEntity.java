package com.bloxbean.cardano.yaci.store.blocks.persistence.impl.jpa.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;


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

    @Type(JsonType.class)
    @Column(name = "nonce_vrf")
    private Vrf nonceVrf;

    @Type(JsonType.class)
    @Column(name="leader_vrf")
    private Vrf leaderVrf;

    @Type(JsonType.class)
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
