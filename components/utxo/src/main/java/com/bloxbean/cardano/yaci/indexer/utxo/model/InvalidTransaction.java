package com.bloxbean.cardano.yaci.indexer.utxo.model;

import com.bloxbean.carano.yaci.indexer.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class InvalidTransaction extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "slot")
    private long slot;

    @Column(name = "block_hash")
    private String blockHash;

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private Transaction transaction;
}
