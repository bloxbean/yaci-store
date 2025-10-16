package com.bloxbean.cardano.yaci.store.cip139.utxo.service;

import com.bloxbean.cardano.yaci.store.cip139.utxo.dto.UtxoDto;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorageReader;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class Cip139UtxoService {

    private static final int RECORDS_PER_PAGE_COUNT = 100;
    private final TransactionStorageReader transactionStorageReader;
    private final UtxoStorageReader utxoStorageReader;

    public UtxoDto getUtxoByTransactionHash(String txHash){
        Optional<Txn> txnOptional = transactionStorageReader.getTransactionByTxHash(txHash);
        if (txnOptional.isPresent()){
            List<UtxoKey> inputs = txnOptional.get().getInputs();
            List<AddressUtxo> outputs = utxoStorageReader.findAllByIds(inputs);
            return UtxoDto.fromDomain(outputs);
        } else {
            return null;
        }
    }

    public UtxoDto getUtxoByAddress(String address){
        List<AddressUtxo> addressUtxos = new ArrayList<>();
        int currentPage = 1;
        while(true){
            var paginatedUtxos = utxoStorageReader.findUtxoByAddress(address, currentPage, RECORDS_PER_PAGE_COUNT, null);
            if (paginatedUtxos.isEmpty()) {
                break;
            } else {
                addressUtxos.addAll(paginatedUtxos);
                currentPage++;
            }
        }

        return UtxoDto.fromDomain(addressUtxos);
    }

    public UtxoDto getUtxoByPaymentCredential(String paymentCredential){

        List<AddressUtxo> addressUtxos = new ArrayList<>();
        int currentPage = 1;
        while(true){
            var paginatedUtxos = utxoStorageReader.findUtxoByPaymentCredential(paymentCredential, currentPage, RECORDS_PER_PAGE_COUNT, null);
            if (paginatedUtxos.isEmpty()) {
                break;
            } else {
                addressUtxos.addAll(paginatedUtxos);
                currentPage++;
            }
        }

        return UtxoDto.fromDomain(addressUtxos);
    }

    public UtxoDto getUtxoByStakeCredential(String rewardAddress){

        List<AddressUtxo> addressUtxos = new ArrayList<>();
        int currentPage = 1;
        while(true){
            var paginatedUtxos = utxoStorageReader.findUtxoByStakeAddress(rewardAddress, currentPage, RECORDS_PER_PAGE_COUNT, null);
            if (paginatedUtxos.isEmpty()) {
                break;
            } else {
                addressUtxos.addAll(paginatedUtxos);
                currentPage++;
            }
        }

        return UtxoDto.fromDomain(addressUtxos);
    }

    public UtxoDto getUtxoByAsset(String assetName, String mintingPolicyHash){

        String unit = assetName + mintingPolicyHash;
        List<AddressUtxo> addressUtxos = new ArrayList<>();
        int currentPage = 1;
        while(true){
            var paginatedUtxos = utxoStorageReader.findUtxosByAsset(unit, currentPage, RECORDS_PER_PAGE_COUNT, null);
            if (paginatedUtxos.isEmpty()) {
                break;
            } else {
                addressUtxos.addAll(paginatedUtxos);
                currentPage++;
            }
        }

        return UtxoDto.fromDomain(addressUtxos);
    }

}
