package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "local_treasury_withdrawal")
@IdClass(LocalTreasuryWithdrawalId.class)
public class LocalTreasuryWithdrawalEntity {
    @Id
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Id
    @Column(name = "gov_action_index")
    private int govActionIndex;

    @Type(JsonType.class)
    @Column(columnDefinition = "json", name = "withdrawals")
    private JsonNode withdrawals;

    @Column(name = "epoch")
    private int epoch;

    @Column(name = "slot")
    private long slot;

    @Column(name = "update_datetime")
    @UpdateTimestamp
    private LocalDateTime updateDatetime;
}
