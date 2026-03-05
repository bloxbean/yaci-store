package com.bloxbean.cardano.yaci.store.blockfrost.transaction.service;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.mapper.BFTransactionMapper;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.BFTransactionStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model.TxRedeemerPricesRaw;
import com.bloxbean.cardano.yaci.store.epoch.domain.EpochParam;
import com.bloxbean.cardano.yaci.store.epoch.storage.EpochParamStorage;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import com.bloxbean.cardano.yaci.store.metadata.storage.TxMetadataStorageReader;
import com.bloxbean.cardano.yaci.store.mir.domain.MirPot;
import com.bloxbean.cardano.yaci.store.mir.domain.MoveInstataneousReward;
import com.bloxbean.cardano.yaci.store.mir.storage.MIRStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BFTransactionService {

    private final BFTransactionStorageReader storageReader;
    private final BFTransactionMapper mapper;
    private final TransactionStorageReader transactionStorageReader;
    private final ObjectProvider<TxMetadataStorageReader> metadataStorageProvider;
    private final ObjectProvider<MIRStorageReader> mirStorageProvider;
    private final ObjectProvider<EpochParamStorage> epochParamStorageProvider;

    public BFTransactionDto getTransaction(String txHash) {
        var raw = storageReader.findTransactionByHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));

        List<BFAmountDto> outputAmounts = storageReader.findTxOutputAmounts(txHash);

        int mirCertCount = 0;
        MIRStorageReader mirReader = mirStorageProvider.getIfAvailable();
        if (mirReader != null) {
            try {
                mirCertCount = mirReader.findMIRsByTxHash(txHash).size();
            } catch (Exception e) {
                log.debug("Could not fetch MIR count for tx {}: {}", txHash, e.getMessage());
            }
        }

        String deposit = calculateDeposit(txHash);

        return mapper.toTransactionDto(raw, outputAmounts, deposit, mirCertCount);
    }

    public BFTxUtxosDto getTxUtxos(String txHash) {
        var raw = storageReader.findTxUtxos(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
        return mapper.toUtxosDto(txHash, raw);
    }

    public BFTxCborDto getTxCbor(String txHash) {
        // Verify transaction exists first
        transactionStorageReader.getTransactionByTxHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));

        String cborHex = storageReader.findTxCborHex(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "CBOR data not available. Enable store.transaction.save-cbor=true"));

        return BFTxCborDto.builder().cbor(cborHex).build();
    }

    public List<BFTxMetadataDto> getTxMetadata(String txHash) {
        TxMetadataStorageReader metadataReader = metadataStorageProvider.getIfAvailable();
        if (metadataReader == null) {
            return Collections.emptyList();
        }
        List<TxMetadataLabel> labels = metadataReader.findByTxHash(txHash);
        return labels.stream()
                .map(label -> BFTxMetadataDto.builder()
                        .label(label.getLabel())
                        .jsonMetadata(label.getBody())
                        .build())
                .collect(Collectors.toList());
    }

    public List<BFTxRedeemerDto> getTxRedeemers(String txHash) {
        ensureTxExists(txHash);
        var raws = storageReader.findTxRedeemers(txHash);
        TxRedeemerPricesRaw prices = storageReader.findRedeemerPrices(txHash).orElse(null);
        BigDecimal priceMem = prices != null ? prices.getPriceMem() : null;
        BigDecimal priceStep = prices != null ? prices.getPriceStep() : null;
        return mapper.toRedeemerDtos(raws, priceMem, priceStep);
    }

    public List<BFTxStakeDto> getTxStakes(String txHash) {
        ensureTxExists(txHash);
        return mapper.toStakeDtos(storageReader.findTxStakes(txHash));
    }

    public List<BFTxDelegationDto> getTxDelegations(String txHash) {
        ensureTxExists(txHash);
        return mapper.toDelegationDtos(storageReader.findTxDelegations(txHash));
    }

    public List<BFTxWithdrawalDto> getTxWithdrawals(String txHash) {
        ensureTxExists(txHash);
        return mapper.toWithdrawalDtos(storageReader.findTxWithdrawals(txHash));
    }

    public List<BFTxMirDto> getTxMirs(String txHash) {
        ensureTxExists(txHash);
        MIRStorageReader mirReader = mirStorageProvider.getIfAvailable();
        if (mirReader == null) {
            return Collections.emptyList();
        }
        return mirReader.findMIRsByTxHash(txHash).stream()
                .map(mir -> BFTxMirDto.builder()
                        .pot(mir.getPot() != null ? (MirPot.reserves == mir.getPot() ? "reserve" : mir.getPot().name()) : null)
                        .certIndex((int) mir.getCertIndex())
                        .address(mir.getAddress())
                        .amount(mir.getAmount() != null ? mir.getAmount().toString() : "0")
                        .build())
                .collect(Collectors.toList());
    }

    public List<BFTxPoolUpdateDto> getTxPoolUpdates(String txHash) {
        ensureTxExists(txHash);
        return mapper.toPoolUpdateDtos(storageReader.findTxPoolUpdates(txHash));
    }

    public List<BFTxPoolRetireDto> getTxPoolRetires(String txHash) {
        ensureTxExists(txHash);
        return mapper.toPoolRetireDtos(storageReader.findTxPoolRetires(txHash));
    }

    public List<Map<String, String>> getTxRequiredSigners(String txHash) {
        Txn txn = transactionStorageReader.getTransactionByTxHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
        if (txn.getRequiredSigners() == null) {
            return Collections.emptyList();
        }
        return txn.getRequiredSigners().stream()
                .map(signer -> Map.of("witness_hash", signer))
                .collect(Collectors.toList());
    }

    private void ensureTxExists(String txHash) {
        transactionStorageReader.getTransactionByTxHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
    }

    private String calculateDeposit(String txHash) {
        BigInteger deposit = BigInteger.ZERO;

        // DRep deposit does not depend on epoch params — compute it independently.
        try {
            BigInteger drepDeposit = storageReader.sumDrepDeposit(txHash);
            if (drepDeposit != null && drepDeposit.compareTo(BigInteger.ZERO) != 0) {
                deposit = deposit.add(drepDeposit);
            }
        } catch (Exception e) {
            log.warn("Could not calculate drep deposit for tx {}: {}", txHash, e.getMessage());
        }

        // Stake registration/deregistration and pool registration require epoch params.
        EpochParamStorage epochParamStorage = epochParamStorageProvider.getIfAvailable();
        if (epochParamStorage != null) {
            try {
                Txn txn = transactionStorageReader.getTransactionByTxHash(txHash).orElse(null);
                if (txn != null && txn.getEpoch() != null) {
                    EpochParam epochParam = epochParamStorage.getProtocolParams(txn.getEpoch()).orElse(null);
                    if (epochParam != null && epochParam.getParams() != null) {
                        BigInteger keyDeposit = epochParam.getParams().getKeyDeposit();
                        BigInteger poolDeposit = epochParam.getParams().getPoolDeposit();

                        int stakeRegCount = storageReader.countStakeRegistrations(txHash);
                        int stakeDeregCount = storageReader.countStakeDeregistrations(txHash);
                        int netStakeCount = stakeRegCount - stakeDeregCount;
                        int poolRegCount = storageReader.countPoolRegistrations(txHash);

                        if (keyDeposit != null && netStakeCount != 0) {
                            deposit = deposit.add(keyDeposit.multiply(BigInteger.valueOf(netStakeCount)));
                        }
                        if (poolDeposit != null && poolRegCount > 0) {
                            deposit = deposit.add(poolDeposit.multiply(BigInteger.valueOf(poolRegCount)));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not calculate stake/pool deposit for tx {}: {}", txHash, e.getMessage());
            }
        }

        return deposit.toString();
    }
}
