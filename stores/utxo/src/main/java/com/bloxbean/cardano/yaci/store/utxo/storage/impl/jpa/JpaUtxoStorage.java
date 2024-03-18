package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper.JpaUtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaAddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.JpaUtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaTxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.JpaUtxoRepository;
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
public class JpaUtxoStorage implements UtxoStorage {

    private final JpaUtxoRepository jpaUtxoRepository;
    private final JpaTxInputRepository spentOutputRepository;
    private final DSLContext dsl;
    private final JpaUtxoMapper mapper = JpaUtxoMapper.INSTANCE;
    private final UtxoCache utxoCache;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        var cacheUtxo = utxoCache.get(txHash, outputIndex);
        if (cacheUtxo.isPresent())
            return cacheUtxo;
        else {
            var savedUtxo = jpaUtxoRepository.findById(new JpaUtxoId(txHash, outputIndex))
                    .map(mapper::toAddressUtxo);
            savedUtxo.ifPresent(utxoCache::add);

            return savedUtxo;
        }
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {

        var cacheResult = utxoCache.get(utxoKeys);
        if (cacheResult._2 == null)
            return cacheResult._1;

        List<UtxoKey> notFoundKeys = cacheResult._2;

        List<JpaUtxoId> jpaUtxoIds = notFoundKeys.stream()
                .map(utxoKey -> new JpaUtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        var savedUtxos = jpaUtxoRepository.findAllById(jpaUtxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();

        //Add remaining utxos to cache
        if (savedUtxos != null)
            savedUtxos.forEach(utxoCache::add);

        List<AddressUtxo> finalUtxos = new ArrayList<>();
        finalUtxos.addAll(cacheResult._1);
        finalUtxos.addAll(savedUtxos);

        return finalUtxos;
    }

    @Override
    public int deleteUnspentBySlotGreaterThan(Long slot) {
        return jpaUtxoRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteSpentBySlotGreaterThan(Long slot) {
        return spentOutputRepository.deleteBySpentAtSlotGreaterThan(slot);
    }

    @Override
    public int deleteBySpentAndBlockLessThan(Long block) {
        return jpaUtxoRepository.deleteBySpentAndBlockLessThan(block);
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        List<JpaAddressUtxoEntity> addressUtxoEntities = addressUtxoList.stream()
                .map(mapper::toAddressUtxoEntity)
                .toList();

        LocalDateTime localDateTime = LocalDateTime.now();
        dsl.batched(c -> {
            for (JpaAddressUtxoEntity addressUtxo : addressUtxoEntities) {
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

        addressUtxoList.forEach(utxoCache::add);
    }

    @Override
    public void saveSpent(List<TxInput> txInputs) {
        if (txInputs == null || txInputs.isEmpty())
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
