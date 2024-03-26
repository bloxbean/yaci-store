package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import org.springframework.data.annotation.Id;
import lombok.*;

@Data
@Builder
@Document("tx_input")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisTxInputEntity {

    @Id
    private String id;

    @Indexed
    private String txHash;

    @Indexed
    private Integer outputIndex;

    @Indexed
    private Long spentAtSlot;

    private Long spentAtBlock;

    private String spentAtBlockHash;

    private Long spentBlockTime;

    private Integer spentEpoch;

    private String spentTxHash;


}
