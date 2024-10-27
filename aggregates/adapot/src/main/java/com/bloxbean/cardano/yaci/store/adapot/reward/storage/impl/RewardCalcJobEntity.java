package com.bloxbean.cardano.yaci.store.adapot.reward.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.reward.domain.RewardCalcStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "reward_calc_jobs")
public class RewardCalcJobEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RewardCalcStatus status;

    @Column(name = "time_taken")
    private Long timeTaken;

    @Column(name = "error_message")
    private String errorMessage;

    public RewardCalcJobEntity() {}

    // Getters and setters

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(Integer epoch) {
        this.epoch = epoch;
    }

    public Long getSlot() {
        return slot;
    }

    public void setSlot(Long slot) {
        this.slot = slot;
    }

    public RewardCalcStatus getStatus() {
        return status;
    }

    public void setStatus(RewardCalcStatus status) {
        this.status = status;
    }

    public Long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
