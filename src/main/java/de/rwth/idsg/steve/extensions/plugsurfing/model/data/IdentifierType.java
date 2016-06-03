package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
public enum IdentifierType {
    EVCO_ID("evco-id"),
    RFID("rfid"),
    USER_NAME("username");

    private final String value;

    IdentifierType(String value) {
        this.value = value;
    }

    @JsonValue // serialize
    public String value() {
        return value;
    }

    @JsonCreator // deserialize
    public static IdentifierType fromValue(String v) {
        for (IdentifierType c: IdentifierType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
