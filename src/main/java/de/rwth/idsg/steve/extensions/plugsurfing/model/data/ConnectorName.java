package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 17.12.2015
 */
public enum ConnectorName {
    Type1("Type1"),
    Type2("Type2"),
    Type3("Type3"),
    Combo("Combo"),
    Chademo("Chademo"),
    Schuko("Schuko"),
    ThreePinSquare("3PinSquare"),
    Cee2Poles("Cee2Poles"),
    CeeBlue("CeeBlue"),
    CeePlus("CeePlus"),
    CeeRed("CeeRed"),
    Marechal("Marechal"),
    Nema5("Nema5"),
    Scame("Scame"),
    T13("T13"),
    T15("T15"),
    T23("T23"),
    Tesla("Tesla"),
    Unknown("UNKNOWN")
    ;

    @Getter
    private String value;

    ConnectorName(String s) {
        this.value = s;
    }

    @JsonValue // serialize
    public String value() {
        return value;
    }

    @JsonCreator // deserialize
    public static ConnectorName fromValue(String v) {
        for (ConnectorName c: ConnectorName.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
