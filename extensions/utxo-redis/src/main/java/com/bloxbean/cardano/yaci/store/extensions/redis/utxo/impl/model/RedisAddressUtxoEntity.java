package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model;

import com.redis.om.spring.annotations.Document;
import com.redis.om.spring.annotations.Indexed;
import com.redis.om.spring.annotations.Searchable;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.Id;

import java.math.BigInteger;
import java.util.List;


@Data
@Document("address_utxo")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class RedisAddressUtxoEntity extends RedisBlockAwareEntity {

    @Id
    private String id;

    @Indexed
    @Searchable
    private String txHash;

    @Indexed
    private Integer outputIndex;
    private Long slot;
    private String blockHash;
    private Integer epoch;

    @Indexed
    private String ownerAddr;
    private String ownerAddrFull;
    private String ownerStakeAddr;

    @Indexed
    private String ownerPaymentCredential;
    private String ownerStakeCredential;
    private BigInteger lovelaceAmount;

    @Indexed
    @Type(JsonType.class)
    private List<RedisAmt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String scriptRef;
    private String referenceScriptHash;
    private Boolean isCollateralReturn;
}
