package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.client.transaction.spec.governance.DRepType;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepStatus;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.DRepStorageReader;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.DREP;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_DIST;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_EXPIRY;

@RequiredArgsConstructor
public class DRepStorageReaderImpl implements DRepStorageReader {
    private final DSLContext dsl;

    @Override
    public List<DRepDetailsDto> getDReps(int epoch, int page, int count, Order order) {
        var d = DREP;
        var drepDist = DREP_DIST;
        var drepExpiry = DREP_EXPIRY;

        Integer maxEpochInGovDRepCalc = dsl
                .select(DSL.max(drepExpiry.EPOCH))
                .from(drepExpiry)
                .where(drepExpiry.EPOCH.le(epoch))
                .fetchOneInto(Integer.class);

        if (maxEpochInGovDRepCalc == null) {
            var drepStatus = dsl.select(
                            d.DREP_ID,
                            d.DREP_HASH,
                            d.DEPOSIT,
                            d.STATUS,
                            d.REGISTRATION_SLOT,
                            d.SLOT,
                            d.TX_INDEX,
                            d.CERT_INDEX,
                            DSL.rowNumber().over()
                                    .partitionBy(d.DREP_ID)
                                    .orderBy(d.SLOT.desc(), d.TX_INDEX.desc(), d.CERT_INDEX.desc())
                                    .as("rn")
                    )
                    .from(d)
                    .where(d.EPOCH.le(epoch))
                    .asTable("ds");

            var latestStatus = dsl.selectFrom(drepStatus)
                    .where(DSL.field("rn", Integer.class).eq(1))
                    .orderBy(order == Order.desc
                            ? DSL.field("registration_slot", Long.class).desc()
                            : DSL.field("registration_slot", Long.class).asc())
                    .limit(count)
                    .offset(page * count)
                    .fetch();

            return latestStatus.stream()
                    .map(r -> {
                        String drepId = r.get("drep_id", String.class);
                        String drepHash = r.get("drep_hash", String.class);
                        BigInteger deposit = r.get("deposit", BigInteger.class);
                        String drepStatusStr = r.get("status", String.class);
                        Long registrationSlot = r.get("registration_slot", Long.class);

                        DRepStatus status;
                        if (com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus.RETIRED.name().equalsIgnoreCase(drepStatusStr)) {
                            status = DRepStatus.INACTIVE;
                        } else {
                            status = DRepStatus.ACTIVE;
                        }

                        return new DRepDetailsDto(
                                drepId,
                                drepHash,
                                null, // todo: add cred type to table 'drep'
                                deposit,
                                status,
                                null,
                                registrationSlot
                        );
                    })
                    .toList();
        }

        var drepStatus = dsl.select(
                        d.DREP_ID,
                        d.DREP_HASH,
                        d.DEPOSIT,
                        d.STATUS,
                        d.REGISTRATION_SLOT,
                        d.SLOT,
                        d.TX_INDEX,
                        d.CERT_INDEX,
                        DSL.rowNumber().over()
                                .partitionBy(d.DREP_ID)
                                .orderBy(d.SLOT.desc(), d.TX_INDEX.desc(), d.CERT_INDEX.desc())
                                .as("rn")
                )
                .from(d)
                .where(d.EPOCH.le(epoch))
                .asTable("ds");

        var latestStatus = dsl.selectFrom(drepStatus)
                .where(DSL.field("rn", Integer.class).eq(1))
                .asTable("latest");

        var result = dsl.select(
                        drepDist.DREP_ID,
                        drepDist.DREP_HASH,
                        drepDist.DREP_TYPE,
                        drepDist.AMOUNT.as("voting_power"),
                        latestStatus.field("deposit", BigInteger.class),
                        latestStatus.field("status", String.class),
                        latestStatus.field("registration_slot", Long.class),
                        drepExpiry.ACTIVE_UNTIL
                )
                .from(latestStatus)
                .leftJoin(drepDist).on(drepDist.DREP_HASH.eq(latestStatus.field("drep_hash", String.class))
                        .and(drepDist.DREP_ID.eq(latestStatus.field("drep_id", String.class)))
                        .and(drepDist.EPOCH.eq(maxEpochInGovDRepCalc)))
                .leftJoin(drepExpiry).on(drepExpiry.DREP_HASH.eq(drepDist.DREP_HASH)
                        .and(drepExpiry.DREP_ID.eq(drepDist.DREP_ID))
                        .and(drepExpiry.EPOCH.eq(maxEpochInGovDRepCalc)))
                .orderBy(order == Order.desc ?
                        DSL.field("registration_slot", Long.class).desc() :
                        DSL.field("registration_slot", Long.class).asc())
                .limit(count)
                .offset(page * count)
                .fetch();

        return result.stream()
                .map(r -> {
                    String drepId = r.get(drepDist.DREP_ID);
                    String drepHash = r.get(drepDist.DREP_HASH);
                    String drepTypeStr = r.get(drepDist.DREP_TYPE);
                    BigInteger votingPower = r.get("voting_power", BigInteger.class);
                    BigInteger deposit = r.get("deposit", BigInteger.class);
                    String drepStatusStr = r.get("status", String.class);
                    Integer activeUntil = r.get(drepExpiry.ACTIVE_UNTIL);
                    Long registrationSlot = r.get("registration_slot", Long.class);

                    DRepType dRepType = drepTypeStr != null ? DRepType.valueOf(drepTypeStr) : null;
                    DRepStatus status;

                    if (com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus.RETIRED.name().equalsIgnoreCase(drepStatusStr)) {
                        status = DRepStatus.INACTIVE;
                        votingPower = BigInteger.ZERO;
                    } else {
                        status = activeUntil == null || activeUntil >= epoch
                                ? DRepStatus.ACTIVE
                                : DRepStatus.INACTIVE;
                    }

                    return new DRepDetailsDto(
                            drepId,
                            drepHash,
                            dRepType,
                            deposit,
                            status,
                            votingPower,
                            registrationSlot
                    );
                })
                .toList();
    }

    @Override
    public Optional<DRepDetailsDto> getDRepDetailsByDRepId(String drepId, int epoch) {
        var d = DREP;
        var dist = DREP_DIST;
        var expiry = DREP_EXPIRY;

        Integer maxEpochInGovCalc = dsl
                .select(DSL.max(dist.EPOCH))
                .from(dist)
                .where(dist.EPOCH.le(epoch))
                .fetchOneInto(Integer.class);

        var latestStatus = dsl.select(
                        d.DREP_ID,
                        d.DREP_HASH,
                        d.DEPOSIT,
                        d.STATUS,
                        d.REGISTRATION_SLOT
                )
                .from(d)
                .where(d.DREP_ID.eq(drepId).and(d.EPOCH.le(epoch)))
                .orderBy(d.SLOT.desc(), d.TX_INDEX.desc(), d.CERT_INDEX.desc())
                .limit(1)
                .fetchOne();

        if (latestStatus == null) {
            return Optional.empty();
        }

        String drepHash = latestStatus.get(d.DREP_HASH);
        BigInteger deposit = BigInteger.valueOf(latestStatus.get(d.DEPOSIT));
        Long registrationSlot = latestStatus.get(d.REGISTRATION_SLOT);
        String drepStatusStr = latestStatus.get(d.STATUS);

        BigInteger votingPower = null;
        Integer activeUntil = null;
        DRepStatus status;
        DRepType dRepType = null;  // todo: add cred type to table 'drep'

        if (maxEpochInGovCalc != null) {
            var distrAndExpiryRow = dsl.select(
                            dist.DREP_ID,
                            dist.DREP_TYPE,
                            dist.AMOUNT.as("voting_power"),
                            expiry.ACTIVE_UNTIL
                    )
                    .from(dist)
                    .leftJoin(expiry).on(expiry.DREP_ID.eq(dist.DREP_ID)
                            .and(expiry.EPOCH.eq(maxEpochInGovCalc)))
                    .where(dist.DREP_ID.eq(drepId)
                            .and(dist.EPOCH.eq(maxEpochInGovCalc)))
                    .fetchOne();

            if (distrAndExpiryRow != null) {
                votingPower = distrAndExpiryRow.get("voting_power", BigInteger.class);
                activeUntil = distrAndExpiryRow.get(expiry.ACTIVE_UNTIL);
                drepId = distrAndExpiryRow.get(dist.DREP_ID);
                dRepType = DRepType.valueOf(distrAndExpiryRow.get(dist.DREP_TYPE));
            }
        }

        status = com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus.RETIRED.name().equalsIgnoreCase(drepStatusStr)
                ? DRepStatus.INACTIVE
                : (activeUntil == null || activeUntil >= epoch)
                ? DRepStatus.ACTIVE
                : DRepStatus.INACTIVE;

        if (status == DRepStatus.INACTIVE)
            votingPower = BigInteger.ZERO;

        return Optional.of(new DRepDetailsDto(
                drepId,
                drepHash,
                dRepType,
                deposit,
                status,
                votingPower,
                registrationSlot
        ));
    }
}
