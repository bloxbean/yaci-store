package com.bloxbean.cardano.yaci.store.utxo.storage.impl.model;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
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
@Table(name = "invalid_transaction")
public class InvalidTransactionEntity extends BaseEntity {
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
