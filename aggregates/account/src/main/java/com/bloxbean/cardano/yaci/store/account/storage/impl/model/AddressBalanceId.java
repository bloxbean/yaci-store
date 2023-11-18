package com.bloxbean.cardano.yaci.store.account.storage.impl.model;

import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class AddressBalanceId implements Serializable {
    @Column(name = "address")
    private String address;
    @Column(name = "unit")
    private String unit;
    @Column(name = "slot")
    private Long slot;
}
