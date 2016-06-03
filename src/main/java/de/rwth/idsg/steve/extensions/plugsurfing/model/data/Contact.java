package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

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
public class Contact {
    private String phone;
    private String fax;
    private String web;
    private String email;
}
