package com.bloxbean.cardano.yaci.store.epochnonce.storage.impl.model;

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
@Table(name = "epoch_nonce")
public class EpochNonceEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "nonce")
    private String nonce;

    @Column(name = "evolving_nonce")
    private String evolvingNonce;

    @Column(name = "candidate_nonce")
    private String candidateNonce;

    @Column(name = "lab_nonce")
    private String labNonce;

    @Column(name = "last_epoch_block_nonce")
    private String lastEpochBlockNonce;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block")
    private Long block;

    @Column(name = "block_time")
    private Long blockTime;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDatetime;
}
