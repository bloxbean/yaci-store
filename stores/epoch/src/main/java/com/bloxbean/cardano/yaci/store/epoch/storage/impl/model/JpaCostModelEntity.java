package com.bloxbean.cardano.yaci.store.epoch.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.JpaBlockAwareEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "cost_model")
public class JpaCostModelEntity extends JpaBlockAwareEntity {
    @Id
    @Column(name = "hash")
    private String hash;

    @Type(JsonType.class)
    @Column(name = "costs", columnDefinition = "json")
    private Map<String, long[]> costs;

    @Column(name = "slot")
    private Long slot;
}
