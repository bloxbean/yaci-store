package com.bloxbean.cardano.yaci.store.adapot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.STAKE_REGISTRATION;
import static org.jooq.impl.DSL.notExists;
import static org.jooq.impl.DSL.selectOne;

@Component
@RequiredArgsConstructor
@Slf4j
public class StakeRegistrationService {
    private final DSLContext dsl;

    public List<String> getDeregisteredAccountsInEpoch(int epoch, long absoluteSlot) {

        /**
         SELECT  distinct s.address
         FROM stake_registration s
         WHERE s.type = 'STAKE_DEREGISTRATION'
         AND s.epoch <= ?
         AND s.slot < ?
         AND NOT EXISTS (
         SELECT 1
         FROM stake_registration s2
         WHERE s2.address = s.address
         AND s2.epoch <= ?
         AND (
             (s2.slot > s.slot) OR
             (s2.slot = s.slot AND s2.cert_index > s.cert_index) OR
             (s2.slot = s.slot AND s2.cert_index = s.cert_index AND s2.tx_index > s.tx_index)
         )
         AND s2.slot < ?
         AND s2.type = 'STAKE_REGISTRATION'
         )
         ORDER BY s.address;
         */

        // Define the table and alias it
        var s = STAKE_REGISTRATION.as("s");
        var s2 = STAKE_REGISTRATION.as("s2");

        // Subquery to check if there is a later registration for the same address in the same epoch
        var noLaterRegistration = notExists(
                selectOne()
                        .from(s2)
                        .where(s2.ADDRESS.eq(s.ADDRESS)
                                .and(s2.EPOCH.le(epoch))
                                .and(s2.SLOT.gt(s.SLOT))
                                .and(
                                        s2.SLOT.gt(s.SLOT)
                                        .or(s2.SLOT.eq(s.SLOT).and(s2.CERT_INDEX.gt(s.CERT_INDEX)))
                                        .or(s2.SLOT.eq(s.SLOT).and(s2.CERT_INDEX.eq(s.CERT_INDEX)).and(s2.TX_INDEX.gt(s.TX_INDEX)))
                                )
                                .and(s2.SLOT.lt(absoluteSlot))
                                .and(s2.TYPE.eq("STAKE_REGISTRATION")))
        );

        // Query to select distinct addresses where there is a deregistration without a later registration
        return dsl.selectDistinct(s.ADDRESS)
                .from(s)
                .where(s.TYPE.eq("STAKE_DEREGISTRATION"))
                .and(s.EPOCH.le(epoch))
                .and(s.SLOT.lt(absoluteSlot))
                .and(noLaterRegistration)
                .orderBy(s.ADDRESS)
                .fetchInto(String.class);
    }

    public List<String> getRegisteredAccountsUntilEpoch(int epoch, Set<String> stakeAddresses, long stabilityWindow) {
        /**
         SELECT   distinct s.address
         FROM stake_registration s
         WHERE s.type = 'STAKE_REGISTRATION'
         AND s.epoch <= 211
         AND s.slot < 4496982
         AND s.address in ('stake1uy5e66lc4u5qzhqed04v6kxq933canz2ugs7je7u6pptqgs56sfaa', 'stake1dxte0wr3zk4x37xknqvp4umkq5pxf69wrg64vz9w32j7gcshuekuf')
         */


//        String result = stakeAddresses.stream()
//                .map(address -> "\"" + address + "\"")
//                .collect(Collectors.joining(","));
//
//        System.out.println(result);

        // JOOQ query to select registered accounts until the last epoch
        return dsl.selectDistinct(STAKE_REGISTRATION.ADDRESS)
                .from(STAKE_REGISTRATION)
                .where(STAKE_REGISTRATION.TYPE.eq("STAKE_REGISTRATION"))
                .and(STAKE_REGISTRATION.EPOCH.le(epoch))
                .and(STAKE_REGISTRATION.SLOT.lt(stabilityWindow))
                .and(STAKE_REGISTRATION.ADDRESS.in(stakeAddresses))
                .fetchInto(String.class);
    }
}
