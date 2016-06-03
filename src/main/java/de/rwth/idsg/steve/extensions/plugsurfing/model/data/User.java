package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
@Setter
@ToString
public class User {

    @NotEmpty(message = "identifier is missing")
    private String identifier;

    @NotNull(message = "identifier-type is missing")
    private IdentifierType identifierType;

    private String token;

    @AssertTrue(message = "only the identifier-type 'rfid' is supported")
    public boolean isValidType() {
        return identifierType != null && identifierType == IdentifierType.RFID;
    }
}
