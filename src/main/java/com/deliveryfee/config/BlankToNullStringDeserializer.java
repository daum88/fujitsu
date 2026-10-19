package com.deliveryfee.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

/**
 * Maps blank or empty XML text nodes to {@code null} so that optional feed values such as wind
 * speed or phenomenon are stored as {@code NULL} rather than empty strings.
 */
public class BlankToNullStringDeserializer extends StdDeserializer<String> {

    /** Creates the deserializer for {@link String}. */
    public BlankToNullStringDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        var value = parser.getValueAsString();
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
