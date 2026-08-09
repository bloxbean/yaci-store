package com.bloxbean.cardano.yaci.store.blockfrost.epoch.mapper;

import com.bloxbean.cardano.yaci.store.blockfrost.epoch.dto.BFProtocolParamsDto;
import com.bloxbean.cardano.yaci.store.epoch.dto.ProtocolParamsDto;

import java.math.BigDecimal;

public class BFProtocolParamMapperDecorator implements BFProtocolParamMapper {

    private final BFProtocolParamMapper delegate;

    public BFProtocolParamMapperDecorator(BFProtocolParamMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public BFProtocolParamsDto toBFProtocolParamsDto(ProtocolParamsDto protocolParamsDto) {

        BFProtocolParamsDto bfProtocolParamsDto = delegate.toBFProtocolParamsDto(protocolParamsDto);

        // Babbage+: coinsPerUtxoSize is authoritative for both min_utxo and coins_per_utxo_word
        if (protocolParamsDto.getCoinsPerUtxoSize() != null) {
            bfProtocolParamsDto.setCoinsPerUtxoWord(protocolParamsDto.getCoinsPerUtxoSize());
            bfProtocolParamsDto.setMinUtxo(protocolParamsDto.getCoinsPerUtxoSize());
        }

        if (protocolParamsDto.getDecentralisationParam() != null) {
            bfProtocolParamsDto.setDecentralisationParam(protocolParamsDto.getDecentralisationParam());
        } else {
            bfProtocolParamsDto.setDecentralisationParam(BigDecimal.ZERO);
        }

        if (protocolParamsDto.getCostModels() != null) {
            bfProtocolParamsDto.setCostModels(protocolParamsDto.getCostModels());
            bfProtocolParamsDto.setCostModelsRaw(protocolParamsDto.getCostModelsRaw());
        }

        if (protocolParamsDto.getGovActionDeposit() != null)
            bfProtocolParamsDto.setGovActionDeposit(protocolParamsDto.getGovActionDeposit().toString());
        if (protocolParamsDto.getDrepDeposit() != null)
            bfProtocolParamsDto.setDrepDeposit(protocolParamsDto.getDrepDeposit().toString());

        bfProtocolParamsDto.setEMax(protocolParamsDto.getEMax());
        bfProtocolParamsDto.setNOpt(protocolParamsDto.getNOpt());
        bfProtocolParamsDto.setPvtPPSecurityGroup(protocolParamsDto.getPvtppSecurityGroup());

        return bfProtocolParamsDto;
    }
}
