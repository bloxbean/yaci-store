package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.UnclaimedRewardRestRepository;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jooq.DSLContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;

@RequiredArgsConstructor
public class RewardStorageImpl implements RewardStorage {
    private final InstantRewardRepository instantRewardRepository;
    private final RewardRestRepository rewardRestRepository;
    private final RewardRepository rewardRepository;
    private final UnclaimedRewardRestRepository unclaimedRewardRestRepository;
    private final Mapper mapper;
    private final DSLContext dsl;

    @Override
    public void saveInstantRewards(List<InstantReward> rewards) {
        instantRewardRepository.saveAll(rewards.stream().map(mapper::toInstantRewardEntity).toList());
    }

    @Override
    public void saveRewardRest(List<RewardRest> rewards) {
        rewardRestRepository.saveAll(rewards.stream().map(mapper::toRewardRestEntity).toList());
    }

    @Override
    public void saveRewards(List<Reward> rewards) {
        var inserts = rewards.stream()
                .map(reward -> dsl.insertInto(REWARD)
                        .set(REWARD.ADDRESS, reward.getAddress())
                        .set(REWARD.EARNED_EPOCH, reward.getEarnedEpoch())
                        .set(REWARD.TYPE, reward.getType().toString())
                        .set(REWARD.POOL_ID, reward.getPoolId())
                        .set(REWARD.AMOUNT, reward.getAmount())
                        .set(REWARD.SPENDABLE_EPOCH, reward.getSpendableEpoch())
                        .set(REWARD.SLOT, reward.getSlot())
                        .onDuplicateKeyUpdate()
                        .set(REWARD.ADDRESS, reward.getAddress())
                        .set(REWARD.EARNED_EPOCH, reward.getEarnedEpoch())
                        .set(REWARD.TYPE, reward.getType().toString())
                        .set(REWARD.POOL_ID, reward.getPoolId())
                        .set(REWARD.AMOUNT, reward.getAmount())
                        .set(REWARD.SPENDABLE_EPOCH, reward.getSpendableEpoch())
                        .set(REWARD.SLOT, reward.getSlot())).toList();

        dsl.batch(inserts).execute();
    }

    @Override
    public void bulkSaveRewards(List<Reward> rewards, int batchSize) {
        var currentTime = LocalDateTime.now();
        var rewardRecords = rewards.stream()
                .map(reward -> {
                    var rewardRecord = dsl.newRecord(REWARD);
                    rewardRecord.setAddress(reward.getAddress());
                    rewardRecord.setEarnedEpoch(reward.getEarnedEpoch());
                    rewardRecord.setType(reward.getType().toString());
                    rewardRecord.setPoolId(reward.getPoolId());
                    rewardRecord.setAmount(reward.getAmount());
                    rewardRecord.setSpendableEpoch(reward.getSpendableEpoch());
                    rewardRecord.setSlot(reward.getSlot());
                    rewardRecord.setUpdateDatetime(currentTime);
                    return rewardRecord;
                });

        try {
            dsl.loadInto(REWARD)
                    //.bulkAfter(batchSize)
                    //.batchAfter(batchSize)
                    .commitAfter(batchSize)
                    .loadRecords(rewardRecords)
                    .fields(REWARD.ADDRESS, REWARD.EARNED_EPOCH, REWARD.TYPE, REWARD.POOL_ID, REWARD.AMOUNT, REWARD.SPENDABLE_EPOCH, REWARD.SLOT)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException("Reward data could not be loaded", e);
        }
    }

    public void bulkSaveRewardsWithCopy(List<Reward> rewards) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LocalDateTime now = LocalDateTime.now();
        try (CSVPrinter csv = new CSVPrinter(
                new OutputStreamWriter(baos, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT
                        .withHeader(
                                "address",
                                "earned_epoch",
                                "type",
                                "pool_id",
                                "amount",
                                "spendable_epoch",
                                "slot",
                                "update_datetime"
                        )
                        .withTrim()
        )) {
            for (Reward r : rewards) {
                csv.printRecord(
                        r.getAddress(),
                        r.getEarnedEpoch(),
                        r.getType().toString(),
                        r.getPoolId(),
                        r.getAmount(),
                        r.getSpendableEpoch(),
                        r.getSlot(),
                        now
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV for rewards", e);
        }

        try (var in = new ByteArrayInputStream(baos.toByteArray())) {
            dsl.loadInto(REWARD)
                    .loadCSV(in)              // <— triggers COPY FROM STDIN on Postgres
                    .fields(
                            REWARD.ADDRESS,
                            REWARD.EARNED_EPOCH,
                            REWARD.TYPE,
                            REWARD.POOL_ID,
                            REWARD.AMOUNT,
                            REWARD.SPENDABLE_EPOCH,
                            REWARD.SLOT,
                            REWARD.UPDATE_DATETIME
                    )
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException("Reward data could not be loaded via COPY", e);
        }
    }

    @Override
    public void saveUnclaimedRewardRest(List<UnclaimedRewardRest> unclaimedRewards) {
        unclaimedRewardRestRepository.saveAll(unclaimedRewards.stream().map(mapper::toUnclaimedRewardRestEntity).toList());
    }

    @Override
    public List<UnclaimedRewardRest> findUnclaimedRewardRest(int spendableEpoch) {
        return unclaimedRewardRestRepository.findBySpendableEpoch(spendableEpoch)
                .stream().map(mapper::toUnclaimedRewardRest)
                .toList();
    }

    @Override
    public List<RewardRest> findTreasuryWithdrawals(int spendableEpoch) {
        return rewardRestRepository.findBySpendableEpochAndType(spendableEpoch, RewardRestType.treasury)
                .stream().map(mapper::toRewardRest)
                .toList();
    }

    @Override
    public int deleteLeaderMemberRewards(int earnedEpoch) {
        return rewardRepository.deleteLeaderMemberRewards(earnedEpoch);
    }

    @Override
    public int deleteRewardRest(int earnedEpoch, RewardRestType type) {
        return rewardRestRepository.deleteByEarnedEpochAndType(earnedEpoch, type);
    }

    @Override
    public int deleteUnclaimedRewardRest(int earnedEpoch, RewardRestType type) {
        return unclaimedRewardRestRepository.deleteByEarnedEpochAndType(earnedEpoch, type);
    }

    @Override
    public int deleteInstantRewardsBySlotGreaterThan(long slot) {
        return instantRewardRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteRewardsBySlotGreaterThan(long slot) {
        return rewardRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteRewardRestsBySlotGreaterThan(long slot) {
        return rewardRestRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteUnclaimedRewardsBySlotGreaterThan(long slot) {
        return unclaimedRewardRestRepository.deleteBySlotGreaterThan(slot);
    }

}
