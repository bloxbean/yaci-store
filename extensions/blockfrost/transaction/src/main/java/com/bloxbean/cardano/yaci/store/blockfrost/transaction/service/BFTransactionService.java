package com.bloxbean.cardano.yaci.store.blockfrost.transaction.service;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.BFTransactionStorageReader;
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
    private final TransactionStorageReader transactionStorageReader;
    private final ObjectProvider<TxMetadataStorageReader> metadataStorageProvider;
    private final ObjectProvider<MIRStorageReader> mirStorageProvider;
    private final ObjectProvider<EpochParamStorage> epochParamStorageProvider;

    public BFTransactionDto getTransaction(String txHash) {
        BFTransactionDto dto = storageReader.findTransactionByHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));

        // Resolve output_amount — lovelace first, then assets sorted by unit
        Map<String, BigInteger> outputAmounts = storageReader.findTxOutputAmounts(txHash);
        List<BFAmountDto> amountList = outputAmounts.entrySet().stream()
                .sorted((a, b) -> {
                    if ("lovelace".equals(a.getKey())) return -1;
                    if ("lovelace".equals(b.getKey())) return 1;
                    return a.getKey().compareTo(b.getKey());
                })
                .map(e -> BFAmountDto.builder()
                        .unit(e.getKey())
                        .quantity(e.getValue().toString())
                        .build())
                .collect(Collectors.toList());
        dto.setOutputAmount(amountList);

        // Resolve mir_cert_count from optional MIR store
        MIRStorageReader mirReader = mirStorageProvider.getIfAvailable();
        if (mirReader != null) {
            try {
                dto.setMirCertCount(mirReader.findMIRsByTxHash(txHash).size());
            } catch (Exception e) {
                log.debug("Could not fetch MIR count for tx {}: {}", txHash, e.getMessage());
                dto.setMirCertCount(0);
            }
        } else {
            dto.setMirCertCount(0);
        }

        // Resolve deposit using protocol params
        dto.setDeposit(calculateDeposit(txHash, dto));

        return dto;
    }

    public BFTxUtxosDto getTxUtxos(String txHash) {
        return storageReader.findTxUtxos(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The requested component has not been found."));
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
        return storageReader.findTxRedeemers(txHash);
    }

    public List<BFTxStakeDto> getTxStakes(String txHash) {
        ensureTxExists(txHash);
        return storageReader.findTxStakes(txHash);
    }

    public List<BFTxDelegationDto> getTxDelegations(String txHash) {
        ensureTxExists(txHash);
        return storageReader.findTxDelegations(txHash);
    }

    public List<BFTxWithdrawalDto> getTxWithdrawals(String txHash) {
        ensureTxExists(txHash);
        return storageReader.findTxWithdrawals(txHash);
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
        return storageReader.findTxPoolUpdates(txHash);
    }

    public List<BFTxPoolRetireDto> getTxPoolRetires(String txHash) {
        ensureTxExists(txHash);
        return storageReader.findTxPoolRetires(txHash);
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

    private String calculateDeposit(String txHash, BFTransactionDto dto) {
        EpochParamStorage epochParamStorage = epochParamStorageProvider.getIfAvailable();
        if (epochParamStorage == null) {
            return "0";
        }

        try {
            // Get epoch from transaction (need to query it)
            Txn txn = transactionStorageReader.getTransactionByTxHash(txHash).orElse(null);
            if (txn == null || txn.getEpoch() == null) {
                return "0";
            }

            EpochParam epochParam = epochParamStorage.getProtocolParams(txn.getEpoch()).orElse(null);
            if (epochParam == null || epochParam.getParams() == null) {
                return "0";
            }

            BigInteger keyDeposit = epochParam.getParams().getKeyDeposit();
            BigInteger poolDeposit = epochParam.getParams().getPoolDeposit();

            int stakeRegCount = storageReader.countStakeRegistrations(txHash);
            int poolRegCount = storageReader.countPoolRegistrations(txHash);

            BigInteger deposit = BigInteger.ZERO;
            if (keyDeposit != null && stakeRegCount > 0) {
                deposit = deposit.add(keyDeposit.multiply(BigInteger.valueOf(stakeRegCount)));
            }
            if (poolDeposit != null && poolRegCount > 0) {
                deposit = deposit.add(poolDeposit.multiply(BigInteger.valueOf(poolRegCount)));
            }
            return deposit.toString();
        } catch (Exception e) {
            log.debug("Could not calculate deposit for tx {}: {}", txHash, e.getMessage());
            return "0";
        }
    }
}
