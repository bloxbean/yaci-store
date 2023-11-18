package com.bloxbean.cardano.yaci.store.account.storage.impl.model;

import com.bloxbean.cardano.yaci.store.account.util.ConfigStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "account_config")
public class AccountConfigEntity {
    @Id
    @Column(name = "config_id")
    private String configId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ConfigStatus status;

    @Column(name = "block")
    private Long block;
}
