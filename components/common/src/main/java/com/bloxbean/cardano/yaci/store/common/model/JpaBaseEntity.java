package com.bloxbean.cardano.yaci.store.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

//@TypeDefs({
//        @TypeDef(name = "string-array", typeClass = StringArrayType.class),
//        @TypeDef(name = "int-array", typeClass = IntArrayType.class),
//        @TypeDef(name = "json", typeClass = JsonType.class)
//})
@MappedSuperclass
public class JpaBaseEntity {

    @CreationTimestamp
    @Column(name = "create_datetime")
    private LocalDateTime createDateTime;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;

}
