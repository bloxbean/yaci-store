package com.bloxbean.cardano.yaci.store.blockfrost.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.storage.BFMetadataStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.metadata.domain.TxMetadataLabel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFMetadataStorageReaderImpl implements BFMetadataStorageReader {

    private static final Table<?> TX_METADATA = DSL.table(DSL.name("transaction_metadata"));
    private static final Field<String> LABEL = DSL.field(DSL.name("label"), String.class);
    private static final Field<String> METADATA_TX_HASH = DSL.field(DSL.name("transaction_metadata", "tx_hash"), String.class);
    private static final Field<Long> METADATA_SLOT = DSL.field(DSL.name("transaction_metadata", "slot"), Long.class);
    private static final Field<String> BODY = DSL.field(DSL.name("body"), String.class);
    private static final Field<String> CBOR = DSL.field(DSL.name("cbor"), String.class);
    private static final Table<?> TRANSACTION = DSL.table(DSL.name("transaction"));
    private static final Field<String> TRANSACTION_TX_HASH = DSL.field(DSL.name("transaction", "tx_hash"), String.class);
    private static final Field<Integer> TX_INDEX = DSL.field(DSL.name("transaction", "tx_index"), Integer.class);

    private final DSLContext dsl;

    @Override
    public List<BFMetadataLabelDto> findLabelsWithCount(int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        Field<BigDecimal> labelAsNumeric = LABEL.cast(BigDecimal.class);
        SortField<?> orderBy = order == Order.desc ? labelAsNumeric.desc() : labelAsNumeric.asc();

        Field<Long> cntField = DSL.count().cast(Long.class).as("cnt");

        var query = dsl.select(LABEL, cntField)
                .from(TX_METADATA)
                .groupBy(LABEL)
                .orderBy(orderBy)
                .limit(count)
                .offset(offset);

        return query.fetch(record -> BFMetadataLabelDto.builder()
                .label(record.get(LABEL))
                .cip10(null)
                .count(String.valueOf(record.get(cntField)))
                .build());
    }

    @Override
    public List<TxMetadataLabel> findMetadataByLabel(String label, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        SortField<Long> slotOrder = order == Order.desc ? METADATA_SLOT.desc() : METADATA_SLOT.asc();
        SortField<Integer> txIndexOrder = order == Order.desc ? TX_INDEX.desc().nullsLast() : TX_INDEX.asc().nullsLast();

        var query = dsl.select(METADATA_TX_HASH, METADATA_SLOT, LABEL, BODY, CBOR)
                .from(TX_METADATA)
                .leftJoin(TRANSACTION)
                .on(METADATA_TX_HASH.eq(TRANSACTION_TX_HASH))
                .where(LABEL.eq(label))
                .orderBy(slotOrder, txIndexOrder)
                .limit(count)
                .offset(offset);

        return query.fetch(record -> TxMetadataLabel.builder()
                .txHash(record.get(METADATA_TX_HASH))
                .slot(record.get(METADATA_SLOT))
                .label(record.get(LABEL))
                .body(record.get(BODY))
                .cbor(record.get(CBOR))
                .build());
    }
}
