package com.bloxbean.cardano.yaci.store.blockfrost.account.serializer;

import com.bloxbean.cardano.yaci.store.utxo.domain.Amount;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class AmountAsStringSerializer extends JsonSerializer<Amount> {
    @Override
    public void serialize(Amount value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("unit", value.getUnit());
        if (value.getQuantity() == null) {
            gen.writeNullField("quantity");
        } else {
            gen.writeStringField("quantity", value.getQuantity().toString());
        }
        gen.writeEndObject();
    }
}
