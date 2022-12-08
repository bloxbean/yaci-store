package com.bloxbean.cardano.yaci.indexer.script.service;

import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.indexer.script.model.Script;
import com.bloxbean.cardano.yaci.indexer.script.dto.NativeScriptDto;
import com.bloxbean.cardano.yaci.indexer.script.dto.PlutusScriptDto;
import com.bloxbean.cardano.yaci.indexer.script.dto.ScriptDto;
import com.bloxbean.cardano.yaci.indexer.script.repository.ScriptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ScriptService {

    private ScriptRepository scriptRepository;

    public ScriptService(ScriptRepository scriptRepository) {
        this.scriptRepository = scriptRepository;
    }

    public Optional<ScriptDto> getScriptByHash(String scriptHash) {
        Optional<Script> scriptOptional = scriptRepository.findById(scriptHash);
        if (scriptOptional.isPresent()) {
            Script script = scriptOptional.get();
            if (script.getNativeScript() != null) {
                JsonNode jsonNode;
                try {
                    jsonNode = JsonUtil.parseJson(script.getNativeScript().getContent());
                } catch (Exception e) {
                    throw new IllegalStateException("NativeScript content cannot be parsed");
                }

                return Optional.of(NativeScriptDto.builder()
                        .scriptHash(script.getScriptHash())
                        .content(jsonNode)
                        .build());
            } else if (script.getPlutusScript() != null) {
                return Optional.of(PlutusScriptDto.builder()
                        .scriptHash(script.getScriptHash())
                        .content(script.getPlutusScript().getContent())
                        .type(script.getPlutusScript().getType())
                        .build()
                );
            } else
                throw new IllegalStateException("Invalid script type");
        } else {
            return Optional.empty();
        }
    }
}
