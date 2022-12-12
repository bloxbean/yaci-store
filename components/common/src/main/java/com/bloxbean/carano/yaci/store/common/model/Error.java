package com.bloxbean.carano.yaci.store.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Error extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "block")
    private Long block;

    @Column(name = "block_hash")
    private String blockHash;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "error_code")
    private String errorCode;

    @Lob
    @Column(name = "reason")
    private String reason;
}
