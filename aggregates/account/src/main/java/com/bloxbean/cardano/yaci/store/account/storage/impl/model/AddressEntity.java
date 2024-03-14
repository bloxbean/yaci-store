package com.bloxbean.cardano.yaci.store.account.storage.impl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "address")
public class AddressEntity {
    @Id
    @Column(name = "address")
    private String address;

    //Only set if address doesn't fit in ownerAddr field. Required for few Byron Era addr
    @Column(name = "addr_full")
    private String addrFull;

    @Column(name = "payment_credential")
    private String paymentCredential;

    @Column(name = "stake_address")
    private String stakeAddress;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;
}
