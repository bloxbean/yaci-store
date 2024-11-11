package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

//@Component
@RequiredArgsConstructor
@Slf4j
public class DRepStakeService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void takeStakeSnapshot(EventMetadata eventMetadata, int epoch) {
        log.info("Taking snapshot for epoch {}", epoch);

        String query = """ 
                     WITH RankedDelegations AS ( 
                     SELECT  
                         address,  
                         drep_id, 
                         drep_hash, 
                         drep_type, 
                         epoch, 
                         slot, 
                         tx_index, 
                         cert_index, 
                         ROW_NUMBER() OVER ( 
                             PARTITION BY address 
                             ORDER BY slot DESC, tx_index DESC, cert_index DESC 
                         ) AS rn 
                     FROM 
                         delegation_vote 
                     WHERE 
                         epoch <= :epoch 
                 ), 
                 MaxSlotBalances AS ( 
                     SELECT 
                         address, 
                         MAX(slot) AS max_slot 
                     FROM 
                         stake_address_balance 
                     WHERE 
                         epoch <= :epoch 
                     GROUP BY 
                         address 
                 ), 
                 DRepStatus AS ( 
                     SELECT 
                         drep_id, 
                         drep_hash, 
                         status, 
                         slot, 
                         registration_slot, 
                         ROW_NUMBER() OVER ( 
                             PARTITION BY drep_id 
                             ORDER BY slot DESC, tx_index DESC, cert_index DESC 
                         ) AS rn 
                     FROM 
                         drep 
                     WHERE epoch <= :epoch 
                 ) 
                
                 insert into drep_dist  
                 SELECT 
                     ds.drep_id, 
                     ds.drep_hash, 
                     SUM(COALESCE(s.quantity, 0)), 
                     :epoch, 
                     :slot,  
                     now() 
                 FROM 
                     DRepStatus ds 
                 LEFT JOIN RankedDelegations d ON 
                     d.drep_id = ds.drep_id 
                     AND d.rn = 1 
                 LEFT JOIN MaxSlotBalances msb ON 
                     d.address = msb.address 
                     AND NOT EXISTS ( 
                         SELECT 1 
                         FROM stake_registration sd 
                         WHERE sd.address = d.address 
                             AND sd.type = 'STAKE_DEREGISTRATION' 
                             AND sd.epoch <= :epoch 
                             AND ( 
                                 sd.slot > d.slot 
                                 OR (sd.slot = d.slot AND sd.tx_index > d.tx_index) 
                                 OR (sd.slot = d.slot AND sd.tx_index = d.tx_index AND sd.cert_index > d.cert_index) 
                             ) 
                     ) 
                 LEFT JOIN stake_address_balance s ON 
                     msb.address = s.address 
                     AND msb.max_slot = s.slot 
                 WHERE 
                     ds.rn = 1 and ds.status = 'ACTIVE' 
                 GROUP BY 
                     ds.drep_id, 
                     ds.drep_hash 
                
                 UNION ALL 
                 SELECT
                        drep_type_info.drep_type AS drep_id,
                        NULL AS drep_hash,
                        COALESCE(SUM(s.quantity), 0) AS amount,
                        :epoch,
                        :slot, 
                        NOW()
                FROM
                    (VALUES ('ABSTAIN'), ('NO_CONFIDENCE')) AS drep_type_info(drep_type)
                LEFT JOIN RankedDelegations d ON
                    d.drep_type = drep_type_info.drep_type AND d.rn = 1
                LEFT JOIN MaxSlotBalances msb ON
                    d.address = msb.address
                    AND NOT EXISTS (
                        SELECT 1
                        FROM stake_registration sd
                        WHERE sd.address = d.address
                            AND sd.type = 'STAKE_DEREGISTRATION'
                            AND sd.epoch <= :epoch
                            AND (
                                sd.slot > d.slot
                                OR (sd.slot = d.slot AND sd.tx_index > d.tx_index)
                                OR (sd.slot = d.slot AND sd.tx_index = d.tx_index AND sd.cert_index > d.cert_index)
                            )
                    )
                LEFT JOIN stake_address_balance s ON
                    msb.address = s.address
                    AND msb.max_slot = s.slot
                GROUP BY
                    drep_type_info.drep_type
                """;

        var params = new MapSqlParameterSource();
        params.addValue("epoch", epoch);
        params.addValue("slot", eventMetadata.getSlot());

        jdbcTemplate.update(query, params);

        log.info("DRep Stake Snapshot for epoch : {} is taken", epoch);

        log.info(">>>>>>>>>>>>>>>>>>>> DRep Stake Snapshot taken for epoch : {} <<<<<<<<<<<<<<<<<<<<", epoch);
    }

}
