package de.rwth.idsg.steve.extensions.plugsurfing.model.send.response;

import com.fasterxml.jackson.annotation.JsonRootName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.SuccessResponse;
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
@JsonRootName("session")
public class SessionPostResponse extends SuccessResponse {
    private String reason;
}
