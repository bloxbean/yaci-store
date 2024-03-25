package com.bloxbean.cardano.yaci.store.account.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "address_balance")
@IdClass(AddressBalanceId.class)
@DynamicUpdate
public class AddressBalanceEntity extends BlockAwareEntity {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "unit")
    private String unit;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "quantity")
    private BigInteger quantity;

    //Only set if address doesn't fit in ownerAddr field. Required for few Byron Era addr
    @Column(name = "addr_full")
    private String addrFull;

    @Column(name = "epoch")
    private Integer epoch;

}
