package com.bloxbean.cardano.yaci.store.epoch.storage.impl.jpa.model;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "epoch_param")
@Slf4j
public class EpochParamEntity extends BlockAwareEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Type(JsonType.class)
    @Column(name = "params", columnDefinition = "json")
    private ProtocolParams params;

    @Column(name = "cost_model_hash")
    private String costModelHash;

    @Column(name = "slot")
    private Long slot;

    @PrePersist
    public void preSave() {
        if (this.getParams() == null)
            return;

        //reset these fields
        if (this.getParams().getCostModels() != null)
            this.getParams().setCostModels(null);
    }
}
