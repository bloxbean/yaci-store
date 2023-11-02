package com.bloxbean.cardano.yaci.store.transaction.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxWitnessType;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@IdClass(TxnWitnessId.class)
@Table(name = "transaction_witness")
public class TxnWitnessEntity {
    @Id
    @Column(name = "tx_hash")
    private String txHash;

    @Id
    @Column(name = "index")
    private Integer index;

    @Column(name = "pub_key")
    private String pubKey;

    @Column(name = "signature")
    private String signature;

    @Column(name = "pub_keyhash")
    private String pubKeyhash;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TxWitnessType type;

    @Type(JsonType.class)
    @Column(name = "additional_data")
    private JsonNode additionalData;

    @Column(name = "slot")
    private Long slot;
}
