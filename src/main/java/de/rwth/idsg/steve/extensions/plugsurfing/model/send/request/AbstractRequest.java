package de.rwth.idsg.steve.extensions.plugsurfing.model.send.request;

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
public abstract class AbstractRequest {
    private String partnerIdentifier;
}
