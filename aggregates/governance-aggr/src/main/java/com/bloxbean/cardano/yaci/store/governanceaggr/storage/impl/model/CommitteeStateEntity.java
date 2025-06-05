package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.store.governancerules.domain.ConstitutionCommitteeState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "committee_state")
public class CommitteeStateEntity {
    @Id
    private Integer epoch;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private ConstitutionCommitteeState state;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;
}
