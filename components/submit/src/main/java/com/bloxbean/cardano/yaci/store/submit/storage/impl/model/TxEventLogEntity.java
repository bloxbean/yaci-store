package com.bloxbean.cardano.yaci.store.submit.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BaseEntity;
import com.bloxbean.cardano.yaci.store.submit.domain.TxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

/**
 * Entity for transaction event log (audit trail).
 * Records every status transition for debugging and analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "tx_event_log")
@EqualsAndHashCode(callSuper = false)
public class TxEventLogEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "tx_hash", nullable = false, length = 64)
    private String txHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private TxStatus previousStatus;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 20)
    private TxStatus currentStatus;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "event_timestamp", nullable = false)
    private Timestamp eventTimestamp;
    
    @Column(name = "confirmed_slot")
    private Long confirmedSlot;
    
    @Column(name = "confirmed_block_number")
    private Long confirmedBlockNumber;
}

