package com.bloxbean.cardano.yaci.store.blockfrost.scripts.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.scripts.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFDatum;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScript;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptListItem;
import com.bloxbean.cardano.yaci.store.blockfrost.scripts.storage.impl.model.BFScriptRedeemer;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
@DecoratedWith(BFScriptsMapperDecorator.class)
public interface BFScriptsMapper {

    @Mapping(target = "scriptHash", source = "scriptHash")
    BFScriptListItemDto toListItemDto(BFScriptListItem model);

    /**
     * Base mapping for script detail.
     * {@code type} and {@code serialisedSize} are overridden in the decorator.
     */
    @Mapping(target = "scriptHash", source = "scriptHash")
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "serialisedSize", ignore = true)
    BFScriptDto toScriptDto(BFScript model);

    /**
     * Base mapping for script JSON view.
     * {@code json} is resolved in the decorator (requires CBOR/JSON parsing).
     */
    @Mapping(target = "json", ignore = true)
    BFScriptJsonDto toScriptJsonDto(BFScript model);

    /**
     * Base mapping for script CBOR view.
     * {@code cbor} is resolved in the decorator.
     */
    @Mapping(target = "cbor", ignore = true)
    BFScriptCborDto toScriptCborDto(BFScript model);

    /**
     * Base mapping for redeemer.
     * {@code purpose} and {@code fee} are overridden in the decorator.
     */
    @Mapping(target = "txHash", source = "txHash")
    @Mapping(target = "txIndex", source = "txIndex")
    @Mapping(target = "purpose", ignore = true)
    @Mapping(target = "unitMem", expression = "java(model.unitMem() != null ? String.valueOf(model.unitMem()) : \"0\")")
    @Mapping(target = "unitSteps", expression = "java(model.unitSteps() != null ? String.valueOf(model.unitSteps()) : \"0\")")
    @Mapping(target = "fee", constant = "0")
    @Mapping(target = "redeemerDataHash", source = "redeemerDataHash")
    @Mapping(target = "datumHash", source = "datumHash")
    BFScriptRedeemerDto toRedeemerDto(BFScriptRedeemer model);

    /**
     * Base mapping for datum JSON view.
     * {@code jsonValue} is resolved in the decorator (CBOR→JSON conversion).
     */
    @Mapping(target = "jsonValue", ignore = true)
    BFDatumDto toDatumDto(BFDatum model);

    /**
     * Base mapping for datum CBOR view.
     */
    @Mapping(target = "cbor", source = "cborHex")
    BFDatumCborDto toDatumCborDto(BFDatum model);
}
