package com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BFProtocolParamMapperDecorator implements BFProtocolParamMapper {

    private final BFProtocolParamMapper delegate;

    public BFProtocolParamMapperDecorator(BFProtocolParamMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public BFProtocolParamsDto toBFProtocolParamsDto(ProtocolParamsDto protocolParamsDto) {

        BFProtocolParamsDto bfProtocolParamsDto = delegate.toBFProtocolParamsDto(protocolParamsDto);

        // Blockfrost uses coins_per_utxo_word for Babbage and later, which is equivalent to coins_per_utxo_size
        // Yaci store uses coins_per_utxo_size for Babbage and later.
        // So, if coinsPerUtxoSize is present, set it to coinsPerUtxoWord for blockfrost compatibility
        if (protocolParamsDto.getCoinsPerUtxoSize() != null) {

            bfProtocolParamsDto.setCoinsPerUtxoWord(protocolParamsDto.getCoinsPerUtxoSize());

            if (protocolParamsDto.getMinUtxo() == null){
                bfProtocolParamsDto.setMinUtxo(protocolParamsDto.getCoinsPerUtxoSize());
            }
        }

        if ( protocolParamsDto.getDecentralisationParam() != null ) {
            bfProtocolParamsDto.setDecentralisationParam(protocolParamsDto.getDecentralisationParam());
        } else {
            bfProtocolParamsDto.setDecentralisationParam(BigDecimal.ZERO);
        }

        if(protocolParamsDto.getCostModels() != null) {
            Map<String, List<Long>> costModelsRaw =  protocolParamsDto.getCostModels().entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().entrySet().stream()
                                    .map(Map.Entry::getValue)
                                    .collect(Collectors.toList())
                    ));
            bfProtocolParamsDto.setCostModelsRaw(costModelsRaw);
        }

        bfProtocolParamsDto.setEMax(protocolParamsDto.getEMax());
        bfProtocolParamsDto.setNOpt(protocolParamsDto.getNOpt());
        bfProtocolParamsDto.setPvtPPSecurityGroup(protocolParamsDto.getPvtppSecurityGroup());

        return bfProtocolParamsDto;
    }
}
