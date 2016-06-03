package de.rwth.idsg.steve.extensions.plugsurfing.model.send.response;

import com.fasterxml.jackson.annotation.JsonRootName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.SuccessResponse;
import lombok.ToString;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 16.12.2015
 */
@ToString(callSuper = true)
@JsonRootName("connector-post-status")
public class ConnectorPostStatusResponse extends SuccessResponse {
}
