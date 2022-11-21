package com.bloxbean.cardano.yaci.indexer.entity;

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

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Transaction transaction;
}
