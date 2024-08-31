package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "committee")
public class CommitteeEntity {

    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Column(name = "gov_action_index")
    private Integer govActionIndex;

    @Column(name = "threshold_numerator")
    private BigInteger thresholdNumerator;

    @Column(name = "threshold_denominator")
    private BigInteger thresholdDenominator;

    @Column(name = "threshold")
    private Double threshold;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "update_datetime")
    @UpdateTimestamp
    private LocalDateTime updateDatetime;
}
