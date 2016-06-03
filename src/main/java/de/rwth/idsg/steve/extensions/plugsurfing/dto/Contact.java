package de.rwth.idsg.steve.extensions.plugsurfing.dto;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Email;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 17.12.2015
 */
@Getter
@Setter
public class Contact {
    // we don't validate, as there are many possibilities (hyphen, plus, parenthesis etc.)
    private String phone;
    private String fax;
    private String website;

    @Email(message = "Please provide a valid email address")
    private String email;
}
