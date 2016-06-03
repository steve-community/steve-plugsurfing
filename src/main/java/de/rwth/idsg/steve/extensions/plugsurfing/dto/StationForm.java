package de.rwth.idsg.steve.extensions.plugsurfing.dto;

import com.google.common.base.Strings;
import de.rwth.idsg.steve.web.dto.ChargePointForm;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;

/**
 * @author Vasil Borozanov <vasil.borozanov@rwth-aachen.de>
 * @since 17.12.2015
 */
@Setter
@Getter
public class StationForm extends ChargePointForm {

    @NotNull
    private Boolean plugSurfing;

    private Integer numberOfConnectors;

    @Range(min = 0, message = "Please enter valid value for the Floor")
    private Integer floorLevel;

    @Range(min = 0, message = "Please enter valid value for the Number of Parking Places")
    private Integer totalParking;

    @Valid
    private Contact contact;

    private Boolean open24;
    private Boolean reservable;
    private Boolean freeCharge;
    private Boolean greenPowerAvailable;
    private Boolean pluginCharge;
    private Boolean roofed;
    private Boolean privatelyOwned;

    @AssertTrue(message = "PlugSurfing requires the number of connectors")
    public boolean isValidNumberOfConnections() {
        if (plugSurfing) {
            return numberOfConnectors != null && numberOfConnectors > 0;
        } else {
            return true;
        }
    }

    @AssertTrue(message = "PlugSurfing requires latitude and longitude values")
    public boolean isLocationEmpty() {
        if (plugSurfing) {
            return getLocationLatitude() != null && getLocationLongitude() !=  null;
        } else {
            return true;
        }
    }

    @AssertTrue(message = "PlugSurfing requires a city")
    public boolean isValidCity() {
        if (plugSurfing) {
            return !Strings.isNullOrEmpty(getAddress().getCity());
        } else {
            return true;
        }
    }

    @AssertTrue(message = "PlugSurfing requires a zip code")
    public boolean isValidZipCode() {
        if (plugSurfing) {
            return !Strings.isNullOrEmpty(getAddress().getZipCode());
        } else {
            return true;
        }
    }

    @AssertTrue(message = "PlugSurfing requires a street name")
    public boolean isValidStreetName() {
        if (plugSurfing) {
            return !Strings.isNullOrEmpty(getAddress().getStreet());
        } else {
            return true;
        }
    }

    @AssertTrue(message = "PlugSurfing requires a street number")
    public boolean isValidStreetNumber() {
        if (plugSurfing) {
            return !Strings.isNullOrEmpty(getAddress().getHouseNumber());
        } else {
            return true;
        }
    }

    @AssertTrue(message = "PlugSurfing requires non-empty phone number")
    public boolean isValidPhone() {
        if (plugSurfing) {
            return !Strings.isNullOrEmpty(getContact().getPhone());
        } else {
            return true;
        }
    }

}
