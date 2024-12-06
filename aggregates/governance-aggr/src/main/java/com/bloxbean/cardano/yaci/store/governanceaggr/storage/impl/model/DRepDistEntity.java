package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.*;
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
@Table(name = "drep_dist")
@IdClass(DRepDistId.class)
public class DRepDistEntity {

    @Column(name = "drep_id")
    private String drepId;

    @Id
    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "amount")
    private BigInteger amount;

    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;
}
