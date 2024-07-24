package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "local_committee_member")
@IdClass(LocalCommitteeMemberId.class)
public class LocalCommitteeMemberEntity {

    @Id
    @Column(name = "hash")
    private String hash;

    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "cred_type")
    @Enumerated(EnumType.STRING)
    private CredentialType credType;

    @Column(name = "expired_epoch")
    private Integer expiredEpoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "update_datetime")
    @UpdateTimestamp
    private LocalDateTime updateDatetime;
}
