package com.bloxbean.cardano.yaci.store.utxo.storage.impl;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.UtxoStorageReader;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;
import static org.jooq.impl.DSL.field;

@RequiredArgsConstructor
public class UtxoStorageReaderImpl implements UtxoStorageReader {

    private final UtxoRepository utxoRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.findById(new UtxoId(txHash, outputIndex))
                .map(mapper::toAddressUtxo);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddress(@NonNull String address, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        return utxoRepository.findUnspentByOwnerAddr(address, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxosByAsset(String unit, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\"")
                        .or(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\":\""+unit+"\""))
                )
                .and(TX_INPUT.TX_HASH.isNull())
                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByAddressAndAsset(String address, String unit, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_ADDR.eq(address))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\"")
                        .or(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\":\""+unit+"\""))
                )
                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: Ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredential(@NonNull String paymentCredential, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        return utxoRepository.findUnspentByOwnerPaymentCredential(paymentCredential, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxoByPaymentCredentialAndAsset(String paymentCredential, String unit, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\"")
                        .or(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\":\""+unit+"\""))
                )
                //.orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc()) //TODO ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddress(@NonNull String stakeAddress, int page, int count, Order order) {
        Pageable pageable = getPageable(page, count, order);

        return utxoRepository.findUnspentByOwnerStakeAddr(stakeAddress, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();
    }

    @Override
    public List<AddressUtxo> findUtxoByStakeAddressAndAsset(@NonNull String stakeAddress, String unit, int page, int count, Order order) {
        stakeAddress = stakeAddress.trim();

        Pageable pageable = getPageable(page, count, order);

        var query = dsl
                .select(ADDRESS_UTXO.fields())
                .from(ADDRESS_UTXO)
                .leftJoin(TX_INPUT)
                .using(field(ADDRESS_UTXO.TX_HASH), field(ADDRESS_UTXO.OUTPUT_INDEX))
                .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(stakeAddress))
                .and(TX_INPUT.TX_HASH.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\": \""+unit+"\"")
                        .or(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains("\"unit\":\""+unit+"\""))
                )
                // .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())  //TODO: ordering
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        return query.fetch().into(AddressUtxo.class);
    }

    @Override
    public List<AddressUtxo> findAllByIds(List<UtxoKey> utxoKeys) {
        List<UtxoId> utxoIds = utxoKeys.stream()
                .map(utxoKey -> new UtxoId(utxoKey.getTxHash(), utxoKey.getOutputIndex()))
                .toList();

        return utxoRepository.findAllById(utxoIds)
                .stream().map(mapper::toAddressUtxo)
                .toList();
    }

    private static PageRequest getPageable(int page, int count, Order order) {
        return PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot", "txHash", "outputIndex");
    }
}
