package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
@Setter
@ToString
public class Connector {

    @JsonProperty(value = "id")
    private String primaryKey;

    private ConnectorName name;

    private double speed;
}
