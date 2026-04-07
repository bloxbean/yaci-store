package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ft_offchain_metadata")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TokenMetadata {

    @Id
    @EqualsAndHashCode.Include
    private String subject;

    @Column(length = 56)
    private String policy;

    private String name;

    private String ticker;

    private String url;

    private String description;

    private Long decimals;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updated;

    private String updatedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    private Mapping properties;

    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime lastSyncedAt;
}
