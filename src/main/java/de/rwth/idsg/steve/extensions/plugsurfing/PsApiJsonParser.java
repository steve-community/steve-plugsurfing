package de.rwth.idsg.steve.extensions.plugsurfing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request.BaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 01.06.2016
 */
@Slf4j
public enum PsApiJsonParser {
    SINGLETON;

    private final ObjectMapper objectMapper;

    PsApiJsonParser() {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);
        objectMapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setPropertyNamingStrategy(new LowerCaseWithHyphenStrategy());
        objectMapper.registerModule(getCompatibleDateTimeModule());
    }

    public ObjectMapper getMapper() {
        return objectMapper;
    }

    private SimpleModule getCompatibleDateTimeModule() {

        // The date/time format is RFC3339 (Y-m-d\TH:i:sP).
        DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();

        // We want HH:mm
        DateTimeFormatter localTimeFormatter = ISODateTimeFormat.hourMinute();

        SimpleModule simpleModule = new SimpleModule();

        simpleModule.addSerializer(DateTime.class, new PsDateTimeSerializer(dateTimeFormatter));
        simpleModule.addDeserializer(DateTime.class, new PsDateTimeDeserializer(dateTimeFormatter));

        simpleModule.addSerializer(LocalTime.class, new PsLocalTimeSerializer(localTimeFormatter));
        simpleModule.addDeserializer(LocalTime.class, new PsLocalTimeDeserializer(localTimeFormatter));

        return simpleModule;
    }

    /**
     * @throws IOException, when it fails to deserialize the json
     */
    @SuppressWarnings("unchecked")
    public <T extends BaseRequest> T deserialize(String json, Class<? extends BaseRequest> clazz) throws IOException {
        return (T) objectMapper.readValue(json, clazz);
    }

    @Nullable
    public String serializeOrNull(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
