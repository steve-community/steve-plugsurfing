package de.rwth.idsg.steve.extensions.plugsurfing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.03.2016
 */
@RequiredArgsConstructor
public class PsDateTimeDeserializer extends JsonDeserializer<DateTime> {

    private final DateTimeFormatter dateTimeFormatter;

    @Override
    public DateTime deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        if (jp.getCurrentToken() == JsonToken.VALUE_STRING) {
            String value = jp.getText();
            return dateTimeFormatter.parseDateTime(value);
        } else {
            return null;
        }
    }
}
