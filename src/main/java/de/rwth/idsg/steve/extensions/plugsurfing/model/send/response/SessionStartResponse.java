package de.rwth.idsg.steve.extensions.plugsurfing.model.send.response;

import com.fasterxml.jackson.annotation.JsonRootName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.SuccessResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 04.12.2015
 */
@Getter
@Setter
@ToString(callSuper = true)
@JsonRootName("session-start")
public class SessionStartResponse extends SuccessResponse {
    private Boolean isStoppable;
    private String sessionId;
}
