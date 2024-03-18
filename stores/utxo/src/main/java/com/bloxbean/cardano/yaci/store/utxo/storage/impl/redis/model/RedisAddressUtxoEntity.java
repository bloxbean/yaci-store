package com.bloxbean.cardano.yaci.store.utxo.storage.impl.redis.model;

import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.model.RedisBlockAwareEntity;
import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigInteger;
import java.util.List;

@Data
@Document
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisAddressUtxoEntity extends RedisBlockAwareEntity {

    @Id
    private String id;

    @Indexed
    private String txHash;

    @Indexed
    private Integer outputIndex;
    private Long slot;
    private String blockHash;
    private Integer epoch;
    private String ownerAddr;
    private String ownerAddrFull;
    private String ownerStakeAddr;
    private String ownerPaymentCredential;
    private String ownerStakeCredential;
    private BigInteger lovelaceAmount;
    @Type(JsonType.class)
    private List<Amt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String scriptRef;
    private String referenceScriptHash;
    private Boolean isCollateralReturn;
}
