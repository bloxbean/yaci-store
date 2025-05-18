package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
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

    @Id
    @Column(name = "drep_hash")
    private String drepHash;

    @Id
    @Column(name = "drep_type")
    @Enumerated(EnumType.STRING)
    private DrepType drepType;

    @Column(name = "drep_id")
    private String drepId;

    @Column(name = "amount")
    private BigInteger amount;

    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "active_until")
    private Integer activeUntil;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;
}
