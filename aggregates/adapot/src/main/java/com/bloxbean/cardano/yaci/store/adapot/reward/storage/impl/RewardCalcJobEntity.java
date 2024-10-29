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

    @Column(name = "total_time")
    private Long totalTime;

    @Column(name = "reward_calc_time")
    private Long rewardCalcTime;

    @Column(name = "update_reward_time")
    private Long updateRewardTime;

    @Column(name = "stake_snapshot_time")
    private Long stakeSnapshotTime;

    @Column(name = "error_message")
    private String errorMessage;

    public RewardCalcJobEntity() {}

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

    public Long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Long totalTime) {
        this.totalTime = totalTime;
    }

    public Long getRewardCalcTime() {
        return rewardCalcTime;
    }

    public void setRewardCalcTime(Long rewardCalcTime) {
        this.rewardCalcTime = rewardCalcTime;
    }

    public Long getUpdateRewardTime() {
        return updateRewardTime;
    }

    public void setUpdateRewardTime(Long updateRewardTime) {
        this.updateRewardTime = updateRewardTime;
    }

    public Long getStakeSnapshotTime() {
        return stakeSnapshotTime;
    }

    public void setStakeSnapshotTime(Long stakeSnapshotTime) {
        this.stakeSnapshotTime = stakeSnapshotTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
