package de.rwth.idsg.steve.extensions.plugsurfing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.RequiredArgsConstructor;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.03.2016
 */
@RequiredArgsConstructor
public class PsLocalTimeSerializer extends JsonSerializer<LocalTime> {

    private final DateTimeFormatter dateTimeFormatter;

    @Override
    public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException, JsonProcessingException {

        if (value == null) {
            serializers.defaultSerializeNull(gen);
        } else {
            gen.writeString(dateTimeFormatter.print(value));
        }
    }
}
