package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "local_drep_dist")
@IdClass(LocalDRepDistrId.class)
public class LocalDRepDistrEntity {

    @Id
    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "drep_type")
    @Enumerated(EnumType.STRING)
    private DrepType drepType;

    @Column(name = "amount")
    private BigInteger amount;

    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "update_datetime")
    @UpdateTimestamp
    private LocalDateTime updateDatetime;
}
