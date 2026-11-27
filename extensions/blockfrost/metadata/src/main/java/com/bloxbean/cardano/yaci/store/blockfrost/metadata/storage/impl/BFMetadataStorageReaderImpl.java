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
    private static final Field<Long> SLOT = DSL.field(DSL.name("slot"), Long.class);
    private static final Field<String> TX_HASH = DSL.field(DSL.name("tx_hash"), String.class);
    private static final Field<String> BODY = DSL.field(DSL.name("body"), String.class);
    private static final Field<String> CBOR = DSL.field(DSL.name("cbor"), String.class);

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

        log.debug("[BFMetadataStorageReader] findLabelsWithCount SQL: {}", query.getSQL(ParamType.INLINED));

        return query.fetch(record -> BFMetadataLabelDto.builder()
                .label(record.get(LABEL))
                .cip10(null)
                .count(String.valueOf(record.get(cntField)))
                .build());
    }

    @Override
    public List<TxMetadataLabel> findByLabel(String label, int page, int count, Order order) {
        int offset = Math.max(page, 0) * count;
        // The range form (instead of label = ?) is deliberate. With equality PostgreSQL treats label as a
        // constant and may walk idx_txn_metadata_slot, filtering on label, which reads most of the table for
        // labels whose rows sit late in slot order. A range keeps label in the sort key, so only
        // idx_txn_metadata_label_slot (blockfrost-index.yml) provides the order. Requires that index.
        List<SortField<?>> orderBy = order == Order.desc
                ? List.of(LABEL.desc(), SLOT.desc(), TX_HASH.desc())
                : List.of(LABEL.asc(), SLOT.asc(), TX_HASH.asc());

        var query = dsl.select(TX_HASH, SLOT, LABEL, BODY, CBOR)
                .from(TX_METADATA)
                .where(LABEL.ge(label).and(LABEL.le(label)))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset);

        return query.fetch(record -> TxMetadataLabel.builder()
                .txHash(record.get(TX_HASH))
                .slot(record.get(SLOT))
                .label(record.get(LABEL))
                .body(record.get(BODY))
                .cbor(record.get(CBOR))
                .build());
    }
}
