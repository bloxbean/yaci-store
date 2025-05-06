package com.bloxbean.cardano.yaci.store.adapot.job.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobExtraInfo;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobStatus;
import com.bloxbean.cardano.yaci.store.adapot.job.domain.AdaPotJobType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "adapot_jobs")
public class AdaPotJobEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "block")
    private Long block;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AdaPotJobType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdaPotJobStatus status;

    @Column(name = "total_time")
    private Long totalTime;

    @Column(name = "reward_calc_time")
    private Long rewardCalcTime;

    @Column(name = "update_reward_time")
    private Long updateRewardTime;

    @Column(name = "stake_snapshot_time")
    private Long stakeSnapshotTime;

    @Column(name = "drep_distr_snapshot_time")
    private Long drepDistrSnapshotTime;

    @Type(JsonType.class)
    @Column(name = "extra_info")
    private AdaPotJobExtraInfo extraInfo;

    @Column(name = "error_message")
    private String errorMessage;

    public AdaPotJobEntity() {}

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

    public Long getBlock() {
        return block;
    }

    public void setBlock(Long block) {
        this.block = block;
    }

    public AdaPotJobType getType() {
        return type;
    }

    public void setType(AdaPotJobType type) {
        this.type = type;
    }

    public AdaPotJobStatus getStatus() {
        return status;
    }

    public void setStatus(AdaPotJobStatus status) {
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

    public Long getDrepDistrSnapshotTime() {
        return drepDistrSnapshotTime;
    }

    public void setDrepDistrSnapshotTime(Long dRepDistrSnapshotTime) {
        this.drepDistrSnapshotTime = dRepDistrSnapshotTime;
    }

    public AdaPotJobExtraInfo getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(AdaPotJobExtraInfo extraInfo) {
        this.extraInfo = extraInfo;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
