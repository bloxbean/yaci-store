package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.springframework.context.event.EventListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

@RequiredArgsConstructor
@Slf4j
public class UtxoStorageImpl implements UtxoStorage {
    private final UtxoRepository utxoRepository;
    private final TxInputRepository spentOutputRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;
    private final UtxoCache utxoCache;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        var cacheUtxo = utxoCache.get(txHash, outputIndex);
        if (cacheUtxo.isPresent())
            return cacheUtxo;
        else {
            var savedUtxo = utxoRepository.findById(new UtxoId(txHash, outputIndex))
                    .map(entity -> mapper.toAddressUtxo(entity));
            if (savedUtxo.isPresent())
                utxoCache.add(savedUtxo.get());

            return savedUtxo;
        }
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {

        var cacheResult = utxoCache.get(utxoKeys);
        if (cacheResult._2 == null)
            return cacheResult._1;

        List<UtxoKey> notFoundKeys = cacheResult._2;

        List<UtxoId> utxoIds = notFoundKeys.stream()
                .map(utxoKey -> new UtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        var savedUtxos = utxoRepository.findAllById(utxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();

        //Add remaining utxos to cache
        if (savedUtxos != null)
            savedUtxos.stream().forEach(utxo -> utxoCache.add(utxo));

        List<AddressUtxo> finalUtxos = new ArrayList<>();
        finalUtxos.addAll(cacheResult._1);
        finalUtxos.addAll(savedUtxos);

        return finalUtxos;
    }

    @Override
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        return utxoRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {
        return spentOutputRepository.deleteBySpentAtSlotGreaterThan(slot);
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        List<AddressUtxoEntity> addressUtxoEntities = addressUtxoList.stream()
                .map(addressUtxo -> mapper.toAddressUtxoEntity(addressUtxo))
                .toList();

        LocalDateTime localDateTime = LocalDateTime.now();
        dsl.batched(c -> {
            for (AddressUtxoEntity addressUtxo : addressUtxoEntities) {
                c.dsl().insertInto(ADDRESS_UTXO)
                        .set(ADDRESS_UTXO.TX_HASH, addressUtxo.getTxHash())
                        .set(ADDRESS_UTXO.OUTPUT_INDEX, addressUtxo.getOutputIndex())
                        .set(ADDRESS_UTXO.SLOT, addressUtxo.getSlot())
                        .set(ADDRESS_UTXO.BLOCK_HASH, addressUtxo.getBlockHash())
                        .set(ADDRESS_UTXO.EPOCH, addressUtxo.getEpoch())
                        .set(ADDRESS_UTXO.LOVELACE_AMOUNT, addressUtxo.getLovelaceAmount() != null ? addressUtxo.getLovelaceAmount().longValue() : 0L)
                        .set(ADDRESS_UTXO.AMOUNTS, JSON.valueOf(JsonUtil.getJson(addressUtxo.getAmounts())))
                        .set(ADDRESS_UTXO.DATA_HASH, addressUtxo.getDataHash())
                        .set(ADDRESS_UTXO.INLINE_DATUM, addressUtxo.getInlineDatum())
                        .set(ADDRESS_UTXO.OWNER_ADDR, addressUtxo.getOwnerAddr())
                        .set(ADDRESS_UTXO.OWNER_ADDR_FULL, addressUtxo.getOwnerAddrFull())
                        .set(ADDRESS_UTXO.OWNER_STAKE_ADDR, addressUtxo.getOwnerStakeAddr())
                        .set(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL, addressUtxo.getOwnerPaymentCredential())
                        .set(ADDRESS_UTXO.OWNER_STAKE_CREDENTIAL, addressUtxo.getOwnerStakeCredential())
                        .set(ADDRESS_UTXO.SCRIPT_REF, addressUtxo.getScriptRef())
                        .set(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH, addressUtxo.getReferenceScriptHash())
                        .set(ADDRESS_UTXO.IS_COLLATERAL_RETURN, addressUtxo.getIsCollateralReturn())
                        .set(ADDRESS_UTXO.BLOCK, addressUtxo.getBlockNumber())
                        .set(ADDRESS_UTXO.BLOCK_TIME, addressUtxo.getBlockTime())
                        .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
                        .onDuplicateKeyUpdate()
                        .set(ADDRESS_UTXO.SLOT, addressUtxo.getSlot())
                        .set(ADDRESS_UTXO.BLOCK_HASH, addressUtxo.getBlockHash())
                        .set(ADDRESS_UTXO.EPOCH, addressUtxo.getEpoch())
                        .set(ADDRESS_UTXO.LOVELACE_AMOUNT, addressUtxo.getLovelaceAmount() != null ? addressUtxo.getLovelaceAmount().longValue() : 0L)
                        .set(ADDRESS_UTXO.AMOUNTS, JSON.valueOf(JsonUtil.getJson(addressUtxo.getAmounts())))
                        .set(ADDRESS_UTXO.DATA_HASH, addressUtxo.getDataHash())
                        .set(ADDRESS_UTXO.INLINE_DATUM, addressUtxo.getInlineDatum())
                        .set(ADDRESS_UTXO.OWNER_ADDR, addressUtxo.getOwnerAddr())
                        .set(ADDRESS_UTXO.OWNER_ADDR_FULL, addressUtxo.getOwnerAddrFull())
                        .set(ADDRESS_UTXO.OWNER_STAKE_ADDR, addressUtxo.getOwnerStakeAddr())
                        .set(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL, addressUtxo.getOwnerPaymentCredential())
                        .set(ADDRESS_UTXO.OWNER_STAKE_CREDENTIAL, addressUtxo.getOwnerStakeCredential())
                        .set(ADDRESS_UTXO.SCRIPT_REF, addressUtxo.getScriptRef())
                        .set(ADDRESS_UTXO.REFERENCE_SCRIPT_HASH, addressUtxo.getReferenceScriptHash())
                        .set(ADDRESS_UTXO.IS_COLLATERAL_RETURN, addressUtxo.getIsCollateralReturn())
                        .set(ADDRESS_UTXO.BLOCK, addressUtxo.getBlockNumber())
                        .set(ADDRESS_UTXO.BLOCK_TIME, addressUtxo.getBlockTime())
                        .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
                        .execute();
            }
        });

        addressUtxoList.stream()
                .forEach(addressUtxo -> utxoCache.add(addressUtxo));
    }

    @Override
    public void saveSpent(List<TxInput> txInputs) {
        if (txInputs == null || txInputs.size() == 0)
            return;

        dsl.batched(c -> {
            for (TxInput spentOutput : txInputs) {
                c.dsl().insertInto(TX_INPUT)
                        .set(TX_INPUT.TX_HASH, spentOutput.getTxHash())
                        .set(TX_INPUT.OUTPUT_INDEX, spentOutput.getOutputIndex())
                        .set(TX_INPUT.SPENT_AT_SLOT, spentOutput.getSpentAtSlot())
                        .set(TX_INPUT.SPENT_AT_BLOCK, spentOutput.getSpentAtBlock())
                        .set(TX_INPUT.SPENT_AT_BLOCK_HASH, spentOutput.getSpentAtBlockHash())
                        .set(TX_INPUT.SPENT_BLOCK_TIME, spentOutput.getSpentBlockTime())
                        .set(TX_INPUT.SPENT_EPOCH, spentOutput.getSpentEpoch())
                        .set(TX_INPUT.SPENT_TX_HASH, spentOutput.getSpentTxHash())
                        .onDuplicateKeyIgnore()
                        .execute();
            }
        });
    }

    @EventListener
    public void handleCommit(CommitEvent commitEvent) {
        utxoCache.clear();
    }

/**   Remove this method after testing
    @EventListener
    @Transactional
    public void handleCommit(CommitEvent event) {
//        try {
//            LocalDateTime localDateTime = LocalDateTime.now();
//            dsl.batched(c -> {
//                for (AddressUtxo addressUtxo : spentUtxoCache) {
//                    c.dsl().insertInto(ADDRESS_UTXO)
//                            .set(ADDRESS_UTXO.TX_HASH, addressUtxo.getTxHash())
//                            .set(ADDRESS_UTXO.OUTPUT_INDEX, addressUtxo.getOutputIndex())
//                            .set(ADDRESS_UTXO.SPENT, true)
//                            .set(ADDRESS_UTXO.SPENT_AT_SLOT, addressUtxo.getSpentAtSlot())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK, addressUtxo.getSpentAtBlock())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK_HASH, addressUtxo.getSpentAtBlockHash())
//                            .set(ADDRESS_UTXO.SPENT_BLOCK_TIME, addressUtxo.getSpentBlockTime())
//                            .set(ADDRESS_UTXO.SPENT_EPOCH, addressUtxo.getSpentEpoch())
//                            .set(ADDRESS_UTXO.SPENT_TX_HASH, addressUtxo.getSpentTxHash())
//                            .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
//                            .onDuplicateKeyUpdate()
//                            .set(ADDRESS_UTXO.SPENT, true)
//                            .set(ADDRESS_UTXO.SPENT_AT_SLOT, addressUtxo.getSpentAtSlot())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK, addressUtxo.getSpentAtBlock())
//                            .set(ADDRESS_UTXO.SPENT_AT_BLOCK_HASH, addressUtxo.getSpentAtBlockHash())
//                            .set(ADDRESS_UTXO.SPENT_BLOCK_TIME, addressUtxo.getSpentBlockTime())
//                            .set(ADDRESS_UTXO.SPENT_EPOCH, addressUtxo.getSpentEpoch())
//                            .set(ADDRESS_UTXO.SPENT_TX_HASH, addressUtxo.getSpentTxHash())
//                            .set(ADDRESS_UTXO.UPDATE_DATETIME, localDateTime)
//                            .execute();
//                }
//            });
//
//        } finally {
//            spentUtxoCache.clear();
//        }
    }
 **/


}
