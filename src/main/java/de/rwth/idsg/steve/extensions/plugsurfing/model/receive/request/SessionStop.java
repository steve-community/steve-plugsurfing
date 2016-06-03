package de.rwth.idsg.steve.extensions.plugsurfing.model.receive.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import de.rwth.idsg.steve.extensions.plugsurfing.model.data.User;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
@Setter
@ToString
@JsonRootName("session-stop")
public class SessionStop extends BaseRequest {

    @Valid
    @NotNull(message = "user is missing")
    private User user;

    @NotEmpty(message = "connector-id is missing")
    @JsonProperty(value = "connector-id")
    private String connectorPrimaryKey;

    @NotEmpty(message = "session-id is missing")
    private String sessionId;
}
