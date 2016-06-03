package de.rwth.idsg.steve.extensions.plugsurfing.model.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
@Getter
@Setter
@ToString
public class Station {
    private String id;
    private String name;
    private String description;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Address address;
    private Contact contact;
    private String cpoId;
    private Boolean isOpen24;
    private List<Connector> connectors;
    private String notes;
    private Boolean isReservable;
    private Integer floorLevel;
    private Boolean isFreeCharge;
    private Integer totalParking;
    private Boolean isGreenPowerAvailable;
    private Boolean isPluginCharge;
    private Boolean isRoofed;
    private Boolean isPrivate;
}
