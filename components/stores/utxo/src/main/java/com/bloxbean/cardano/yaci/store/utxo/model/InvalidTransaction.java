package com.bloxbean.cardano.yaci.store.utxo.model;

import com.bloxbean.carano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
public class InvalidTransaction extends BaseEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block_hash")
    private String blockHash;

    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private Transaction transaction;
}
