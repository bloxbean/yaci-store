package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.CredentialType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "committee_member")
@IdClass(CommitteeMemberId.class)
public class CommitteeMemberEntity {
    @Id
    @Column(name = "hash")
    private String hash;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "cred_type")
    @Enumerated(EnumType.STRING)
    private CredentialType credType;

    @Column(name = "expired_epoch")
    private Integer expiredEpoch;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;
}
