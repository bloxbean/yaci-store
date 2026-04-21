package com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.BFScriptsStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFDatum;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScript;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptListItem;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptRedeemer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.SortField;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.script.jooq.Tables.DATUM;
import static com.bloxbean.cardano.yaci.store.script.jooq.Tables.SCRIPT;
import static com.bloxbean.cardano.yaci.store.script.jooq.Tables.TRANSACTION_SCRIPTS;

@Component
@RequiredArgsConstructor
@Slf4j
public class BFScriptsStorageReaderImpl implements BFScriptsStorageReader {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    @Override
    public List<BFScriptListItem> getScripts(int page, int count, String order) {
        int offset = page * count;
        SortField<?> sortField = "asc".equalsIgnoreCase(order)
                ? SCRIPT.SLOT.asc().nullsFirst()
                : SCRIPT.SLOT.desc().nullsLast();

        return dsl.select(SCRIPT.SCRIPT_HASH)
                .from(SCRIPT)
                .orderBy(sortField)
                .limit(count)
                .offset(offset)
                .fetch(r -> new BFScriptListItem(r.get(SCRIPT.SCRIPT_HASH)));
    }

    @Override
    public Optional<BFScript> getScript(String scriptHash) {
        return dsl.select(SCRIPT.SCRIPT_HASH, SCRIPT.SCRIPT_TYPE, SCRIPT.CONTENT)
                .from(SCRIPT)
                .where(SCRIPT.SCRIPT_HASH.eq(scriptHash))
                .fetchOptional(r -> new BFScript(
                        r.get(SCRIPT.SCRIPT_HASH),
                        r.get(SCRIPT.SCRIPT_TYPE),
                        r.get(SCRIPT.CONTENT) != null ? r.get(SCRIPT.CONTENT).data() : null
                ));
    }

    @Override
    public List<BFScriptRedeemer> getScriptRedeemers(String scriptHash, int page, int count, String order) {
        int offset = page * count;
        SortField<?> sortField = "asc".equalsIgnoreCase(order)
                ? TRANSACTION_SCRIPTS.SLOT.asc().nullsLast()
                : TRANSACTION_SCRIPTS.SLOT.desc().nullsLast();

        // Fetch execution unit prices once for the entire page — more efficient than per-row
        ExecUnitPrices prices = fetchExecUnitPrices();

        return dsl.select(
                        TRANSACTION_SCRIPTS.TX_HASH,
                        TRANSACTION_SCRIPTS.REDEEMER_INDEX,
                        TRANSACTION_SCRIPTS.PURPOSE,
                        TRANSACTION_SCRIPTS.UNIT_MEM,
                        TRANSACTION_SCRIPTS.UNIT_STEPS,
                        TRANSACTION_SCRIPTS.REDEEMER_DATAHASH,
                        TRANSACTION_SCRIPTS.DATUM_HASH
                )
                .from(TRANSACTION_SCRIPTS)
                .where(TRANSACTION_SCRIPTS.SCRIPT_HASH.eq(scriptHash))
                .and(TRANSACTION_SCRIPTS.PURPOSE.isNotNull())
                .orderBy(sortField)
                .limit(count)
                .offset(offset)
                .fetch(r -> {
                    Long unitMem   = r.get(TRANSACTION_SCRIPTS.UNIT_MEM);
                    Long unitSteps = r.get(TRANSACTION_SCRIPTS.UNIT_STEPS);
                    String fee     = computeFee(unitMem, unitSteps, prices);
                    return new BFScriptRedeemer(
                            r.get(TRANSACTION_SCRIPTS.TX_HASH),
                            r.get(TRANSACTION_SCRIPTS.REDEEMER_INDEX) != null
                                    ? r.get(TRANSACTION_SCRIPTS.REDEEMER_INDEX).intValue() : 0,
                            r.get(TRANSACTION_SCRIPTS.PURPOSE),
                            unitMem,
                            unitSteps,
                            fee,
                            r.get(TRANSACTION_SCRIPTS.REDEEMER_DATAHASH),
                            r.get(TRANSACTION_SCRIPTS.DATUM_HASH)
                    );
                });
    }

    @Override
    public Optional<BFDatum> getDatum(String datumHash) {
        return dsl.select(DATUM.HASH, DATUM.DATUM_)
                .from(DATUM)
                .where(DATUM.HASH.eq(datumHash))
                .fetchOptional(r -> new BFDatum(
                        r.get(DATUM.HASH),
                        r.get(DATUM.DATUM_)
                ));
    }

    // ---- fee computation helpers ----

    /**
     * Fetches execution unit prices from the latest epoch_param row.
     * price_mem and price_step are stored as JSON objects:
     * {@code {"numerator": 577, "denominator": 10000}}.
     * Returns default preprod/mainnet prices if the query fails or returns null.
     */
    private ExecUnitPrices fetchExecUnitPrices() {
        try {
            // epoch_param.params is JSONB; use ->> to extract text value
            Field<String> paramsField = DSL.field("params", String.class);
            org.jooq.Table<?> epochParam = DSL.table(DSL.name("epoch_param"));

            String priceMemJson = dsl.select(
                            DSL.field("params->>'price_mem'", String.class))
                    .from(epochParam)
                    .orderBy(DSL.field("epoch", Integer.class).desc())
                    .limit(1)
                    .fetchOneInto(String.class);

            String priceStepJson = dsl.select(
                            DSL.field("params->>'price_step'", String.class))
                    .from(epochParam)
                    .orderBy(DSL.field("epoch", Integer.class).desc())
                    .limit(1)
                    .fetchOneInto(String.class);

            BigDecimal priceMem  = parseRationalJson(priceMemJson);
            BigDecimal priceStep = parseRationalJson(priceStepJson);
            return new ExecUnitPrices(priceMem, priceStep);
        } catch (Exception e) {
            log.warn("Could not fetch execution unit prices from epoch_param, using defaults: {}", e.getMessage());
            // Cardano standard prices for preprod/mainnet (0.0577 lovelace/mem, 0.0000721 lovelace/step)
            return new ExecUnitPrices(
                    new BigDecimal("577").divide(new BigDecimal("10000"), 20, RoundingMode.HALF_UP),
                    new BigDecimal("721").divide(new BigDecimal("10000000"), 20, RoundingMode.HALF_UP)
            );
        }
    }

    /**
     * Parses a rational number expressed as a JSON object:
     * {@code {"numerator": N, "denominator": D}} → BigDecimal N/D.
     */
    private BigDecimal parseRationalJson(String json) {
        if (json == null || json.isBlank()) return BigDecimal.ZERO;
        try {
            JsonNode node = objectMapper.readTree(json);
            long numerator   = node.get("numerator").asLong();
            long denominator = node.get("denominator").asLong();
            return new BigDecimal(numerator).divide(new BigDecimal(denominator), 20, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Failed to parse rational JSON '{}': {}", json, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Computes execution fee: {@code ceil(unitMem * priceMem + unitSteps * priceStep)}.
     * Returns {@code "0"} if execution units are null.
     */
    private String computeFee(Long unitMem, Long unitSteps, ExecUnitPrices prices) {
        if (unitMem == null || unitSteps == null) return "0";
        BigDecimal memCost  = prices.priceMem().multiply(new BigDecimal(unitMem));
        BigDecimal stepCost = prices.priceStep().multiply(new BigDecimal(unitSteps));
        BigDecimal total    = memCost.add(stepCost).setScale(0, RoundingMode.CEILING);
        return total.toPlainString();
    }

    private record ExecUnitPrices(BigDecimal priceMem, BigDecimal priceStep) {}
}
