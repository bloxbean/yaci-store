package com.bloxbean.cardano.yaci.store.adapot.processor;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.adapot.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardAccountStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.WithdrawalStorage;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawalProcessor {
    private final WithdrawalStorage withdrawalStorage;
    private final RewardAccountStorage rewardAccountStorage;

    @EventListener
    @Transactional
    public void processWithdrawal(TransactionEvent transactionEvent) {
        var metadata = transactionEvent.getMetadata();

        var transactions = transactionEvent.getTransactions();

        List<Withdrawal> withdrawals = null;
        for (var transaction : transactions) {
            Map<String, BigInteger> txWithdrawals = transaction.getBody().getWithdrawals();
            if (txWithdrawals == null || txWithdrawals.isEmpty())
                continue;

            if (withdrawals == null)
                withdrawals = new ArrayList<>();

            var withdrawalsToSave = txWithdrawals.entrySet().stream()
                    .map(entry -> {
                        var addressHex = entry.getKey();
                        var amount = entry.getValue();

                        Address address = new Address(HexUtil.decodeHexString(addressHex));//AddressProvider.getRewardAddress(toCCLCredential(stakeCredCoinEntry.getKey()), eventMetadata.isMainnet() ? Networks.mainnet() : Networks.testnet());
                        String stakeAddress = address.toBech32();

                        var withdrawal = new Withdrawal();
                        withdrawal.setAddress(stakeAddress);
                        withdrawal.setAmount(amount);
                        withdrawal.setTxHash(transaction.getTxHash());
                        withdrawal.setSlot(metadata.getSlot());
                        withdrawal.setEpoch(metadata.getEpochNumber());
                        withdrawal.setBlockNumber(metadata.getBlock());
                        withdrawal.setBlockTime(metadata.getBlockTime());
                        return withdrawal;
                    }).toList();

            if (withdrawalsToSave.size() > 0) {
                withdrawals.addAll(withdrawalsToSave);
            }
        }

        if (withdrawals != null && withdrawals.size() > 0)
            withdrawalStorage.save(withdrawals);

        //update reward account
        if (withdrawals != null && withdrawals.size() > 0) {
            rewardAccountStorage.withdrawReward(withdrawals);
        }
    }

    @EventListener
    @Transactional
    public void handleRollbackEvent(RollbackEvent rollbackEvent) {
        int count = withdrawalStorage.deleteBySlotGreaterThan(rollbackEvent.getRollbackTo().getSlot());
        log.info("Rollback -- {} withdrawal records", count);
    }
}
