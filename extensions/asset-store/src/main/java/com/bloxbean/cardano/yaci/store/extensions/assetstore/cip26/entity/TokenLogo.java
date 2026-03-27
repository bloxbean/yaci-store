package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Entity
@Table(name = "token_logo")
@Getter
@Setter
public class TokenLogo {

    @Id
    private String subject;

    private String logo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenLogo tokenLogo = (TokenLogo) o;
        return Objects.equals(subject, tokenLogo.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject);
    }
}
