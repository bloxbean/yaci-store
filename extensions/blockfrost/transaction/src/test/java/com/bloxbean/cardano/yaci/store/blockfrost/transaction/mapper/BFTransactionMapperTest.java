package com.bloxbean.cardano.yaci.store.blockfrost.transaction.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.transaction.dto.BFTransactionDto;
import com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model.TxRaw;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BFTransactionMapperTest {

    private final BFTransactionMapper mapper = Mappers.getMapper(BFTransactionMapper.class);

    @Test
    void givenSize_shouldMapToTransactionDto() {
        BFTransactionDto dto = mapper.toTransactionDto(txRaw(814), List.of(), "0", 0);

        assertThat(dto.getSize()).isEqualTo(814);
    }

    @Test
    void givenNoSize_shouldMapNull() {
        BFTransactionDto dto = mapper.toTransactionDto(txRaw(null), List.of(), "0", 0);

        assertThat(dto.getSize()).isNull();
    }

    private TxRaw txRaw(Integer size) {
        return TxRaw.builder()
                .txHash("df39d52ed5e803903f94646ca9c7b8afc94436277c29f8c1b050f1749d863ce3")
                .size(size)
                .build();
    }
}
