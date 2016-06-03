package de.rwth.idsg.steve.extensions.plugsurfing.model.send.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.ConnectorStatus;
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
@JsonRootName("connector-post-status")
public class ConnectorPostStatus extends AbstractRequest {

    @JsonProperty(value = "connector-id")
    private String connectorPrimaryKey;

    private ConnectorStatus status;
}
