package com.bloxbean.cardano.yaci.store.blockfrost.metadata.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.metadata.dto.BFMetadataLabelDto;
import com.bloxbean.cardano.yaci.store.blockfrost.metadata.storage.BFMetadataStorageReader;
import com.bloxbean.cardano.yaci.store.common.model.Order;
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
}
