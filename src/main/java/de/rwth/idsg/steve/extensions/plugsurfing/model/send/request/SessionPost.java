package de.rwth.idsg.steve.extensions.plugsurfing.model.send.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.TimePeriod;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.User;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
@Setter
@ToString(callSuper = true)
@JsonRootName("session-post")
public class SessionPost extends AbstractRequest {
    private User user;
    private String sessionId;

    @JsonProperty(value = "connector-id")
    private String connectorPrimaryKey;

    private TimePeriod sessionInterval;
    private TimePeriod chargingInterval;
    private Double energyConsumed;
}
