package com.bloxbean.cardano.yaci.store.script.dto;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;

import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.removeDoubleEncoding;

@Mapper(componentModel = "spring")
@Slf4j
public class ScriptDtoMapper {
    public ScriptDetailDto toScriptDetailDto(Script script) {
        ScriptDtoType type = getType(script);

        if (script.getScriptType() == ScriptType.NATIVE_SCRIPT) {
            ScriptJsonDto scriptJson = toScriptJson(script);
            return ScriptDetailDto.builder()
                    .scriptHash(script.getScriptHash())
                    .scriptType(type)
                    .content(scriptJson.getJson())
                    .build();
        } else {
            ScriptCborDto scriptCbor = toScriptCbor(script);
            return ScriptDetailDto.builder()
                    .scriptHash(script.getScriptHash())
                    .scriptType(type)
                    .content(new TextNode(scriptCbor.getCbor()))
                    .build();
        }

    }

    public ScriptDto toScriptDto(Script script) {
        ScriptDtoType type = getType(script); //These are Blockfrost compatible type
        Integer size = null;
        if (script.getContent() != null && script.getScriptType() != ScriptType.NATIVE_SCRIPT) {
            try {
                byte[] bytes = removeDoubleEncoding(script);
                size = bytes.length;
            } catch (Exception e) {
                log.error("Error while parsing script content", e);
            }
        }

        return ScriptDto.builder()
                .scriptHash(script.getScriptHash())
                .type(type)
                .serialisedSize(size)
                .build();
    }

    public ScriptCborDto toScriptCbor(Script script) {
        if (script.getScriptType() == ScriptType.NATIVE_SCRIPT)
            return new ScriptCborDto();

        try {
            byte[] bytes = removeDoubleEncoding(script);
            if (bytes == null)
                return new ScriptCborDto();

            return ScriptCborDto.builder()
                    .cbor(HexUtil.encodeHexString(bytes))
                    .build();

        } catch (Exception e) {
            log.error("Error while parsing script content", e);
        }

        return new ScriptCborDto();
    }

    public ScriptJsonDto toScriptJson(Script script) {
        if (script.getScriptType() != ScriptType.NATIVE_SCRIPT)
            return new ScriptJsonDto();

        try {
            JsonNode contentNode = JsonUtil.parseJson(script.getContent());
            String content = contentNode.get("content").asText();
            JsonNode contentJson = JsonUtil.parseJson(content);

            return ScriptJsonDto.builder()
                    .json(contentJson)
                    .build();

        } catch (Exception e) {
            log.error("Error while parsing script content", e);
        }

        return new ScriptJsonDto();
    }

    private static ScriptDtoType getType(Script script) {
        ScriptDtoType type = null;
        if (script.getScriptType() == ScriptType.NATIVE_SCRIPT)
            type = ScriptDtoType.timelock;
        else if (script.getScriptType() == ScriptType.PLUTUS_V1)
            type = ScriptDtoType.plutusV1;
        else if (script.getScriptType() == ScriptType.PLUTUS_V2)
            type = ScriptDtoType.plutusV2;

        return type;
    }
}
