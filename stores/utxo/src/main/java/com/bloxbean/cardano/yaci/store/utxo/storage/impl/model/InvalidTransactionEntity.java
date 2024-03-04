package com.bloxbean.cardano.yaci.store.utxo.storage.impl.model;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
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

    @PrePersist
    public void prePersist() {
        if (transaction == null) return;

        var json = JsonUtil.getJson(transaction);
        if (json.contains("\\u0000")) {
            json = json.replace("\\u0000", "");

            try {
                var updatedTransaction = JsonUtil.getMapper().readValue(json, Transaction.class);
                setTransaction(updatedTransaction);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error deserializing transaction json", e);
            }
        }
    }
}
