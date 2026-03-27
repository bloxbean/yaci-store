package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "metadata")
@Getter
@Setter
public class TokenMetadata {

    @Id
    private String subject;

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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenMetadata that = (TokenMetadata) o;
        return Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject);
    }
}
