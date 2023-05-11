package com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.storage.api.UtxoStorage;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.mapper.UtxoMapper;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.AddressUtxoEntity;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.jpa.repository.UtxoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static org.jooq.impl.DSL.field;

@RequiredArgsConstructor
@Slf4j
public class UtxoStorageImpl implements UtxoStorage {
    private final UtxoRepository utxoRepository;
    private final DSLContext dsl;
    private final UtxoMapper mapper = UtxoMapper.INSTANCE;

    @Override
    public Optional<AddressUtxo> findById(String txHash, int outputIndex) {
        return utxoRepository.findById(new UtxoId(txHash, outputIndex))
                .map(entity -> mapper.toAddressUtxo(entity));
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByAddress(String address, int page, int count, Order order) {
        return findUtxoByAddressAndSpent(address, null, page, count, order);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByAddressAndSpent(String address, Boolean spent, int page, int count, Order order) {
        String paymentCredential = getPaymentCredential(address);

        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        List<AddressUtxo> addressUtxoList = utxoRepository.findByOwnerPaymentCredentialAndSpent(paymentCredential, spent, pageable)
                .stream()
                .flatMap(addressUtxoEntities -> addressUtxoEntities.stream().map(mapper::toAddressUtxo))
                .toList();

        return Optional.of(addressUtxoList);
    }

    @Override
    public Optional<List<AddressUtxo>> findUtxoByAddressAndAsset(String address, String unit, int page, int count, Order order) {
        String paymentCredential = getPaymentCredential(address);

        Pageable pageable = PageRequest.of(page, count)
                .withSort(order.equals(Order.desc) ? Sort.Direction.DESC : Sort.Direction.ASC, "slot");

        var query = dsl
                .select()
                .from(ADDRESS_UTXO)
                .where(ADDRESS_UTXO.OWNER_PAYMENT_CREDENTIAL.eq(paymentCredential))
                        .and(ADDRESS_UTXO.SPENT.isNull())
                .and(field(ADDRESS_UTXO.AMOUNTS).cast(String.class).contains(unit))
                .orderBy(order.equals(Order.desc) ? ADDRESS_UTXO.SLOT.desc() : ADDRESS_UTXO.SLOT.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        List<AddressUtxo> addressUtxoList = query.fetch().into(AddressUtxo.class);
        return Optional.of(addressUtxoList);
    }

    private static String getPaymentCredential(String address) {
        String paymentCredential = null;
        if (address.startsWith("addr_vkh")) {
            paymentCredential = HexUtil.encodeHexString(Bech32.decode(address).data);

        } else if (address.startsWith("addr")) {
            Address _address = new Address(address);
            paymentCredential = _address.getPaymentKeyHash()
                    .map(bytes -> HexUtil.encodeHexString(bytes))
                    .orElse(null);
        }

        return paymentCredential;
    }


    @Override
    public List<AddressUtxo> findBySlot(Long slot) {
        return utxoRepository.findBySlot(slot)
                .stream().map(mapper::toAddressUtxo)
                .toList();
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

    @Override
    public int deleteBySlotGreaterThan(Long slot) {
        return utxoRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public Optional<AddressUtxo> save(AddressUtxo addressUtxo) {
        AddressUtxoEntity addressUtxoEntity = utxoRepository.save(mapper.toAddressUtxoEntity(addressUtxo));
        return Optional.of(mapper.toAddressUtxo(addressUtxoEntity));
    }

    @Override
    public Optional<List<AddressUtxo>> saveAll(List<AddressUtxo> addressUtxoList) {
        List<AddressUtxoEntity> addressUtxoEntities = addressUtxoList.stream()
                .map(addressUtxo -> mapper.toAddressUtxoEntity(addressUtxo))
                .toList();
        addressUtxoEntities = utxoRepository.saveAll(addressUtxoEntities);
        return Optional.of(addressUtxoEntities.stream()
                .map(entity -> mapper.toAddressUtxo(entity))
                .toList());
    }
}
