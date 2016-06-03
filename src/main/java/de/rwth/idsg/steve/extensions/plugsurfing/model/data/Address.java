package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

import com.neovisionaries.i18n.CountryCode;
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
public class Address {
    private String street;
    private String streetNumber;
    private String city;
    private String zip;
    private CountryCode country;
}
