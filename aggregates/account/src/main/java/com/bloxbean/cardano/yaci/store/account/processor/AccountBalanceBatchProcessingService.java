package com.bloxbean.cardano.yaci.store.account.processor;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountBalanceBatchProcessingService {
    private final AccountBalanceStorage accountBalanceStorage;
    private final UtxoStorage utxoStorage;
    private final AccountConfigService accountConfigService;

    private AtomicLong latestProcessedBlock = new AtomicLong(0L);
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    public void init() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void runBalanceCalculationBatch(Long maxBlockNumber, int blockBatchSize) {
        log.info("Starting balance calculation batch. Latest Processed Block : {} " + latestProcessedBlock.get());
        try {
            while (true) {
                Long _latestProcessedBlock = latestProcessedBlock.get();
                if (_latestProcessedBlock == 0) {
                    _latestProcessedBlock = accountBalanceStorage.getBalanceCalculationBlock();
                }

                if (_latestProcessedBlock == null)
                    _latestProcessedBlock = -2L; //as genesis balances start with block -1

                log.info("Starting balance calculation batch. Latest processed block {}", _latestProcessedBlock);
                long startBlock = _latestProcessedBlock + 1;

                if (startBlock > maxBlockNumber) {
                    log.info("Start block {} is greater than max block {}. Skipping for now", startBlock, maxBlockNumber);
                    break;
                }

                List<Long> blocks = utxoStorage.findNextAvailableBlocks(startBlock, blockBatchSize);
                if (blocks == null || blocks.size() == 0) {
                    log.info("No blocks found for balance calculation");
                    break;
                }

                Long endBlock = blocks.get(blocks.size() - 1);
                if (endBlock > maxBlockNumber) {
                    log.info("End block {} is greater than max block {}. Setting EndBlock to MaxBlockNumber", endBlock, maxBlockNumber);
                    endBlock = maxBlockNumber;
                }

                log.info("Total blocks to process {}", blocks.size());

                List<AddressUtxo> inputs = utxoStorage.findSpentUtxosBetweenBlocks(startBlock, endBlock);
                List<AddressUtxo> outputs = utxoStorage.findUnspentUtxosBetweenBlocks(startBlock, endBlock);

                log.info("Total inputs {} and outputs {} found for block {} to {}", inputs.size(), outputs.size(), startBlock, endBlock);

                if (inputs.size() == 0 && outputs.size() == 0) {
                    log.info("No inputs and outputs found for block {} to {}", startBlock, endBlock);
                    latestProcessedBlock.set(endBlock);
                    continue;
                }

                var addressBalances = handleAddressBalance(inputs, outputs);
                for (AddressBalance addressBalance : addressBalances) {
                    if (addressBalance.getQuantity().compareTo(BigInteger.ZERO) < 0) {
                        log.error("Negative balance found for address: " + addressBalance.getAddress() + " : " + addressBalance.getQuantity());
                        throw new IllegalArgumentException("Negative balance found for address: " + addressBalance.getAddress() + " : " + addressBalance.getQuantity());
                    }
                }

                //Stake address balances
                var stakeAddressBalance = handleStakeAddressBalance(inputs, outputs);
                for (StakeAddressBalance stakeAddrBalance : stakeAddressBalance) {
                    if (stakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) < 0) {
                        log.error("Negative balance found for address: " + stakeAddrBalance.getAddress() + " : " + stakeAddrBalance.getQuantity());
                        throw new IllegalArgumentException("Negative balance found for address: " + stakeAddrBalance.getAddress() + " : " + stakeAddrBalance.getQuantity());
                    }
                }

                final long _endBlock = endBlock;
                transactionTemplate.execute(status -> {
                    log.info("Before saving account & stake balances");
                    accountBalanceStorage.saveStakeAddressBalances(stakeAddressBalance);
                    accountBalanceStorage.saveAddressBalances(addressBalances);
                    accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK,null, _endBlock);
                    log.info("After saving account & stake balances");
                    return null;
                });

                if (outputs.size() > 0)
                    log.info("Account balance processing completed for block {} to {}", startBlock, endBlock);

                latestProcessedBlock.set(endBlock);
            }

            //Setting the final block number
            accountConfigService.upateConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK,null, maxBlockNumber);

            log.info("Account Balance aggregation completed till block {}", latestProcessedBlock.get());
        } catch (IllegalStateException e) {
            log.error("Skip for now due to exception : " + e.getMessage(), e);
        }
    }

    private List<AddressBalance> handleAddressBalance(List<AddressUtxo> inputs, List<AddressUtxo> outputs) {
        Map<String, AddressBalance> addressBalanceMap = new HashMap<>();

        //Update inputs
        for (AddressUtxo input : inputs) {
            if (input.getAmounts() == null) {
                log.error("Input amounts are null for tx: " + input.getTxHash());
                log.error("Input: " + input);
            }

            for (Amt amount : input.getAmounts()) {
                String key = getKey(input.getOwnerAddr(), amount.getUnit());
                if (addressBalanceMap.get(key) != null) {
                    AddressBalance addressBalance = addressBalanceMap.get(key);
                    addressBalance.setQuantity(addressBalance.getQuantity().subtract(amount.getQuantity()));
                } else {
                    accountBalanceStorage.getAddressBalance(input.getOwnerAddr(), amount.getUnit(), input.getSpentAtSlot() - 1)
                            .ifPresentOrElse(addressBalance -> {
                                BigInteger newBalance = addressBalance.getQuantity().subtract(amount.getQuantity());
                                AddressBalance newAddressBalance = AddressBalance.builder()
                                        .address(input.getOwnerAddr())
                                        .blockHash(input.getSpentAtBlockHash())
                                        .slot(input.getSpentAtSlot())
                                        .blockNumber(input.getSpentAtBlock())
                                        .blockTime(input.getSpentBlockTime())
                                        .epoch(input.getSpentEpoch())
                                        .paymentCredential(input.getOwnerPaymentCredential())
                                        .stakeAddress(input.getOwnerStakeAddr())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(newBalance)
                                        .build();
                                addressBalanceMap.put(key, newAddressBalance);

                            }, () -> {
                                AddressBalance newAddressBalance = AddressBalance.builder()
                                        .address(input.getOwnerAddr())
                                        .blockHash(input.getSpentAtBlockHash())
                                        .slot(input.getSpentAtSlot())
                                        .blockNumber(input.getSpentAtBlock())
                                        .blockTime(input.getSpentBlockTime())
                                        .epoch(input.getSpentEpoch())
                                        .paymentCredential(input.getOwnerPaymentCredential())
                                        .stakeAddress(input.getOwnerStakeAddr())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(BigInteger.ZERO.subtract(amount.getQuantity()))
                                        .build();
                                addressBalanceMap.put(key, newAddressBalance);
                            });
                }
            }
        }

        //Update outputs
        for (AddressUtxo output : outputs) {
            for (Amt amount : output.getAmounts()) {
                String key = getKey(output.getOwnerAddr(), amount.getUnit());
                if (addressBalanceMap.get(key) != null) {
                    AddressBalance addressBalance = addressBalanceMap.get(key);
                    addressBalance.setQuantity(addressBalance.getQuantity().add(amount.getQuantity()));
                } else {
                    accountBalanceStorage.getAddressBalance(output.getOwnerAddr(), amount.getUnit(), output.getSlot() - 1)
                            .ifPresentOrElse(addressBalance -> {
                                BigInteger newBalance = addressBalance.getQuantity().add(amount.getQuantity());
                                AddressBalance newAddressBalance = AddressBalance.builder()
                                        .address(output.getOwnerAddr())
                                        .blockHash(output.getBlockHash())
                                        .slot(output.getSlot())
                                        .blockNumber(output.getBlockNumber())
                                        .blockTime(output.getBlockTime())
                                        .epoch(output.getEpoch())
                                        .paymentCredential(output.getOwnerPaymentCredential())
                                        .stakeAddress(output.getOwnerStakeAddr())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(newBalance)
                                        .build();
                                addressBalanceMap.put(key, newAddressBalance);

                            }, () -> {
                                AddressBalance newAddressBalance = AddressBalance.builder()
                                        .address(output.getOwnerAddr())
                                        .blockHash(output.getBlockHash())
                                        .slot(output.getSlot())
                                        .blockNumber(output.getBlockNumber())
                                        .blockTime(output.getBlockTime())
                                        .epoch(output.getEpoch())
                                        .paymentCredential(output.getOwnerPaymentCredential())
                                        .stakeAddress(output.getOwnerStakeAddr())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(amount.getQuantity())
                                        .build();
                                addressBalanceMap.put(key, newAddressBalance);
                            });
                }
            }
        }

        return addressBalanceMap.values().stream().toList();
    }

    private List<StakeAddressBalance> handleStakeAddressBalance(List<AddressUtxo> inputs, List<AddressUtxo> outputs) {
        Map<String, StakeAddressBalance> stakeBalanceMap = new HashMap<>();

        //Update inputs
        for (AddressUtxo input : inputs) {
            if (StringUtil.isEmpty(input.getOwnerStakeAddr())) //Don't process if stake address is empty
                continue;

            for (Amt amount : input.getAmounts()) {
                String key = getKey(input.getOwnerStakeAddr(), amount.getUnit());
                if (stakeBalanceMap.get(key) != null) {
                    StakeAddressBalance addressBalance = stakeBalanceMap.get(key);
                    addressBalance.setQuantity(addressBalance.getQuantity().subtract(amount.getQuantity()));
                } else {
                    accountBalanceStorage.getStakeAddressBalance(input.getOwnerStakeAddr(), amount.getUnit(), input.getSpentAtSlot() - 1)
                            .ifPresentOrElse(stakeAddrBalance -> {
                                BigInteger newBalance = stakeAddrBalance.getQuantity().subtract(amount.getQuantity());
                                StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                                        .address(input.getOwnerStakeAddr())
                                        .blockHash(input.getSpentAtBlockHash())
                                        .slot(input.getSpentAtSlot())
                                        .blockNumber(input.getSpentAtBlock())
                                        .blockTime(input.getSpentBlockTime())
                                        .epoch(input.getSpentEpoch())
                                        .stakeCredential(input.getOwnerStakeCredential())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(newBalance)
                                        .build();
                                stakeBalanceMap.put(key, newStakeAddrBalance);

                            }, () -> {
                                StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                                        .address(input.getOwnerStakeAddr())
                                        .blockHash(input.getSpentAtBlockHash())
                                        .slot(input.getSpentAtSlot())
                                        .blockNumber(input.getSpentAtBlock())
                                        .blockTime(input.getSpentBlockTime())
                                        .epoch(input.getSpentEpoch())
                                        .stakeCredential(input.getOwnerStakeCredential())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(BigInteger.ZERO.subtract(amount.getQuantity()))
                                        .build();
                                stakeBalanceMap.put(key, newStakeAddrBalance);
                            });
                }
            }
        }

        //Update outputs
        for (AddressUtxo output : outputs) {
            if (StringUtil.isEmpty(output.getOwnerStakeAddr())) //Don't process if stake address is empty
                continue;

            for (Amt amount : output.getAmounts()) {
                String key = getKey(output.getOwnerStakeAddr(), amount.getUnit());
                if (stakeBalanceMap.get(key) != null) {
                    StakeAddressBalance stakeAddrBalance = stakeBalanceMap.get(key);
                    stakeAddrBalance.setQuantity(stakeAddrBalance.getQuantity().add(amount.getQuantity()));
                } else {
                    accountBalanceStorage.getStakeAddressBalance(output.getOwnerStakeAddr(), amount.getUnit(), output.getSlot() - 1)
                            .ifPresentOrElse(stakeAddrBalance -> {
                                BigInteger newBalance = stakeAddrBalance.getQuantity().add(amount.getQuantity());
                                StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                                        .address(output.getOwnerStakeAddr())
                                        .blockHash(output.getBlockHash())
                                        .slot(output.getSlot())
                                        .blockNumber(output.getBlockNumber())
                                        .blockTime(output.getBlockTime())
                                        .epoch(output.getEpoch())
                                        .stakeCredential(output.getOwnerStakeCredential())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(newBalance)
                                        .build();
                                stakeBalanceMap.put(key, newStakeAddrBalance);

                            }, () -> {
                                StakeAddressBalance newStakeAddrBalance = StakeAddressBalance.builder()
                                        .address(output.getOwnerStakeAddr())
                                        .blockHash(output.getBlockHash())
                                        .slot(output.getSlot())
                                        .blockNumber(output.getBlockNumber())
                                        .blockTime(output.getBlockTime())
                                        .epoch(output.getEpoch())
                                        .stakeCredential(output.getOwnerStakeCredential())
                                        .unit(amount.getUnit())
                                        .policy(amount.getPolicyId())
                                        .assetName(amount.getAssetName())
                                        .quantity(amount.getQuantity())
                                        .build();
                                stakeBalanceMap.put(key, newStakeAddrBalance);
                            });
                }
            }
        }


        return stakeBalanceMap.values().stream().toList();
    }

    private String getKey(String address, String unit) {
        return address + "-" + unit;
    }
}
